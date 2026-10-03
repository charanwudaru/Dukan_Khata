package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shop_profile")
data class ShopProfile(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val gstin: String = "",
    val address: String = "",
    val isInitialized: Boolean = false,
    val language: String = "en", // "en" or "hi"
    val isPremium: Boolean = false
)

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String = "piece", // kg, litre, piece, dozen, box, metre
    val currentQty: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Int = 0 // 0 = unsynced, 1 = synced
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val balanceDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Int = 0
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billNumber: String = "",
    val billType: String = "invoice", // "invoice", "quotation", "receipt"
    val customerId: String? = null,
    val customerNameSnapshot: String = "Walk-in Customer",
    val customerPhoneSnapshot: String = "",
    val billDate: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val gstAmount: Double = 0.0,
    val gstRatePercent: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paymentMode: String = "cash", // "cash", "credit", "upi"
    val status: String = "active", // "active", "voided"
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Int = 0
)

@Entity(tableName = "bill_items")
data class BillItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billId: String,
    val stockItemId: String,
    val itemNameSnapshot: String,
    val qty: Double,
    val unit: String = "piece",
    val pricePerUnit: Double,
    val purchasePriceSnapshot: Double = 0.0,
    val lineTotal: Double
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val stockItemId: String,
    val stockItemName: String,
    val qty: Double,
    val purchasePrice: Double,
    val purchaseDate: Long = System.currentTimeMillis(),
    val supplierName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Int = 0
)

@Entity(tableName = "khata_entries")
data class KhataEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val entryType: String, // "credit_bill", "payment_received"
    val amount: Double,
    val billId: String? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Int = 0
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val stockItemId: String,
    val stockItemName: String,
    val qtyChange: Double,
    val reason: String, // "damage", "correction", "returned"
    val adjustmentDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Int = 0
)
