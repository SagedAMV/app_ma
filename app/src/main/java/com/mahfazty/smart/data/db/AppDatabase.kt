package com.mahfazty.smart.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class, GoalEntity::class, SavingsEntity::class,
        SettingEntity::class, ClientEntity::class, AccountEntity::class,
        OperationEntity::class, TransferEntity::class, AuditLogEntity::class,
    ],
    version = 3,
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
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * سجل الترحيلات — إصلاح A1:
         * عند أي تعديل مستقبلي على المخطط: ارفع version وأضف Migration(قديم, جديد) هنا.
         * المخططات تُصدَّر تلقائياً إلى app/schemas (بفضل room.schemaLocation) للمقارنة وكتابة ترحيل صحيح.
         *
         * v1 → v2 (نظام «الفواتير وحالة التسليم» على عمليات العملاء):
         *   إضافة أعمدة وصفية بحتة إلى جدول operations:
         *     isInvoice         (INTEGER NOT NULL DEFAULT 0) — هل هي فاتورة؟
         *     invoiceRef        (TEXT)                        — رقم/وصف الفاتورة (اختياري فارغ)
         *     invoiceDelivered  (INTEGER NOT NULL DEFAULT 0) — هل سُلّمت؟
         *   الأعمدة الافتراضية تجعل كل العمليات القديمة «غير فاتورة» — لا تغيّر أي حساب مالي.
         */
        private val MIGRATIONS = arrayOf<Migration>(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE operations ADD COLUMN isInvoice INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE operations ADD COLUMN invoiceRef TEXT")
                    db.execSQL("ALTER TABLE operations ADD COLUMN invoiceDelivered INTEGER NOT NULL DEFAULT 0")
                }
            },
            /**
             * v2 → v3 (حزمة الأمان v2.7.0):
             *   - جدول audit_log جديد: سجل التدقيق للتعديلات والحذف (sec-6)
             *   - operations.dueDate: تاريخ استحقاق الدين (data-1)
             *   - operations.currency: عملة العملية المثبتة وقت التسجيل (data-4)
             *   - clients.status: حالة العميل نشط/موقوف/قائمة سوداء (data-3)
             * الأعمدة الجديدة اختيارية (أو بافتراضي آمن) — كل البيانات القديمة تبقى كما هي.
             */
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS audit_log (" +
                            "id INTEGER NOT NULL PRIMARY KEY, " +
                            "ts INTEGER NOT NULL, " +
                            "action TEXT NOT NULL, " +
                            "details TEXT NOT NULL)"
                    )
                    db.execSQL("ALTER TABLE operations ADD COLUMN dueDate INTEGER")
                    db.execSQL("ALTER TABLE operations ADD COLUMN currency TEXT")
                    db.execSQL("ALTER TABLE clients ADD COLUMN status TEXT NOT NULL DEFAULT 'active'")
                }
            },
        )

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
