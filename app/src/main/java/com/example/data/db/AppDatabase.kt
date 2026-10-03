package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        ShopProfile::class,
        StockItem::class,
        Customer::class,
        Bill::class,
        BillItem::class,
        Purchase::class,
        KhataEntry::class,
        StockAdjustment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    abstract fun stockDao(): StockDao
    abstract fun customerDao(): CustomerDao
    abstract fun billDao(): BillDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun khataDao(): KhataDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dukan_khata.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
