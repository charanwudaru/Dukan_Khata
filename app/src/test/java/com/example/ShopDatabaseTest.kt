package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Bill
import com.example.data.model.BillItem
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ShopRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShopRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAddStockAndDeductOnBill() = runBlocking {
        // 1. Add Stock
        val item = repository.addStockItem(
            name = "Sugar",
            unit = "kg",
            openingQty = 50.0,
            purchasePrice = 36.0,
            sellingPrice = 42.0,
            lowStockThreshold = 10.0
        )

        val stockList = repository.allStockFlow.first()
        assertEquals(1, stockList.size)
        assertEquals(50.0, stockList[0].currentQty, 0.01)

        // 2. Create Bill with 5kg sugar
        val billId = UUID.randomUUID().toString()
        val bill = Bill(
            id = billId,
            billNumber = "INV-1001",
            billType = "invoice",
            customerId = null,
            customerNameSnapshot = "Walk-in Customer",
            customerPhoneSnapshot = "",
            billDate = System.currentTimeMillis(),
            subtotal = 210.0,
            gstAmount = 0.0,
            discount = 0.0,
            total = 210.0,
            paymentMode = "cash",
            status = "active",
            createdAt = System.currentTimeMillis(),
            synced = 0
        )

        val billItem = BillItem(
            id = UUID.randomUUID().toString(),
            billId = billId,
            stockItemId = item.id,
            itemNameSnapshot = item.name,
            qty = 5.0,
            unit = item.unit,
            pricePerUnit = 42.0,
            purchasePriceSnapshot = 36.0,
            lineTotal = 210.0
        )

        repository.createBill(bill, listOf(billItem))

        // Check stock was deducted: 50 - 5 = 45kg
        val updatedStock = repository.allStockFlow.first()
        assertEquals(45.0, updatedStock[0].currentQty, 0.01)

        // 3. Void Bill -> Check stock restored to 50kg
        repository.voidBill(billId)
        val restoredStock = repository.allStockFlow.first()
        assertEquals(50.0, restoredStock[0].currentQty, 0.01)
    }

    @Test
    fun testCustomerKhataCreditAndPayment() = runBlocking {
        // 1. Add Customer
        val customer = repository.addCustomer("Ramesh Kumar", "9876543210", 100.0)
        assertEquals(100.0, customer.balanceDue, 0.01)

        // 2. Record Credit sale
        repository.recordKhataCredit(customer.id, 250.0, "Goods taken on credit")
        val customersAfterCredit = repository.allCustomersFlow.first()
        assertEquals(350.0, customersAfterCredit[0].balanceDue, 0.01)

        // 3. Record Payment
        repository.recordKhataPayment(customer.id, 200.0, "Cash received")
        val customersAfterPayment = repository.allCustomersFlow.first()
        assertEquals(150.0, customersAfterPayment[0].balanceDue, 0.01)
    }
}
