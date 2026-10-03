package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<ShopProfile?>

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): ShopProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ShopProfile)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllStockFlow(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllStock(): List<StockItem>

    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1")
    suspend fun getStockById(id: String): StockItem?

    @Query("SELECT COUNT(*) FROM stock_items WHERE isArchived = 0 AND currentQty <= lowStockThreshold")
    fun getLowStockCountFlow(): Flow<Int>

    @Query("SELECT * FROM stock_items WHERE isArchived = 0 AND currentQty <= lowStockThreshold ORDER BY currentQty ASC")
    fun getLowStockItemsFlow(): Flow<List<StockItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(item: StockItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockBatch(items: List<StockItem>)

    @Update
    suspend fun updateStock(item: StockItem)

    @Query("UPDATE stock_items SET currentQty = currentQty + :qtyDelta, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun adjustStockQuantity(id: String, qtyDelta: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE stock_items SET isArchived = 1, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun archiveStock(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM stock_items WHERE synced = 0")
    suspend fun getUnsyncedStock(): List<StockItem>

    @Query("UPDATE stock_items SET synced = 1 WHERE id IN (:ids)")
    suspend fun markStockSynced(ids: List<String>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY balanceDue DESC, name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY balanceDue DESC, name ASC")
    suspend fun getAllCustomers(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT SUM(balanceDue) FROM customers WHERE balanceDue > 0")
    fun getTotalDuesFlow(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerBatch(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET balanceDue = balanceDue + :amountDelta, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun adjustCustomerBalance(id: String, amountDelta: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE synced = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("UPDATE customers SET synced = 1 WHERE id IN (:ids)")
    suspend fun markCustomersSynced(ids: List<String>)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY billDate DESC")
    fun getAllBillsFlow(): Flow<List<Bill>>

    @Query("SELECT * FROM bills ORDER BY billDate DESC")
    suspend fun getAllBills(): List<Bill>

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: String): Bill?

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    fun getBillItemsFlow(billId: String): Flow<List<BillItem>>

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getBillItems(billId: String): List<BillItem>

    @Query("SELECT * FROM bill_items")
    suspend fun getAllBillItems(): List<BillItem>

    @Query("SELECT * FROM bills WHERE billDate >= :startOfDay AND billDate <= :endOfDay AND status = 'active'")
    fun getTodayBillsFlow(startOfDay: Long, endOfDay: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE billDate >= :startOfDay AND billDate <= :endOfDay AND status = 'active'")
    suspend fun getTodayBills(startOfDay: Long, endOfDay: Long): List<Bill>

    @Query("SELECT * FROM bills WHERE billDate >= :startDate AND billDate <= :endDate AND status = 'active'")
    suspend fun getBillsInRange(startDate: Long, endDate: Long): List<Bill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillsBatch(bills: List<Bill>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItem>)

    @Query("UPDATE bills SET status = 'voided', synced = 0 WHERE id = :billId")
    suspend fun markBillVoided(billId: String)

    @Query("SELECT * FROM bills WHERE synced = 0")
    suspend fun getUnsyncedBills(): List<Bill>

    @Query("SELECT * FROM bill_items WHERE billId IN (:billIds)")
    suspend fun getBillItemsForBills(billIds: List<String>): List<BillItem>

    @Query("UPDATE bills SET synced = 1 WHERE id IN (:ids)")
    suspend fun markBillsSynced(ids: List<String>)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun getAllPurchasesFlow(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    suspend fun getAllPurchases(): List<Purchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasesBatch(purchases: List<Purchase>)

    @Query("SELECT * FROM purchases WHERE synced = 0")
    suspend fun getUnsyncedPurchases(): List<Purchase>

    @Query("UPDATE purchases SET synced = 1 WHERE id IN (:ids)")
    suspend fun markPurchasesSynced(ids: List<String>)
}

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_entries WHERE customerId = :customerId ORDER BY entryDate DESC")
    fun getEntriesForCustomerFlow(customerId: String): Flow<List<KhataEntry>>

    @Query("SELECT * FROM khata_entries WHERE customerId = :customerId ORDER BY entryDate DESC")
    suspend fun getEntriesForCustomer(customerId: String): List<KhataEntry>

    @Query("SELECT * FROM khata_entries ORDER BY entryDate DESC")
    suspend fun getAllKhataEntries(): List<KhataEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataBatch(entries: List<KhataEntry>)

    @Query("SELECT * FROM khata_entries WHERE synced = 0")
    suspend fun getUnsyncedKhata(): List<KhataEntry>

    @Query("UPDATE khata_entries SET synced = 1 WHERE id IN (:ids)")
    suspend fun markKhataSynced(ids: List<String>)
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments ORDER BY adjustmentDate DESC")
    fun getAllAdjustmentsFlow(): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments ORDER BY adjustmentDate DESC")
    suspend fun getAllAdjustments(): List<StockAdjustment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustmentBatch(adjustments: List<StockAdjustment>)

    @Query("SELECT * FROM stock_adjustments WHERE synced = 0")
    suspend fun getUnsyncedAdjustments(): List<StockAdjustment>

    @Query("UPDATE stock_adjustments SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAdjustmentsSynced(ids: List<String>)
}
