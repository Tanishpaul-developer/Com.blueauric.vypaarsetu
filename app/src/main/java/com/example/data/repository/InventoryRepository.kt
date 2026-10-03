package com.example.data.repository

import com.example.data.firebase.FirestoreManager
import com.example.data.local.ProductDao
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InventoryRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val totalInventoryValue: Flow<Double?> = productDao.getTotalInventoryValue()
    val recentMovements: Flow<List<StockMovementEntity>> = productDao.getRecentStockMovements()

    suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    suspend fun addProduct(product: ProductEntity, merchantUpi: String): Long {
        val id = productDao.insertProduct(product)
        val saved = product.copy(id = id)
        // Log initial stock movement if quantity > 0
        if (product.stockQuantity > 0) {
            val movement = StockMovementEntity(
                productId = id,
                productName = product.name,
                quantityDelta = product.stockQuantity,
                reason = "Initial Stock Setup"
            )
            productDao.insertStockMovement(movement)
            FirestoreManager.logStockMovement(merchantUpi, movement)
        }
        FirestoreManager.syncProduct(merchantUpi, saved)
        return id
    }

    suspend fun updateProduct(product: ProductEntity, merchantUpi: String) {
        productDao.updateProduct(product)
        FirestoreManager.syncProduct(merchantUpi, product)
    }

    suspend fun deleteProduct(productId: Long, merchantUpi: String) {
        productDao.deleteProduct(productId)
    }

    /**
     * Updates stock for an item: positive for restock / arrival, negative for sale.
     * Returns true if stock is now below threshold (low-stock alert condition).
     */
    suspend fun adjustStock(
        productId: Long,
        delta: Double,
        reason: String,
        merchantUpi: String
    ): Pair<ProductEntity?, Boolean> {
        val current = productDao.getProductById(productId) ?: return Pair(null, false)
        val newStock = (current.stockQuantity + delta).coerceAtLeast(0.0)

        productDao.updateStock(productId, delta)
        val updated = current.copy(stockQuantity = newStock)

        val movement = StockMovementEntity(
            productId = productId,
            productName = current.name,
            quantityDelta = delta,
            reason = reason
        )
        productDao.insertStockMovement(movement)

        FirestoreManager.syncProduct(merchantUpi, updated)
        FirestoreManager.logStockMovement(merchantUpi, movement)

        val isLowStock = newStock <= current.lowStockThreshold
        return Pair(updated, isLowStock)
    }

    suspend fun syncAllWithCloud(merchantUpi: String): Boolean {
        val list = productDao.getAllProductsList()
        return FirestoreManager.syncCatalogList(merchantUpi, list)
    }
}
