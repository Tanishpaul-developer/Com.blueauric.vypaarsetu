/**
 * VyaparSetu RESTful Backend API - Firebase Cloud Functions (v2)
 *
 * Exposes secure mercantile endpoints for authorized external integrations:
 * - /api/v1/ledger (Retrieve Udhar lists, customer balances, and recent ledger history)
 * - /api/v1/ledger/add (Atomic Firestore transaction for Udhar/Payment entries)
 * - /api/v1/store (Store profile, inventory, and dynamic UPI payment links)
 * - /api/v1/customers (Search & fetch merchant customer contact records)
 *
 * Region: asia-south1 (Mumbai)
 */

const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const app = express();

// Middleware: CORS & JSON Body Parser
app.use(cors({ origin: true }));
app.use(express.json());

// ============================================================================
// 1. AUTHENTICATION & SECURITY MIDDLEWARE
// ============================================================================

/**
 * Validates API keys against Cloud Firestore at `/api_keys/{apiKeyString}`.
 * Verifies that the key exists, is active (active === true), and not expired.
 * Attaches authenticated merchantId and key metadata to the request object.
 */
async function authenticateApiKey(req, res, next) {
  try {
    let apiKey = req.headers["x-api-key"];

    // Fallback: check Authorization: Bearer <key>
    if (!apiKey && req.headers.authorization) {
      const authHeader = req.headers.authorization.trim();
      if (authHeader.startsWith("Bearer ")) {
        apiKey = authHeader.substring(7).trim();
      }
    }

    if (!apiKey || typeof apiKey !== "string" || apiKey.trim().length === 0) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: Missing API key. Provide via 'x-api-key' header or 'Authorization: Bearer <key>'."
      });
    }

    const cleanKey = apiKey.trim();
    const keyDocRef = db.collection("api_keys").doc(cleanKey);
    const keySnap = await keyDocRef.get();

    if (!keySnap.exists) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: Invalid API key."
      });
    }

    const keyData = keySnap.data();

    // Check if key is active
    if (!keyData || keyData.active !== true) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: API key has been revoked or deactivated."
      });
    }

    // Check optional expiration timestamp
    if (keyData.expiresAt && keyData.expiresAt.toDate && keyData.expiresAt.toDate() < new Date()) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: API key has expired."
      });
    }

    // Ensure merchantId is associated
    if (!keyData.merchantId) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: API key is not linked to any valid merchant account."
      });
    }

    // Attach merchantId and key info to request
    req.merchantId = keyData.merchantId;
    req.apiKeyInfo = {
      keyId: cleanKey,
      name: keyData.name || "Default Integration Key",
      permissions: keyData.permissions || ["read", "write"]
    };

    next();
  } catch (err) {
    console.error("Authentication middleware error:", err);
    return res.status(500).json({
      success: false,
      error: "Internal server error during authentication verification."
    });
  }
}

// Apply authentication middleware to all /api/v1 routes
const v1Router = express.Router();
v1Router.use(authenticateApiKey);

// ============================================================================
// 2. RESTFUL API ENDPOINTS (/api/v1)
// ============================================================================

/**
 * GET /api/v1/ledger
 * Retrieve pending Udhar lists, customer balance summaries, and recent transaction history.
 */
v1Router.get("/ledger", async (req, res) => {
  try {
    const merchantId = req.merchantId;
    const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 50, 1), 100);

    // 1. Fetch Customers with positive Udhar balances
    const customersRef = db.collection("merchants").doc(merchantId).collection("customers");
    const customersSnap = await customersRef.get();

    let totalPendingUdhar = 0;
    let totalAdvancePayments = 0;
    const pendingUdharList = [];
    const customerSummary = [];

    customersSnap.forEach((doc) => {
      const c = doc.data();
      const balance = Number(c.currentBalance) || 0;

      const customerObj = {
        customerId: doc.id,
        name: c.name || "Unknown Customer",
        phone: c.phone || "",
        currentBalance: balance,
        address: c.address || "",
        lastTransactionAt: c.lastTransactionAt || null
      };

      customerSummary.push(customerObj);

      if (balance > 0) {
        totalPendingUdhar += balance;
        pendingUdharList.push(customerObj);
      } else if (balance < 0) {
        totalAdvancePayments += Math.abs(balance);
      }
    });

    // Sort pending Udhar descending by balance
    pendingUdharList.sort((a, b) => b.currentBalance - a.currentBalance);

    // 2. Fetch Recent Transactions
    const txRef = db
      .collection("merchants")
      .doc(merchantId)
      .collection("transactions")
      .orderBy("timestamp", "desc")
      .limit(limit);

    const txSnap = await txRef.get();
    const recentTransactions = [];

    txSnap.forEach((doc) => {
      const t = doc.data();
      recentTransactions.push({
        transactionId: doc.id,
        customerId: t.customerId || "",
        customerName: t.customerName || "",
        type: t.type, // "GIVE_UDHAR" | "RECEIVE_PAYMENT"
        amount: Number(t.amount) || 0,
        notes: t.notes || "",
        timestamp: t.timestamp ? (t.timestamp.toDate ? t.timestamp.toDate().toISOString() : t.timestamp) : null
      });
    });

    return res.status(200).json({
      success: true,
      data: {
        summary: {
          totalPendingUdhar: Math.round(totalPendingUdhar * 100) / 100,
          totalAdvancePayments: Math.round(totalAdvancePayments * 100) / 100,
          totalCustomers: customerSummary.length,
          udharCustomerCount: pendingUdharList.length
        },
        pendingUdharCustomers: pendingUdharList,
        recentTransactions: recentTransactions
      }
    });
  } catch (err) {
    console.error("GET /api/v1/ledger error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to retrieve merchant ledger records.",
      details: err.message
    });
  }
});

/**
 * POST /api/v1/ledger/add
 * Accept transaction payload, record transaction, and update customer balance atomically.
 * Body: { customerId: string, type: "GIVE_UDHAR" | "RECEIVE_PAYMENT", amount: number, notes?: string }
 */
v1Router.post("/ledger/add", async (req, res) => {
  try {
    const merchantId = req.merchantId;
    const { customerId, type, amount, notes } = req.body;

    // Validation
    if (!customerId || typeof customerId !== "string" || customerId.trim().length === 0) {
      return res.status(400).json({
        success: false,
        error: "Validation failed: 'customerId' is required."
      });
    }

    if (type !== "GIVE_UDHAR" && type !== "RECEIVE_PAYMENT") {
      return res.status(400).json({
        success: false,
        error: "Validation failed: 'type' must be either 'GIVE_UDHAR' or 'RECEIVE_PAYMENT'."
      });
    }

    const numAmount = Number(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      return res.status(400).json({
        success: false,
        error: "Validation failed: 'amount' must be a positive number greater than 0."
      });
    }

    const cleanNotes = typeof notes === "string" ? notes.trim().substring(0, 500) : "";
    const cleanCustomerId = customerId.trim();

    const customerDocRef = db
      .collection("merchants")
      .doc(merchantId)
      .collection("customers")
      .doc(cleanCustomerId);

    const newTxDocRef = db
      .collection("merchants")
      .doc(merchantId)
      .collection("transactions")
      .doc();

    // Execute Atomic Firestore Transaction
    const result = await db.runTransaction(async (transaction) => {
      const customerDoc = await transaction.get(customerDocRef);

      if (!customerDoc.exists) {
        throw new Error("CUSTOMER_NOT_FOUND");
      }

      const customerData = customerDoc.data();
      const previousBalance = Number(customerData.currentBalance) || 0;
      const customerName = customerData.name || "Customer";

      // Calculate updated balance:
      // GIVE_UDHAR increases the merchant's receivable balance (+ amount)
      // RECEIVE_PAYMENT decreases the receivable balance (- amount)
      const balanceDelta = type === "GIVE_UDHAR" ? numAmount : -numAmount;
      const updatedBalance = Math.round((previousBalance + balanceDelta) * 100) / 100;
      const now = admin.firestore.FieldValue.serverTimestamp();

      // 1. Record the transaction entry
      transaction.set(newTxDocRef, {
        transactionId: newTxDocRef.id,
        merchantId: merchantId,
        customerId: cleanCustomerId,
        customerName: customerName,
        type: type,
        amount: numAmount,
        previousBalance: previousBalance,
        newBalance: updatedBalance,
        notes: cleanNotes,
        timestamp: now,
        source: "REST_API"
      });

      // 2. Update customer balance and last active timestamp atomically
      transaction.update(customerDocRef, {
        currentBalance: updatedBalance,
        lastTransactionAt: now
      });

      return {
        transactionId: newTxDocRef.id,
        customerId: cleanCustomerId,
        customerName: customerName,
        type: type,
        amount: numAmount,
        previousBalance: previousBalance,
        newBalance: updatedBalance,
        notes: cleanNotes
      };
    });

    return res.status(201).json({
      success: true,
      message: "Ledger transaction recorded successfully.",
      data: result
    });
  } catch (err) {
    if (err.message === "CUSTOMER_NOT_FOUND") {
      return res.status(404).json({
        success: false,
        error: "Customer not found for the authenticated merchant."
      });
    }

    console.error("POST /api/v1/ledger/add error:", err);
    return res.status(500).json({
      success: false,
      error: "Atomic transaction failed while updating merchant ledger.",
      details: err.message
    });
  }
});

/**
 * GET /api/v1/store
 * Returns store profile details, inventory status, and dynamic UPI payload links for instant QR generation.
 * Optional query parameter: ?amount=250.00
 */
v1Router.get("/store", async (req, res) => {
  try {
    const merchantId = req.merchantId;
    const requestedAmount = req.query.amount ? parseFloat(req.query.amount) : null;

    // 1. Fetch Merchant Profile
    const merchantDoc = await db.collection("merchants").doc(merchantId).get();
    if (!merchantDoc.exists) {
      return res.status(404).json({
        success: false,
        error: "Merchant store profile not found."
      });
    }

    const merchantData = merchantDoc.data() || {};
    const shopName = merchantData.shop_name || merchantData.shopName || "VyaparSetu Merchant";
    const upiId = merchantData.upi_id || merchantData.upiId || "";

    // 2. Construct Dynamic UPI Link
    let dynamicUpiLink = "";
    if (upiId) {
      const encodedShop = encodeURIComponent(shopName);
      if (requestedAmount && requestedAmount > 0) {
        dynamicUpiLink = `upi://pay?pa=${upiId}&pn=${encodedShop}&am=${requestedAmount.toFixed(2)}&cu=INR`;
      } else {
        dynamicUpiLink = `upi://pay?pa=${upiId}&pn=${encodedShop}&cu=INR`;
      }
    }

    // 3. Fetch Inventory Summary
    const inventoryRef = db.collection("merchants").doc(merchantId).collection("inventory");
    const inventorySnap = await inventoryRef.get();

    const items = [];
    let totalInventoryValue = 0;
    let lowStockCount = 0;

    inventorySnap.forEach((doc) => {
      const item = doc.data();
      const stock = Number(item.stockQuantity ?? item.quantity) || 0;
      const price = Number(item.sellingPrice ?? item.price) || 0;
      const minStock = Number(item.minStockAlert) || 5;

      totalInventoryValue += stock * price;
      if (stock <= minStock) {
        lowStockCount++;
      }

      items.push({
        itemId: doc.id,
        name: item.name || "Item",
        category: item.category || "General",
        stockQuantity: stock,
        sellingPrice: price,
        isLowStock: stock <= minStock
      });
    });

    return res.status(200).json({
      success: true,
      data: {
        store: {
          merchantId: merchantId,
          shopName: shopName,
          ownerName: merchantData.owner_name || merchantData.ownerName || "",
          phone: merchantData.phone || "",
          upiId: upiId,
          address: merchantData.address || "",
          gstNumber: merchantData.gst_number || merchantData.gstNumber || "",
          category: merchantData.category || "Retail Store"
        },
        payment: {
          upiId: upiId,
          dynamicUpiLink: dynamicUpiLink,
          qrPayload: dynamicUpiLink
        },
        inventory: {
          totalItems: items.length,
          totalValuation: Math.round(totalInventoryValue * 100) / 100,
          lowStockCount: lowStockCount,
          items: items.slice(0, 100) // Return first 100 items
        }
      }
    });
  } catch (err) {
    console.error("GET /api/v1/store error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to retrieve store and inventory details.",
      details: err.message
    });
  }
});

/**
 * GET /api/v1/customers
 * Support searching and fetching customer contact records linked to the merchant.
 * Query parameter: ?q=name_or_phone&limit=50
 */
v1Router.get("/customers", async (req, res) => {
  try {
    const merchantId = req.merchantId;
    const queryTerm = (req.query.q || "").toString().trim().toLowerCase();
    const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 50, 1), 100);

    const customersRef = db.collection("merchants").doc(merchantId).collection("customers");
    const snapshot = await customersRef.get();

    const results = [];

    snapshot.forEach((doc) => {
      const data = doc.data();
      const name = (data.name || "").toString();
      const phone = (data.phone || "").toString();

      // Case-insensitive filtering on name or phone
      if (!queryTerm || name.toLowerCase().includes(queryTerm) || phone.includes(queryTerm)) {
        results.push({
          customerId: doc.id,
          name: name,
          phone: phone,
          currentBalance: Number(data.currentBalance) || 0,
          address: data.address || "",
          email: data.email || "",
          lastTransactionAt: data.lastTransactionAt || null
        });
      }
    });

    return res.status(200).json({
      success: true,
      count: results.length,
      data: results.slice(0, limit)
    });
  } catch (err) {
    console.error("GET /api/v1/customers error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to search customer records.",
      details: err.message
    });
  }
});

// Mount /api/v1
app.use("/api/v1", v1Router);

// 404 Catch-All Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: `Cannot ${req.method} ${req.originalUrl}. Route not found.`
  });
});

// Export Firebase Cloud Functions (v2) HTTPS entrypoint for region asia-south1
exports.api = onRequest(
  {
    region: "asia-south1",
    cors: true,
    maxInstances: 20,
    memory: "256MiB",
    timeoutSeconds: 30
  },
  app
);
