package com.mahfazty.smart.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        TransactionEntity::class, GoalEntity::class, SavingsEntity::class,
        SettingEntity::class, ClientEntity::class, AccountEntity::class,
        OperationEntity::class, TransferEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun savingsDao(): SavingsDao
    abstract fun settingsDao(): SettingsDao
    abstract fun clientDao(): ClientDao
    abstract fun accountDao(): AccountDao
    abstract fun operationDao(): OperationDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * سجل الترحيلات — إصلاح A1:
         * عند أي تعديل مستقبلي على المخطط: ارفع version وأضف Migration(قديم, جديد) هنا.
         * المخططات تُصدَّر تلقائياً إلى app/schemas (بفضل room.schemaLocation) للمقارنة وكتابة ترحيل صحيح.
         */
        private val MIGRATIONS = arrayOf<Migration>()

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mahfazty.db",
                )
                    .addMigrations(*MIGRATIONS)
                    // أُزيل fallbackToDestructiveMigration عمداً:
                    // لا مسح صامت لبيانات المستخدم المالية مهما حدث —
                    // أي ترقية مخطط بلا ترحيل صريح تظهر كخطأ واضح يُصلح، لا كفقدان بيانات خفي.
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
