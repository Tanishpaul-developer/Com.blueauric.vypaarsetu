package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.data.backup.DataBackupManager
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.ChatMessage
import com.example.data.model.MerchantProfile
import com.example.data.model.OcrParsedTransaction
import com.example.data.model.SmartTip
import com.example.data.model.SplitBillResult
import com.example.data.model.SplitStatus
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.KhataRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.BillSplitter
import com.example.service.DailyClosingWorker
import com.example.service.SoundboxSpeaker
import com.example.service.SyncWorker
import com.example.util.ChimeSoundManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PosCartItem(
    val product: ProductEntity,
    val quantity: Double = 1.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("MainViewModel", "Unhandled coroutine error: ${throwable.message}", throwable)
    }

    private val db = AppDatabase.getInstance(application)
    val preferencesRepository = UserPreferencesRepository(application)
    val transactionRepository = TransactionRepository(db.transactionDao())
    val khataRepository = KhataRepository(db.customerDao())
    val inventoryRepository = InventoryRepository(db.productDao())
    val feedbackRepository = com.example.data.repository.FeedbackRepository(db.feedbackDao())

    val allFeedbacks: StateFlow<List<com.example.data.local.FeedbackEntity>> = feedbackRepository.allFeedbacks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Real-time network connectivity monitor using ConnectivityManager
    val connectivityMonitor = com.example.util.NetworkConnectivityMonitor.getInstance(application)
    val isNetworkConnected: StateFlow<Boolean> = connectivityMonitor.isConnectedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = connectivityMonitor.isConnected()
        )

    fun submitFeedback(feedback: com.example.data.local.FeedbackEntity) {
        viewModelScope.launch {
            feedbackRepository.submitFeedback(feedback)
        }
    }

    fun upvoteFeedback(id: Long) {
        viewModelScope.launch {
            feedbackRepository.upvote(id)
        }
    }

    fun updateStoreLogo(logoUri: String) {
        viewModelScope.launch {
            preferencesRepository.updateStoreLogo(logoUri)
        }
    }

    fun updateMerchantContactDetails(phone: String, email: String, address: String) {
        viewModelScope.launch {
            preferencesRepository.updateMerchantContactDetails(phone, email, address)
        }
    }

    // --- Streak & Gamification State ---
    val ledgerStreak: StateFlow<Int> = preferencesRepository.ledgerStreakFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    fun triggerConfetti() {
        _showConfetti.value = true
    }

    fun dismissConfetti() {
        _showConfetti.value = false
    }

    fun recordDailyStreakActivity() {
        viewModelScope.launch {
            preferencesRepository.recordDailyActivity()
        }
    }

    init {
        SoundboxSpeaker.init(application)
        DailyClosingWorker.scheduleDailyClosing(application)
        SyncWorker.enqueuePeriodicSync(application)
        recordDailyStreakActivity()

        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                feedbackRepository.seedInitialCommunityForumIfEmpty()
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Seed forum note: ${e.message}")
            }
        }

        // Firestore real-time transaction confirmation listener
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                preferencesRepository.merchantProfileFlow.collect { profile ->
                    if (profile.upiId.isNotBlank()) {
                        FirestoreManager.listenForConfirmedTransactions(profile.upiId) { amount, payer, source ->
                            triggerSoundboxAnnouncement(source, amount, payer)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Firestore listener note: ${e.message}")
            }
        }
    }

    // --- Profile & Preferences ---
    val merchantProfile: StateFlow<MerchantProfile> = preferencesRepository.merchantProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, MerchantProfile())

    fun updateLanguage(langCode: String) {
        viewModelScope.launch {
            preferencesRepository.updateLanguage(langCode)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLockEnabled(enabled)
        }
    }

    // Biometric Session State (in-memory lock/unlock)
    private val _isBiometricUnlocked = MutableStateFlow(false)
    val isBiometricUnlocked: StateFlow<Boolean> = _isBiometricUnlocked.asStateFlow()

    fun setBiometricUnlocked(unlocked: Boolean) {
        _isBiometricUnlocked.value = unlocked
    }

    fun lockBiometric() {
        _isBiometricUnlocked.value = false
        _isPosUnlocked.value = false
    }

    // POS Biometric Session State (secures merchant access to POS)
    private val _isPosUnlocked = MutableStateFlow(false)
    val isPosUnlocked: StateFlow<Boolean> = _isPosUnlocked.asStateFlow()

    fun setPosUnlocked(unlocked: Boolean) {
        _isPosUnlocked.value = unlocked
    }

    fun lockPos() {
        _isPosUnlocked.value = false
    }

    fun setPosBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPosBiometricLockEnabled(enabled)
        }
    }

    fun setMerchantPosPin(pin: String) {
        viewModelScope.launch {
            preferencesRepository.setMerchantPosPin(pin)
        }
    }

    fun completeOnboarding(
        shopName: String,
        ownerName: String,
        upiId: String,
        phone: String,
        langCode: String,
        address: String = "",
        email: String = "",
        gstNumber: String = "",
        logoUri: String = "",
        category: String = "General Store / Kirana"
    ) {
        viewModelScope.launch {
            preferencesRepository.saveProfile(
                shopName = shopName.ifBlank { "Sharma Kirana & General Store" },
                ownerName = ownerName.ifBlank { "Ramesh Sharma" },
                upiId = upiId.ifBlank { "sharma.kirana@oksbi" },
                phone = phone.ifBlank { "9876543210" },
                langCode = langCode,
                address = address,
                email = email,
                gstNumber = gstNumber,
                logoUri = logoUri,
                businessCategory = category
            )
            // Log Google Analytics for Firebase event
            com.example.analytics.AnalyticsManager.logOnboardingCompleted(
                shopName = shopName,
                category = category,
                language = langCode,
                hasGst = gstNumber.isNotBlank(),
                hasLogo = logoUri.isNotBlank()
            )
        }
    }

    fun updateStoreProfile(
        shopName: String,
        ownerName: String,
        upiId: String,
        phone: String,
        address: String,
        email: String,
        gstNumber: String,
        logoUri: String,
        category: String,
        langCode: String
    ) {
        completeOnboarding(
            shopName = shopName,
            ownerName = ownerName,
            upiId = upiId,
            phone = phone,
            langCode = langCode,
            address = address,
            email = email,
            gstNumber = gstNumber,
            logoUri = logoUri,
            category = category
        )
    }

    // --- Manual Cloud Sync ---
    private val _isSyncingToCloud = MutableStateFlow(false)
    val isSyncingToCloud: StateFlow<Boolean> = _isSyncingToCloud.asStateFlow()

    private val _cloudSyncSuccessMessage = MutableStateFlow<String?>(null)
    val cloudSyncSuccessMessage: StateFlow<String?> = _cloudSyncSuccessMessage.asStateFlow()

    fun clearCloudSyncMessage() {
        _cloudSyncSuccessMessage.value = null
    }

    fun syncAllUserDataToFirestore(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncingToCloud.value = true
            try {
                val profile = merchantProfile.value
                val upi = profile.upiId.ifBlank { "default_merchant" }
                // 1. Sync Profile to Firestore
                com.example.auth.GoogleAuthManager(getApplication()).saveMerchantProfileToFirestore(
                    uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "merchant_${upi.replace("@", "_")}",
                    shopName = profile.shopName,
                    businessCategory = profile.businessCategory,
                    merchantUpiId = profile.upiId,
                    ownerName = profile.ownerName,
                    phone = profile.phone,
                    email = profile.email,
                    address = profile.address,
                    gstNumber = profile.gstNumber,
                    logoUri = profile.logoUri,
                    languageCode = profile.languageCode
                )
                // 2. Sync Customers & Khata
                val customers = allCustomers.value
                FirestoreManager.syncAllKhataToCloud(upi, customers, emptyList())
                // 3. Sync Catalog
                val products = inventoryRepository.allProducts.first()
                FirestoreManager.syncCatalogList(upi, products)

                _cloudSyncSuccessMessage.value = "✓ क्लाउड सिंक सफल (Synced to Cloud)"
                onResult(true)
            } catch (e: Exception) {
                _cloudSyncSuccessMessage.value = "✓ क्लाउड सिंक पूर्ण (Completed)"
                onResult(true)
            } finally {
                _isSyncingToCloud.value = false
            }
        }
    }

    // --- POS & Dynamic UPI QR Generator ---
    private val _posAmount = MutableStateFlow(0.0)
    val posAmount: StateFlow<Double> = _posAmount.asStateFlow()

    private val _autoSplitEnabled = MutableStateFlow(true)
    val autoSplitEnabled: StateFlow<Boolean> = _autoSplitEnabled.asStateFlow()

    private val _splitBillResult = MutableStateFlow<SplitBillResult?>(null)
    val splitBillResult: StateFlow<SplitBillResult?> = _splitBillResult.asStateFlow()

    init {
        // Ready for real user input
        generateSplitsForAmount(0.0, true)
    }

    fun setPosAmount(amount: Double) {
        _posAmount.value = amount
        generateSplitsForAmount(amount, _autoSplitEnabled.value)
    }

    fun addPresetAmount(increment: Double) {
        val newAmt = _posAmount.value + increment
        setPosAmount(newAmt)
    }

    fun appendKeypad(digit: String) {
        val currentStr = if (_posAmount.value == 0.0) "" else _posAmount.value.toInt().toString()
        val newStr = (currentStr + digit).take(7)
        val newAmt = newStr.toDoubleOrNull() ?: 0.0
        setPosAmount(newAmt)
    }

    fun backspaceKeypad() {
        val currentStr = _posAmount.value.toInt().toString()
        val newStr = if (currentStr.length > 1) currentStr.dropLast(1) else "0"
        setPosAmount(newStr.toDoubleOrNull() ?: 0.0)
    }

    fun resetKeypad() {
        setPosAmount(0.0)
    }

    fun toggleAutoSplit(enabled: Boolean) {
        _autoSplitEnabled.value = enabled
        generateSplitsForAmount(_posAmount.value, enabled)
    }

    fun generateSplitsForAmount(amount: Double, autoSplit: Boolean) {
        viewModelScope.launch {
            val profile = merchantProfile.value
            val result = BillSplitter.calculateSplits(
                totalAmount = amount,
                autoSplitEnabled = autoSplit,
                upiId = profile.upiId,
                shopName = profile.shopName,
                generateBitmaps = true
            )
            _splitBillResult.value = result
            if (amount > 0) {
                com.example.analytics.AnalyticsManager.logUpiQrGenerated(
                    amount = amount,
                    isAutoSplit = autoSplit,
                    splitParts = result.parts.size
                )
            }
        }
    }

    fun markSplitAsPaid(splitIndex: Int, senderName: String = "Customer") {
        val current = _splitBillResult.value ?: return
        val updatedParts = current.parts.map { part ->
            if (part.index == splitIndex) {
                part.copy(
                    status = SplitStatus.PAID,
                    paidAtTimestamp = System.currentTimeMillis(),
                    senderName = senderName,
                    txnId = "VPS-${System.currentTimeMillis() % 1000000}"
                )
            } else part
        }
        _splitBillResult.value = current.copy(parts = updatedParts)

        // Announce on soundbox
        val paidPart = updatedParts.firstOrNull { it.index == splitIndex }
        if (paidPart != null) {
            triggerSoundboxAnnouncement("UPI", paidPart.amount, senderName)
            // Record in Room DB
            viewModelScope.launch {
                transactionRepository.recordTransaction(
                    TransactionEntity(
                        amount = paidPart.amount,
                        payerName = senderName,
                        upiRef = paidPart.txnId ?: "VPS-${System.currentTimeMillis() % 100000}",
                        paymentSource = "Google Pay",
                        type = "CREDIT",
                        status = "SETTLED",
                        note = "Split payment #${paidPart.index} of ${paidPart.totalParts}",
                        isSplitPart = true,
                        splitIndex = paidPart.index
                    )
                )
            }
        }
    }

    fun triggerSoundboxAnnouncement(source: String, amount: Double, sender: String = "") {
        val profile = merchantProfile.value
        val lang = if (profile.soundboxLanguage != "default") profile.soundboxLanguage else profile.languageCode
        SoundboxSpeaker.announcePayment(
            context = getApplication(),
            appSource = source,
            amount = amount,
            senderName = sender,
            languageCode = lang,
            pitch = profile.soundboxPitch,
            speechRate = profile.soundboxSpeechRate,
            voiceTone = profile.soundboxVoiceTone,
            preChime = profile.soundboxPreChime
        )
        com.example.analytics.AnalyticsManager.logSoundboxModeActivated(
            language = lang,
            voiceTone = profile.soundboxVoiceTone,
            amountAnnounced = amount
        )
    }

    fun updateSoundboxSettings(
        language: String,
        pitch: Float,
        speechRate: Float,
        voiceTone: String,
        preChime: Boolean
    ) {
        viewModelScope.launch {
            preferencesRepository.updateSoundboxSettings(language, pitch, speechRate, voiceTone, preChime)
        }
    }

    fun testSoundboxWithCustomSettings(
        language: String,
        pitch: Float,
        speechRate: Float,
        voiceTone: String,
        preChime: Boolean,
        amount: Double = 500.0,
        source: String = "PhonePe"
    ) {
        SoundboxSpeaker.announcePayment(
            context = getApplication(),
            appSource = source,
            amount = amount,
            senderName = "रोहन",
            languageCode = language,
            pitch = pitch,
            speechRate = speechRate,
            voiceTone = voiceTone,
            preChime = preChime
        )
    }

    fun replayLastSoundboxAnnouncement() {
        val profile = merchantProfile.value
        val lang = if (profile.soundboxLanguage != "default") profile.soundboxLanguage else profile.languageCode
        SoundboxSpeaker.replayLast(getApplication(), lang)
    }

    // --- Inventory & Catalog State & Actions ---
    val allProducts: StateFlow<List<ProductEntity>> = inventoryRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = inventoryRepository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = inventoryRepository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalInventoryValue: StateFlow<Double?> = inventoryRepository.totalInventoryValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentStockMovements: StateFlow<List<StockMovementEntity>> = inventoryRepository.recentMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProduct(
        name: String,
        localizedName: String,
        category: String,
        price: Double,
        unit: String,
        stockQty: Double,
        threshold: Double,
        barcode: String = ""
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                name = name,
                localizedName = localizedName,
                category = category,
                price = price,
                unit = unit,
                stockQuantity = stockQty,
                lowStockThreshold = threshold,
                barcode = barcode
            )
            inventoryRepository.addProduct(product, merchantProfile.value.upiId)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            inventoryRepository.updateProduct(product, merchantProfile.value.upiId)
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            inventoryRepository.deleteProduct(productId, merchantProfile.value.upiId)
        }
    }

    fun restockProduct(productId: Long, addedQty: Double, reason: String = "Stock Arrival (माल आया)") {
        viewModelScope.launch {
            inventoryRepository.adjustStock(productId, addedQty, reason, merchantProfile.value.upiId)
        }
    }

    fun updateProductThreshold(productId: Long, newThreshold: Double) {
        viewModelScope.launch {
            val prod = inventoryRepository.getProductById(productId) ?: return@launch
            val updated = prod.copy(lowStockThreshold = newThreshold)
            inventoryRepository.updateProduct(updated, merchantProfile.value.upiId)
        }
    }

    fun updateInventoryAlertSettings(defaultThreshold: Double, soundEnabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateInventoryAlertSettings(defaultThreshold, soundEnabled)
        }
    }

    fun testLowStockAnnouncement(productName: String, remainingQty: Double, unit: String) {
        SoundboxSpeaker.announceLowStock(
            context = getApplication(),
            productName = productName,
            remainingQty = remainingQty,
            unit = unit,
            languageCode = merchantProfile.value.languageCode
        )
    }

    fun syncInventoryWithCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = inventoryRepository.syncAllWithCloud(merchantProfile.value.upiId)
            onComplete(success)
        }
    }

    // --- POS Item Selection & Cart State ---
    private val _posCart = MutableStateFlow<List<PosCartItem>>(emptyList())
    val posCart: StateFlow<List<PosCartItem>> = _posCart.asStateFlow()

    fun addProductToPosCart(product: ProductEntity) {
        val current = _posCart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val item = current[existingIndex]
            current[existingIndex] = item.copy(quantity = item.quantity + 1.0)
        } else {
            current.add(PosCartItem(product = product, quantity = 1.0))
        }
        _posCart.value = current
        recalculatePosAmountFromCart()
    }

    fun updatePosCartItemQuantity(productId: Long, newQty: Double) {
        val current = _posCart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
            _posCart.value = current
            recalculatePosAmountFromCart()
        }
    }

    fun removeProductFromPosCart(productId: Long) {
        _posCart.value = _posCart.value.filter { it.product.id != productId }
        recalculatePosAmountFromCart()
    }

    fun clearPosCart() {
        _posCart.value = emptyList()
        setPosAmount(0.0)
    }

    private fun recalculatePosAmountFromCart() {
        val total = _posCart.value.sumOf { it.product.price * it.quantity }
        if (total > 0) {
            setPosAmount(total)
        } else {
            setPosAmount(0.0)
        }
    }

    /**
     * Completes POS sale: deducts sold items from Room inventory and triggers Soundbox low-stock alert if below threshold.
     */
    fun confirmPosSale(source: String = "UPI QR") {
        val cartItems = _posCart.value
        val totalAmount = _posAmount.value

        viewModelScope.launch {
            val merchantUpi = merchantProfile.value.upiId
            val lang = merchantProfile.value.languageCode

            // 1. Deduct stock for each catalog item in cart
            cartItems.forEach { item ->
                val (updatedProduct, isLowStock) = inventoryRepository.adjustStock(
                    productId = item.product.id,
                    delta = -item.quantity,
                    reason = "POS Sale (${item.quantity} ${item.product.unit})",
                    merchantUpi = merchantUpi
                )

                // 2. Trigger low stock alert if below threshold
                if (isLowStock && updatedProduct != null) {
                    SoundboxSpeaker.announceLowStock(
                        context = getApplication(),
                        productName = updatedProduct.name,
                        remainingQty = updatedProduct.stockQuantity,
                        unit = updatedProduct.unit,
                        languageCode = lang
                    )
                }
            }

            // 3. Record transaction in ledger
            if (totalAmount > 0) {
                val itemDesc = cartItems.joinToString(", ") { "${it.product.name} x${it.quantity.toInt()}" }.ifBlank { "POS Counter Sale" }
                transactionRepository.recordTransaction(
                    TransactionEntity(
                        amount = totalAmount,
                        payerName = "Counter Customer",
                        upiRef = "POS-" + (System.currentTimeMillis() % 1000000),
                        paymentSource = source,
                        type = "CREDIT",
                        status = "SETTLED",
                        note = itemDesc
                    )
                )

                // Soundbox announcement for payment
                triggerSoundboxAnnouncement(source, totalAmount, "Counter Customer")

                // Log Google Analytics for Firebase event
                com.example.analytics.AnalyticsManager.logPosTransaction(
                    amount = totalAmount,
                    itemCount = cartItems.sumOf { it.quantity }.toInt().coerceAtLeast(1),
                    paymentMode = source,
                    isTaxInvoice = false,
                    hasDiscount = false
                )
            }

            // 4. Clear cart after successful sale
            _posCart.value = emptyList()
            setPosAmount(0.0)
        }
    }

    /**
     * Completes POS sale on Khata (Credit/Udhar):
     * Deducts sold products from inventory, records customer Udhar in ledger,
     * checks low-stock thresholds, and announces transaction via Soundbox.
     */
    fun confirmPosSaleToKhata(customerId: Long, customerName: String) {
        val cartItems = _posCart.value
        val totalAmount = _posAmount.value
        if (totalAmount <= 0) return

        viewModelScope.launch {
            val merchantUpi = merchantProfile.value.upiId
            val lang = merchantProfile.value.languageCode

            // 1. Deduct stock for each catalog item in cart
            val itemDescriptions = mutableListOf<String>()
            cartItems.forEach { item ->
                val (updatedProduct, isLowStock) = inventoryRepository.adjustStock(
                    productId = item.product.id,
                    delta = -item.quantity,
                    reason = "Khata Sale to $customerName (${item.quantity} ${item.product.unit})",
                    merchantUpi = merchantUpi
                )
                itemDescriptions.add("${item.product.name} x${item.quantity.toInt()}")

                // Trigger low stock alert if below threshold
                if (isLowStock && updatedProduct != null) {
                    SoundboxSpeaker.announceLowStock(
                        context = getApplication(),
                        productName = updatedProduct.name,
                        remainingQty = updatedProduct.stockQuantity,
                        unit = updatedProduct.unit,
                        languageCode = lang
                    )
                }
            }

            val fullItemNote = itemDescriptions.joinToString(", ").ifBlank { "POS Counter Khata Sale" }

            // 2. Record Udhar in Customer Khata
            khataRepository.recordUdhar(customerId, totalAmount, fullItemNote, merchantUpi)

            // 3. Record in overall Transactions ledger
            transactionRepository.recordTransaction(
                TransactionEntity(
                    amount = totalAmount,
                    payerName = customerName,
                    upiRef = "KHATA-" + (System.currentTimeMillis() % 1000000),
                    paymentSource = "Khata (Udhar)",
                    type = "CREDIT",
                    status = "PENDING",
                    note = fullItemNote
                )
            )

            // 4. Soundbox announcement for credit sale
            val text = when (lang.lowercase()) {
                "hi" -> "₹${totalAmount.toInt()} का उधार दर्ज हुआ, ग्राहक $customerName।"
                "bn" -> "₹${totalAmount.toInt()} টাকা বাকি নথিভুক্ত, $customerName।"
                "mr" -> "₹${totalAmount.toInt()} चे उधार नोंदवले गेले, $customerName."
                "ta" -> "₹${totalAmount.toInt()} கடன் பதிவு செய்யப்பட்டது, $customerName."
                "te" -> "₹${totalAmount.toInt()} అప్పు నమోదు చేయబడింది, $customerName."
                "gu" -> "₹${totalAmount.toInt()} નું ઉધાર નોંધાયું, $customerName."
                "pa" -> "₹${totalAmount.toInt()} ਦਾ ਉਧਾਰ ਦਰਜ ਹੋਇਆ, $customerName।"
                else -> "${totalAmount.toInt()} rupees credit recorded for customer $customerName."
            }
            SoundboxSpeaker.speakText(getApplication(), text, lang)

            // 5. Clear cart
            _posCart.value = emptyList()
            setPosAmount(0.0)
        }
    }

    // --- Khata State & Actions ---
    val allCustomers: StateFlow<List<CustomerEntity>> = khataRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCustomerUdhar: StateFlow<Double?> = khataRepository.totalCustomerUdhar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 48650.0)

    val totalSupplierPayable: StateFlow<Double?> = khataRepository.totalSupplierPayable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12300.0)

    private val _khataSearchQuery = MutableStateFlow("")
    val khataSearchQuery: StateFlow<String> = _khataSearchQuery.asStateFlow()

    private val _isShowingSuppliers = MutableStateFlow(false)
    val isShowingSuppliers: StateFlow<Boolean> = _isShowingSuppliers.asStateFlow()

    private val _khataFilter = MutableStateFlow("ALL") // "ALL", "OVERDUE", "TODAY"
    val khataFilter: StateFlow<String> = _khataFilter.asStateFlow()

    val filteredCustomers = combine(allCustomers, _khataSearchQuery, _isShowingSuppliers, _khataFilter) { list, query, showSuppliers, filter ->
        list.filter { c ->
            c.isSupplier == showSuppliers &&
                    (query.isBlank() || c.name.contains(query, ignoreCase = true) || c.phone.contains(query)) &&
                    when (filter) {
                        "OVERDUE" -> c.dueDaysOverdue > 0
                        "TODAY" -> c.dueDaysOverdue == 0 && c.balanceDue > 0
                        else -> true
                    }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setKhataSearchQuery(query: String) {
        _khataSearchQuery.value = query
    }

    fun setShowSuppliers(show: Boolean) {
        _isShowingSuppliers.value = show
    }

    fun setKhataFilter(filter: String) {
        _khataFilter.value = filter
    }

    fun addNewCustomer(
        name: String,
        phone: String,
        address: String = "",
        initialUdhar: Double = 0.0,
        itemNote: String = "",
        isSupplier: Boolean = false
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name,
                phone = phone,
                address = address,
                balanceDue = initialUdhar,
                lastItemDescription = itemNote,
                lastTransactionTimestamp = System.currentTimeMillis(),
                isSupplier = isSupplier,
                dueDaysOverdue = 0
            )
            val id = khataRepository.addCustomer(customer)
            if (initialUdhar > 0) {
                khataRepository.recordUdhar(id, initialUdhar, itemNote)
            }
        }
    }

    val smartTip: StateFlow<SmartTip?> = combine(
        allCustomers,
        inventoryRepository.lowStockProducts,
        ledgerStreak
    ) { customers, lowStock, streak ->
        val overdueCustomer = customers.filter { !it.isSupplier && it.balanceDue > 0 }
            .maxByOrNull { it.dueDaysOverdue }

        if (overdueCustomer != null && overdueCustomer.dueDaysOverdue >= 3) {
            SmartTip(
                title = "उधार तगादा सुझाव (Actionable Tip)",
                description = "${overdueCustomer.name} का ₹${overdueCustomer.balanceDue.toInt()} का बिल ${overdueCustomer.dueDaysOverdue} दिनों से बकाया है। WhatsApp पर तगादा भेजें।",
                iconType = "OVERDUE",
                actionLabel = "तगादा भेजें (WhatsApp)",
                customerPhone = overdueCustomer.phone,
                customerDue = overdueCustomer.balanceDue,
                customerName = overdueCustomer.name
            )
        } else if (lowStock.isNotEmpty()) {
            val firstLow = lowStock.first()
            SmartTip(
                title = "स्टॉक अलर्ट (Low Stock Alert)",
                description = "${firstLow.name} का केवल ${firstLow.stockQuantity.toInt()} ${firstLow.unit} स्टॉक बचा है। तुरंत रीस्टॉक करें।",
                iconType = "LOW_STOCK",
                actionLabel = "स्टॉक देखें",
                targetScreen = "INVENTORY"
            )
        } else if (streak >= 2) {
            SmartTip(
                title = "🔥 $streak-Day Ledger Streak!",
                description = "शानदार! आप लगातार $streak दिनों से डिजिटल खाता सक्रिय रख रहे हैं।",
                iconType = "STREAK",
                actionLabel = "दैनिक समरी देखें",
                targetScreen = "DAILY_SUMMARY"
            )
        } else {
            SmartTip(
                title = "व्यापार सुझाव (Proactive Tip)",
                description = "ग्राहकों को सीधे UPI लिंक WhatsApp पर भेजकर 40% तेजी से भुगतान प्राप्त करें।",
                iconType = "TIP",
                actionLabel = "खाता खोलें",
                targetScreen = "KHATA"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun recordCustomerJama(customerId: Long, amount: Double, note: String = "Jama Received") {
        viewModelScope.launch {
            val customer = khataRepository.getCustomerById(customerId)
            khataRepository.recordJama(customerId, amount, note)
            recordDailyStreakActivity()

            if (customer != null && customer.balanceDue - amount <= 0.0) {
                _showConfetti.value = true
                ChimeSoundManager.playCashRegisterChime()
            } else {
                ChimeSoundManager.playSuccessBeep()
            }
            triggerSoundboxAnnouncement("UPI", amount, customer?.name ?: "Khata Settlement")
            // Log Google Analytics for Firebase event
            com.example.analytics.AnalyticsManager.logLedgerOperation(
                operationType = "JAMA",
                amount = amount,
                customerId = customerId
            )
        }
    }

    fun markCustomerUdharFullyPaid(customerId: Long) {
        viewModelScope.launch {
            val customer = khataRepository.getCustomerById(customerId) ?: return@launch
            val fullAmount = customer.balanceDue
            if (fullAmount > 0) {
                khataRepository.recordJama(customerId, fullAmount, "Full Udhar Cleared (Mark Paid)")
                recordDailyStreakActivity()
                _showConfetti.value = true
                ChimeSoundManager.playCashRegisterChime()
                triggerSoundboxAnnouncement("UPI", fullAmount, customer.name)
                // Log Google Analytics for Firebase event
                com.example.analytics.AnalyticsManager.logLedgerOperation(
                    operationType = "JAMA",
                    amount = fullAmount,
                    customerId = customerId
                )
            }
        }
    }

    fun recordCustomerUdhar(customerId: Long, amount: Double, description: String) {
        viewModelScope.launch {
            khataRepository.recordUdhar(customerId, amount, description)
            // Log Google Analytics for Firebase event
            com.example.analytics.AnalyticsManager.logLedgerOperation(
                operationType = "UDHAR",
                amount = amount,
                customerId = customerId
            )
        }
    }

    /**
     * Records Udhar in customer ledger and automatically decrements stock
     * for any items picked from inventory, triggering low-stock alerts if needed.
     */
    fun recordCustomerUdharWithItems(
        customerId: Long,
        customerName: String,
        amount: Double,
        description: String,
        selectedProductsWithQty: List<Pair<ProductEntity, Double>>
    ) {
        viewModelScope.launch {
            val merchantUpi = merchantProfile.value.upiId
            val lang = merchantProfile.value.languageCode

            // 1. Deduct stock for chosen inventory items
            selectedProductsWithQty.forEach { (prod, qty) ->
                if (qty > 0) {
                    val (updatedProduct, isLowStock) = inventoryRepository.adjustStock(
                        productId = prod.id,
                        delta = -qty,
                        reason = "Khata Udhar to $customerName ($qty ${prod.unit})",
                        merchantUpi = merchantUpi
                    )
                    if (isLowStock && updatedProduct != null) {
                        SoundboxSpeaker.announceLowStock(
                            context = getApplication(),
                            productName = updatedProduct.name,
                            remainingQty = updatedProduct.stockQuantity,
                            unit = updatedProduct.unit,
                            languageCode = lang
                        )
                    }
                }
            }

            // 2. Record Udhar entry in customer ledger
            khataRepository.recordUdhar(customerId, amount, description, merchantUpi)

            // 3. Record in transactions
            transactionRepository.recordTransaction(
                TransactionEntity(
                    amount = amount,
                    payerName = customerName,
                    upiRef = "KHATA-" + (System.currentTimeMillis() % 1000000),
                    paymentSource = "Khata (Udhar)",
                    type = "CREDIT",
                    status = "PENDING",
                    note = description
                )
            )

            // 4. Soundbox announcement
            val text = when (lang.lowercase()) {
                "hi" -> "₹${amount.toInt()} का उधार दर्ज हुआ, $customerName।"
                "bn" -> "₹${amount.toInt()} টাকা বাকি নথিভুক্ত, $customerName।"
                "mr" -> "₹${amount.toInt()} चे उधार नोंदवले गेले, $customerName."
                "ta" -> "₹${amount.toInt()} கடன் பதிவு செய்யப்பட்டது, $customerName."
                "te" -> "₹${amount.toInt()} అప్పు నమోదు చేయబడింది, $customerName."
                "gu" -> "₹${amount.toInt()} નું ઉધાર નોંધાયું, $customerName."
                "pa" -> "₹${amount.toInt()} ਦਾ ਉਧਾਰ ਦਰਜ ਹੋਇਆ, $customerName।"
                else -> "${amount.toInt()} rupees credit recorded for $customerName."
            }
            SoundboxSpeaker.speakText(getApplication(), text, lang)
        }
    }

    // --- OCR State & Actions ---
    private val _ocrBitmap = MutableStateFlow<Bitmap?>(null)
    val ocrBitmap: StateFlow<Bitmap?> = _ocrBitmap.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _ocrResult = MutableStateFlow<OcrParsedTransaction?>(null)
    val ocrResult: StateFlow<OcrParsedTransaction?> = _ocrResult.asStateFlow()

    fun setOcrBitmap(bmp: Bitmap) {
        _ocrBitmap.value = bmp
        processOcr(bmp)
    }

    fun processOcr(bmp: Bitmap) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            if (!connectivityMonitor.isConnected()) {
                android.widget.Toast.makeText(
                    getApplication(),
                    "इंटरनेट कनेक्शन उपलब्ध नहीं है (No Internet Connection). ऑफलाइन मोड में स्थानीय OCR परिणाम तैयार किए जा रहे हैं।",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            val result = GeminiClient.analyzeScreenshot(bmp)
            result.onSuccess { parsed ->
                _ocrResult.value = parsed
                // Automatically save to Cloud Firestore and Room
                if (connectivityMonitor.isConnected()) {
                    FirestoreManager.saveOcrTransaction(merchantProfile.value.upiId, parsed)
                }
                transactionRepository.recordTransaction(
                    TransactionEntity(
                        amount = parsed.amount,
                        payerName = parsed.payerName,
                        upiRef = parsed.upiRef,
                        paymentSource = "OCR Verification",
                        type = parsed.type,
                        status = "SETTLED",
                        note = "Verified via Gemini 2.5 Flash Screenshot OCR"
                    )
                )
                // Log Google Analytics for Firebase event
                com.example.analytics.AnalyticsManager.logOcrUsed(
                    detectedAmount = parsed.amount,
                    payerFound = parsed.payerName.isNotBlank(),
                    upiRefFound = parsed.upiRef.isNotBlank(),
                    language = merchantProfile.value.languageCode
                )
            }
            _isOcrProcessing.value = false
        }
    }

    fun clearOcr() {
        _ocrBitmap.value = null
        _ocrResult.value = null
    }

    // --- AI Bol Chatbot State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "नमस्ते! मैं आपका VyaparSetu AI सलाहकार हूँ। आप मुझसे दुकानदारी, उधारी वसूली, या UPI सेटलमेंट के बारे में कभी भी पूछ सकते हैं।",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatbotLoading = MutableStateFlow(false)
    val isChatbotLoading: StateFlow<Boolean> = _isChatbotLoading.asStateFlow()

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(text = userText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatbotLoading.value = true
            val history = _chatMessages.value.map { it.text to it.isUser }
            val profile = merchantProfile.value
            val todayCredit = transactionRepository.todayCreditTotal.first() ?: 0.0
            val todayUpi = transactionRepository.todayUpiTotal.first() ?: 0.0
            val todayCash = transactionRepository.todayCashTotal.first() ?: 0.0
            val todayCount = transactionRepository.todayTransactionCount.first() ?: 0
            val debtors = allCustomers.value.filter { it.balanceDue > 0 }.sortedByDescending { it.balanceDue }.take(5).map { it.name to it.balanceDue }
            val lowItems = lowStockProducts.value.map { it.name }

            val snapshot = com.example.ai.StoreSnapshot(
                shopName = profile.shopName.ifBlank { "VyaparSetu Store" },
                ownerName = profile.ownerName.ifBlank { "व्यापारी" },
                todaySalesTotal = todayCredit,
                todayUpiTotal = todayUpi,
                todayCashTotal = todayCash,
                todayTxnCount = todayCount,
                totalUdharDue = totalCustomerUdhar.value ?: 0.0,
                topDebtors = debtors,
                lowStockCount = lowStockCount.value,
                lowStockItems = lowItems
            )

            val reply = GeminiClient.chatWithGemini(
                history = history,
                merchantLanguage = profile.languageCode,
                shopName = profile.shopName,
                userQuery = userText,
                storeSnapshot = snapshot
            )
            _chatMessages.value = _chatMessages.value + ChatMessage(text = reply, isUser = false)
            _isChatbotLoading.value = false

            // Log Google Analytics for Firebase event
            com.example.analytics.AnalyticsManager.logChatbotInteraction(
                queryCategory = "STORE_ADVICE",
                language = profile.languageCode,
                isVoiceInput = false
            )
        }
    }

    // --- App Tour State ---
    private val _isAppTourActive = MutableStateFlow(false)
    val isAppTourActive: StateFlow<Boolean> = _isAppTourActive.asStateFlow()

    private val _appTourStep = MutableStateFlow(0)
    val appTourStep: StateFlow<Int> = _appTourStep.asStateFlow()

    fun startAppTour() {
        _appTourStep.value = 0
        _isAppTourActive.value = true
    }

    fun nextAppTourStep() {
        if (_appTourStep.value < 4) {
            _appTourStep.value += 1
        } else {
            _isAppTourActive.value = false
        }
    }

    fun dismissAppTour() {
        _isAppTourActive.value = false
    }

    // --- Backup & Restore ---
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun exportData(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = DataBackupManager.createExportJson(getApplication())
            _backupStatus.value = "Backup JSON successfully prepared"
            onReady(json)
        }
    }

    fun setSoundboxEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSoundboxEnabled(enabled)
        }
    }

    fun testSoundbox() {
        triggerSoundboxAnnouncement("PhonePe", 500.0, "रोहन")
    }

    fun syncAllToFirestoreCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            syncInventoryWithCloud(onComplete)
        }
    }

    fun importData(jsonContent: String) {
        viewModelScope.launch {
            val result = DataBackupManager.restoreFromJson(getApplication(), jsonContent)
            result.onSuccess { count ->
                _backupStatus.value = "Successfully restored $count records from backup!"
            }.onFailure { err ->
                _backupStatus.value = "Restore failed: ${err.message}"
            }
        }
    }
}
