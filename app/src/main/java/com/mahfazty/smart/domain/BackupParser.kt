package com.mahfazty.smart.domain

import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import org.json.JSONObject

/**
 * عزل منطق قراءة ملف النسخة الاحتياطية عن قاعدة البيانات (إصلاح استيراد النسخ الاحتياطية).
 *
 * سابقاً كان التحليل والتحقق يحدثان داخل importBackup وسط حلقات إدخال Room، فأي سجل
 * معطوب يُسقط الاستيراد كله برسالة غامضة «تعذر قراءة الملف» دون معرفة السبب أو موقعه.
 *
 * الآن: BackupParser يقرأ JSON ويتحقق من كل سجل قبل لمس قاعدة البيانات، ويعيد إما
 * سجلات مجردة جاهزة للإدخال، أو قائمة أخطاء دقيقة الموقع (القسم + رقم السجل + المفتاح).
 *
 * تحمّل الصيغ: BOM، مسافات زائدة، أنواع ENUM بأي حالة أحرف، أرقام نصية ("250")،
 * وحقول الإصدارات الأقدم المفقودة (status/dueDate/currency/isInvoice...).
 */
object BackupParser {

    /** خطأ دقيق الموقع في ملف النسخة الاحتياطية */
    data class BackupError(val section: String, val index: Int, val message: String) {
        override fun toString(): String =
            if (index >= 0) "$section — السجل رقم ${index + 1}: $message" else "$section: $message"
    }

    data class ParsedGoal(val oldId: Long, val name: String, val target: Double, val icon: String, val opening: Double)
    data class ParsedTx(
        val oldGoalId: Long?, val type: TxType, val amount: Double, val category: String,
        val note: String?, val date: Long, val wallet: Wallet,
    )
    data class ParsedClient(val oldId: Long, val name: String, val phone: String?, val photoPath: String?, val status: String)
    data class ParsedAccount(val oldId: Long, val oldClientId: Long, val name: String, val icon: String, val realBalance: Double)
    data class ParsedOperation(
        val oldAccountId: Long, val type: OpType, val amount: Double, val note: String?, val date: Long,
        val materialsJson: String?, val receiptPath: String?, val isInvoice: Boolean,
        val invoiceRef: String?, val invoiceDelivered: Boolean, val dueDate: Long?, val currency: String?,
    )
    data class ParsedTransfer(
        val fromClientId: Long, val fromAccountId: Long, val toClientId: Long, val toAccountId: Long,
        val amount: Double, val date: Long,
    )

    data class ParsedBackup(
        val transactions: List<ParsedTx>,
        val goals: List<ParsedGoal>,
        val hasSavings: Boolean,
        val savingsOpening: Double,
        val savingsGoal: Double,
        val settings: Map<String, String>,
        val clients: List<ParsedClient>,
        val accounts: List<ParsedAccount>,
        val operations: List<ParsedOperation>,
        val transfers: List<ParsedTransfer>,
    )

    data class ParseResult(val ok: Boolean, val errors: List<BackupError>, val backup: ParsedBackup?) {
        /** رسالة جاهزة للعرض في توست — أول خطأين + عدّاد الباقي */
        fun errorMessage(): String {
            if (errors.isEmpty()) return "تعذر قراءة الملف"
            val shown = errors.take(2).joinToString("\n")
            val rest = errors.size - minOf(2, errors.size)
            return if (rest > 0) "الملف يحتوي $rest+ مشكلة:\n$shown" else "الملف يحتوي مشكلة:\n$shown"
        }
    }

    /** استثناء داخلي يحمل موضع المشكلة بدقة */
    private class Bad(val section: String, val index: Int, val msg: String) : Exception(msg)

    // ===== قراءة الحقول بتسامح (نص رقمي / حالة أحرف / مفتاح مفقود) =====

    private fun num(o: JSONObject, key: String, section: String, index: Int): Double {
        if (!o.has(key) || o.isNull(key)) throw Bad(section, index, "المفتاح المطلوب «$key» مفقود")
        val v = o.get(key)
        if (v is Number) return v.toDouble()
        if (v is String) {
            val d = v.trim().toDoubleOrNull()
                ?: throw Bad(section, index, "«$key» ليس رقمًا: \"$v\"")
            return d
        }
        throw Bad(section, index, "«$key» نوع غير متوقع: $v")
    }

    private fun numOr(o: JSONObject, key: String, fallback: Double): Double {
        if (!o.has(key) || o.isNull(key)) return fallback
        val v = o.get(key)
        if (v is Number) return v.toDouble()
        if (v is String) return v.trim().toDoubleOrNull() ?: fallback
        return fallback
    }

    private fun longOr(o: JSONObject, key: String, fallback: Long): Long {
        if (!o.has(key) || o.isNull(key)) return fallback
        val v = o.get(key)
        if (v is Number) return v.toLong()
        if (v is String) return v.trim().toDoubleOrNull()?.toLong() ?: fallback
        return fallback
    }

    private fun longReq(o: JSONObject, key: String, section: String, index: Int): Long {
        if (!o.has(key) || o.isNull(key)) throw Bad(section, index, "المفتاح المطلوب «$key» مفقود")
        val v = o.get(key)
        if (v is Number) return v.toLong()
        if (v is String) {
            val l = v.trim().toDoubleOrNull()?.toLong()
                ?: throw Bad(section, index, "«$key» ليس تاريخًا صحيحًا: \"$v\"")
            return l
        }
        throw Bad(section, index, "«$key» نوع غير متوقع: $v")
    }

    private fun strReq(o: JSONObject, key: String, section: String, index: Int): String {
        if (!o.has(key) || o.isNull(key)) throw Bad(section, index, "المفتاح المطلوب «$key» مفقود")
        return o.get(key).toString()
    }

    /** نص اختياري — null إذا المفتاح مفقود أو JSON null، فارغ يبقى كما هو (نفس سلوك الاستيراد القديم) */
    private fun strOpt(o: JSONObject, key: String): String? =
        if (!o.has(key) || o.isNull(key)) null else o.get(key).toString()

    /** نص اختياري مع تحويل الفارغ إلى null (لinvoiceRef وcurrency) */
    private fun strOptBlankToNull(o: JSONObject, key: String): String? =
        strOpt(o, key)?.ifBlank { null }

    /** enum بتسامح حالة الأحرف — يرمي خطأ موقع دقيق بدل الفشل الصامت أو الانهيار */
    private inline fun <reified E : Enum<E>> enumReq(
        o: JSONObject, key: String, section: String, index: Int, known: String,
    ): E {
        val raw = strReq(o, key, section, index).trim().uppercase()
        val value = enumValues<E>().firstOrNull { it.name == raw }
            ?: throw Bad(section, index, "قيمة «$key» غير معروفة: \"$raw\" (المعروفة: $known)")
        return value
    }

    private fun boolOr(o: JSONObject, key: String, fallback: Boolean): Boolean {
        if (!o.has(key) || o.isNull(key)) return fallback
        val v = o.get(key)
        if (v is Boolean) return v
        if (v is String) return v.trim().equals("true", ignoreCase = true)
        if (v is Number) return v.toDouble() != 0.0
        return fallback
    }

    // ===== التحليل الكامل =====

    fun parse(text0: String): ParseResult {
        // تحمّل BOM والمسافات المحيطة (بعض التطبيقات وأدوات المشاركة تضيف \uFEFF)
        val text = text0.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) return ParseResult(false, listOf(BackupError("الملف", -1, "الملف فارغ — تعذر قراءة محتواه")), null)

        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            return ParseResult(false, listOf(BackupError("الملف", -1, "ليس ملف JSON صالحًا: ${e.message}")), null)
        }
        val data = root.optJSONObject("data")
            ?: return ParseResult(false, listOf(BackupError("الملف", -1, "لا يحتوي على قسم البيانات «data» — ليس نسخة احتياطية من هذا التطبيق")), null)

        val errors = mutableListOf<BackupError>()

        // الأهداف
        val goals = mutableListOf<ParsedGoal>()
        data.optJSONArray("goals")?.let { arr ->
            for (i in 0 until arr.length()) runCatching {
                val o = arr.getJSONObject(i)
                goals += ParsedGoal(
                    oldId = longOr(o, "id", -1L),
                    name = strReq(o, "name", "الأهداف", i),
                    target = num(o, "target", "الأهداف", i),
                    icon = strOpt(o, "icon") ?: "💻",
                    opening = numOr(o, "opening", 0.0),
                )
            }.onFailure { if (it is Bad) errors += BackupError(it.section, it.index, it.msg) }
        }

        // العمليات المالية
        val txs = mutableListOf<ParsedTx>()
        data.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) runCatching {
                val o = arr.getJSONObject(i)
                txs += ParsedTx(
                    oldGoalId = if (o.isNull("goalId")) null else longOr(o, "goalId", -1L).takeIf { it > 0 },
                    type = enumReq<TxType>(o, "type", "العمليات المالية", i, "INCOME / EXPENSE / TRANSFER"),
                    amount = num(o, "amount", "العمليات المالية", i),
                    category = strReq(o, "category", "العمليات المالية", i),
                    note = strOpt(o, "note"),
                    date = longReq(o, "date", "العمليات المالية", i),
                    wallet = enumReq<Wallet>(o, "wallet", "العمليات المالية", i, "BANK / CASH"),
                )
            }.onFailure { if (it is Bad) errors += BackupError(it.section, it.index, it.msg) }
        }

        // الادخار
        var savingsOpening = 0.0
        var savingsGoal = 50_000.0
        var hasSavings = false
        data.optJSONObject("savings")?.let { s ->
            hasSavings = true
            savingsOpening = numOr(s, "opening", 0.0)
            savingsGoal = numOr(s, "goal", 50_000.0)
        }

        // الإعدادات — فقط القيم غير الفارغة (نفس سلوك الاستيراد القديم)
        val settings = mutableMapOf<String, String>()
        data.optJSONObject("settings")?.let { s ->
            val keys = s.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (!s.isNull(k)) {
                    val v = s.get(k).toString()
                    if (v.isNotBlank()) settings[k] = v
                }
            }
        }

        // العملاء
        val clients = mutableListOf<ParsedClient>()
        data.optJSONArray("clients")?.let { arr ->
            for (i in 0 until arr.length()) runCatching {
                val o = arr.getJSONObject(i)
                clients += ParsedClient(
                    oldId = longOr(o, "id", -1L),
                    name = strReq(o, "name", "العملاء", i),
                    phone = strOpt(o, "phone"),
                    photoPath = strOpt(o, "photoPath"),
                    // النسخ القديمة بلا هذا المفتاح → «نشط» (إصلاح data-3)
                    status = strOpt(o, "status") ?: "active",
                )
            }.onFailure { if (it is Bad) errors += BackupError(it.section, it.index, it.msg) }
        }

        // حسابات العملاء
        val accounts = mutableListOf<ParsedAccount>()
        data.optJSONArray("accounts")?.let { arr ->
            for (i in 0 until arr.length()) runCatching {
                val o = arr.getJSONObject(i)
                accounts += ParsedAccount(
                    oldId = longOr(o, "id", -1L),
                    oldClientId = longOr(o, "clientId", -1L),
                    name = strReq(o, "name", "حسابات العملاء", i),
                    icon = strOpt(o, "icon") ?: "💰",
                    realBalance = numOr(o, "realBalance", 0.0),
                )
            }.onFailure { if (it is Bad) errors += BackupError(it.section, it.index, it.msg) }
        }

        // عمليات العملاء (دين/سداد)
        val ops = mutableListOf<ParsedOperation>()
        data.optJSONArray("operations")?.let { arr ->
            for (i in 0 until arr.length()) runCatching {
                val o = arr.getJSONObject(i)
                ops += ParsedOperation(
                    oldAccountId = longOr(o, "accountId", -1L),
                    type = enumReq<OpType>(o, "type", "عمليات العملاء", i, "DEBT / PAY"),
                    amount = num(o, "amount", "عمليات العملاء", i),
                    note = strOpt(o, "note"),
                    date = longReq(o, "date", "عمليات العملاء", i),
                    materialsJson = strOpt(o, "materials"),
                    receiptPath = strOpt(o, "receiptPath"),
                    isInvoice = boolOr(o, "isInvoice", false),
                    invoiceRef = strOptBlankToNull(o, "invoiceRef"),
                    invoiceDelivered = boolOr(o, "invoiceDelivered", false),
                    // حقول v2.7.0 — النسخ القديمة بلاها (إصلاح data-1/data-4)
                    dueDate = if (o.isNull("dueDate")) null else longOr(o, "dueDate", 0L).takeIf { it > 0 },
                    currency = strOptBlankToNull(o, "currency"),
                )
            }.onFailure { if (it is Bad) errors += BackupError(it.section, it.index, it.msg) }
        }

        // سجل التحويلات
        val transfers = mutableListOf<ParsedTransfer>()
        data.optJSONArray("transfers")?.let { arr ->
            for (i in 0 until arr.length()) runCatching {
                val o = arr.getJSONObject(i)
                transfers += ParsedTransfer(
                    fromClientId = longOr(o, "fromClientId", -1L),
                    fromAccountId = longOr(o, "fromAccountId", -1L),
                    toClientId = longOr(o, "toClientId", -1L),
                    toAccountId = longOr(o, "toAccountId", -1L),
                    amount = num(o, "amount", "التحويلات", i),
                    date = longReq(o, "date", "التحويلات", i),
                )
            }.onFailure { if (it is Bad) errors += BackupError(it.section, it.index, it.msg) }
        }

        if (errors.isNotEmpty()) return ParseResult(false, errors, null)
        return ParseResult(
            true, emptyList(),
            ParsedBackup(txs, goals, hasSavings, savingsOpening, savingsGoal, settings, clients, accounts, ops, transfers),
        )
    }
}
