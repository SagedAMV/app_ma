package com.mahfazty.smart.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.mahfazty.smart.domain.model.Client
import com.mahfazty.smart.domain.model.ClientAccount
import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.Goal
import com.mahfazty.smart.domain.model.MaterialItem
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.RealTransfer
import com.mahfazty.smart.domain.model.SavingsAccount
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import org.json.JSONArray
import org.json.JSONObject

/** عملية مالية */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val note: String?,
    val date: Long,
    val goalId: Long?,
    val wallet: String,
)

/** هدف ادخاري */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val target: Double,
    val icon: String,
    val opening: Double,
)

/** حصالة الادخار — صف واحد دائم (id=1) */
@Entity(tableName = "savings")
data class SavingsEntity(
    @PrimaryKey val id: Int = 1,
    val opening: Double,
    val goal: Double,
)

/** إعداد — تخزين مفتاح/قيمة (مرن مثل localStorage) */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** عميل */
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val phone: String?,
    val photoPath: String?,
    /** حالة العميل: نشط/موقوف/قائمة سوداء (إصلاح data-3) — الافتراضي «نشط» للصفوف القديمة */
    @ColumnInfo(defaultValue = "active")
    val status: String = "active",
)

/** سجل تدقيق: من عدّل/حذف/حوّل ماذا ومتى (إصلاح sec-6) */
@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey val id: Long,
    val ts: Long,
    val action: String,
    val details: String,
)

/** حساب عميل (ماطور، كهرباء...) */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Long,
    val clientId: Long,
    val name: String,
    val icon: String,
    val realBalance: Double,
)

/** عملية على حساب عميل (دين/سداد) مع مواد الفاتورة كـ JSON */
@Entity(tableName = "operations")
data class OperationEntity(
    @PrimaryKey val id: Long,
    val accountId: Long,
    val type: String,
    val amount: Double,
    val note: String?,
    val date: Long,
    val materialsJson: String?,
    val receiptPath: String?,
    /** حقول الفواتير (نظام «الفواتير وحالة التسليم») — وصفية بحتة */
    @ColumnInfo(defaultValue = "0")
    val isInvoice: Boolean = false,
    val invoiceRef: String? = null,
    @ColumnInfo(defaultValue = "0")
    val invoiceDelivered: Boolean = false,
    /** تاريخ استحقاق الدين — null = بلا استحقاق (إصلاح data-1) */
    val dueDate: Long? = null,
    /** العملة المثبتة وقت العملية — null = عملة الإعدادات وقت العرض (إصلاح data-4) */
    val currency: String? = null,
)

/** تحويل رصيد حقيقي بين حسابات */
@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val id: Long,
    val fromClientId: Long,
    val fromAccountId: Long,
    val toClientId: Long,
    val toAccountId: Long,
    val amount: Double,
    val date: Long,
)

// ============ تحويلات الكيانات ↔ نماذج النطاق ============

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id, type = runCatching { TxType.valueOf(type) }.getOrDefault(TxType.EXPENSE), amount = amount, category = category,
    note = note, date = date, goalId = goalId,
    wallet = runCatching { Wallet.valueOf(wallet) }.getOrDefault(Wallet.CASH),
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id, type = type.name, amount = amount, category = category,
    note = note, date = date, goalId = goalId, wallet = wallet.name,
)

fun GoalEntity.toDomain(): Goal = Goal(id = id, name = name, target = target, icon = icon, opening = opening)

fun Goal.toEntity(): GoalEntity = GoalEntity(id = id, name = name, target = target, icon = icon, opening = opening)

fun SavingsEntity.toDomain(): SavingsAccount = SavingsAccount(opening = opening, goal = goal)

fun ClientEntity.toDomain(): Client =
    Client(id = id, name = name, phone = phone, photoPath = photoPath, status = status)

fun Client.toEntity(): ClientEntity =
    ClientEntity(id = id, name = name, phone = phone, photoPath = photoPath, status = status)

fun AccountEntity.toDomain(): ClientAccount =
    ClientAccount(id = id, clientId = clientId, name = name, icon = icon, realBalance = realBalance)

fun OperationEntity.toDomain(): ClientOperation = ClientOperation(
    id = id, accountId = accountId,
    type = runCatching { OpType.valueOf(type) }.getOrDefault(OpType.DEBT),
    amount = amount, note = note, date = date,
    materials = materialsJson.toMaterials(), receiptPath = receiptPath,
    isInvoice = isInvoice, invoiceRef = invoiceRef, invoiceDelivered = invoiceDelivered,
    dueDate = dueDate, currency = currency,
)

fun ClientOperation.toEntity(): OperationEntity = OperationEntity(
    id = id, accountId = accountId, type = type.name, amount = amount,
    note = note, date = date,
    materialsJson = materials.toJsonOrNull(), receiptPath = receiptPath,
    isInvoice = isInvoice, invoiceRef = invoiceRef, invoiceDelivered = invoiceDelivered,
    dueDate = dueDate, currency = currency,
)

fun TransferEntity.toDomain(): RealTransfer = RealTransfer(
    id = id, fromClientId = fromClientId, fromAccountId = fromAccountId,
    toClientId = toClientId, toAccountId = toAccountId, amount = amount, date = date,
)

// تسلسل مواد الفاتورة إلى JSON والعكس (باستخدام org.json المدمج في أندرويد)

fun List<MaterialItem>.toJsonOrNull(): String? {
    if (isEmpty()) return null
    val arr = JSONArray()
    forEach { m ->
        arr.put(JSONObject().apply {
            put("name", m.name); put("qty", m.qty); put("unitPrice", m.unitPrice)
        })
    }
    return arr.toString()
}

fun String?.toMaterials(): List<MaterialItem> {
    if (isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(this)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MaterialItem(
                name = o.optString("name"),
                qty = o.optDouble("qty"),
                unitPrice = o.optDouble("unitPrice"),
            )
        }
    }.getOrDefault(emptyList())
}
