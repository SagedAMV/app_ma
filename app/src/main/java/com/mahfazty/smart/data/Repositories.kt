package com.mahfazty.smart.data

import androidx.room.withTransaction
import com.mahfazty.smart.data.db.AccountEntity
import com.mahfazty.smart.data.db.AppDatabase
import com.mahfazty.smart.data.db.ClientEntity
import com.mahfazty.smart.data.db.GoalEntity
import com.mahfazty.smart.data.db.OperationEntity
import com.mahfazty.smart.data.db.SettingEntity
import com.mahfazty.smart.data.db.SavingsEntity
import com.mahfazty.smart.data.db.TransactionEntity
import com.mahfazty.smart.data.db.TransferEntity
import com.mahfazty.smart.data.db.toDomain
import com.mahfazty.smart.data.db.toEntity
import com.mahfazty.smart.data.db.toJsonOrNull
import com.mahfazty.smart.data.db.toMaterials
import com.mahfazty.smart.domain.Ids
import com.mahfazty.smart.domain.WalletEngine
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.Category
import com.mahfazty.smart.domain.model.CategoryIds
import com.mahfazty.smart.domain.model.CategoryKind
import com.mahfazty.smart.domain.model.Client
import com.mahfazty.smart.domain.model.ClientAccount
import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.Goal
import com.mahfazty.smart.domain.model.GoalWithSaved
import com.mahfazty.smart.domain.model.MaterialItem
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.RealTransfer
import com.mahfazty.smart.domain.model.SavingsAccount
import com.mahfazty.smart.domain.model.ThemeMode
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.domain.model.WalletError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

// =====================================================================
// 1) مستودع الإعدادات — مصدر وحيد لحالة التطبيق (اسم، عملة، مظهر، حدود...)
// =====================================================================

class SettingsRepository(private val db: AppDatabase) {

    val settings: Flow<AppSettings> = db.settingsDao().observeAll().map { list ->
        parse(list.associate { it.key to it.value })
    }

    val openingBank: Flow<Double> = db.settingsDao().observeAll().map { list ->
        list.firstOrNull { it.key == "opening_bank" }?.value?.toDoubleOrNull() ?: 0.0
    }

    val openingCash: Flow<Double> = db.settingsDao().observeAll().map { list ->
        list.firstOrNull { it.key == "opening_cash" }?.value?.toDoubleOrNull() ?: 0.0
    }

    suspend fun set(key: String, value: String) = db.settingsDao().upsert(SettingEntity(key, value))

    suspend fun setBudget(catId: String, value: Double) = set("budget_$catId", value.toString())

    suspend fun setOpeningBank(value: Double) = set("opening_bank", value.toString())

    suspend fun setOpeningCash(value: Double) = set("opening_cash", value.toString())

    suspend fun setCustomCategories(kind: CategoryKind, categories: List<Category>) {
        val arr = JSONArray()
        categories.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("icon", c.icon); put("name", c.name)
            })
        }
        set(if (kind == CategoryKind.EXPENSE) "custom_expense" else "custom_income", arr.toString())
    }

    suspend fun openingBankSync(): Double =
        db.settingsDao().getAll().firstOrNull { it.key == "opening_bank" }?.value?.toDoubleOrNull() ?: 0.0

    suspend fun openingCashSync(): Double =
        db.settingsDao().getAll().firstOrNull { it.key == "opening_cash" }?.value?.toDoubleOrNull() ?: 0.0

    private fun parse(map: Map<String, String>): AppSettings {
        val budgets = AppSettings.defaultBudgets().toMutableMap()
        map.forEach { (k, v) ->
            if (k.startsWith("budget_")) budgets[k.removePrefix("budget_")] = v.toDoubleOrNull() ?: 0.0
        }
        return AppSettings(
            name = map["name"] ?: "أحمد",
            bankName = map["bank_name"] ?: "البنك",
            cashName = map["cash_name"] ?: "الكاش",
            currency = map["currency"] ?: "ر.ي",
            theme = when (map["theme"]) {
                "dark" -> ThemeMode.DARK
                "system" -> ThemeMode.SYSTEM
                else -> ThemeMode.LIGHT
            },
            primaryColor = map["primary_color"] ?: "#6C5CE7",
            primary2 = map["primary_color2"] ?: "#A29BFE",
            hideBalance = map["hide_balance"] == "true",
            hideSavings = map["hide_savings"] == "true",
            savingsGoal = map["savings_goal"]?.toDoubleOrNull() ?: 50_000.0,
            budgets = budgets,
            customExpense = parseCategories(map["custom_expense"], CategoryKind.EXPENSE),
            customIncome = parseCategories(map["custom_income"], CategoryKind.INCOME),
        )
    }

    private fun parseCategories(json: String?, kind: CategoryKind): List<Category> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Category(o.optString("id"), o.optString("icon"), o.optString("name"), kind)
            }
        }.getOrDefault(emptyList())
    }
}

// =====================================================================
// 2) مستودع المحفظة — العمليات والأهداف والادخار + قواعد منع السالب
// =====================================================================

class WalletRepository(
    private val db: AppDatabase,
    private val settingsRepo: SettingsRepository,
) {
    val transactions: Flow<List<Transaction>> =
        db.transactionDao().observeAll().map { list -> list.map { it.toDomain() } }

    val goals: Flow<List<Goal>> =
        db.goalDao().observeAll().map { list -> list.map { it.toDomain() } }

    val savings: Flow<SavingsAccount> =
        db.savingsDao().observe().map { it?.toDomain() ?: SavingsAccount() }

    val bankBalance: Flow<Double> = combine(settingsRepo.openingBank, transactions) { o, t ->
        WalletEngine.bankBalance(o, t)
    }

    val cashBalance: Flow<Double> = combine(settingsRepo.openingCash, transactions) { o, t ->
        WalletEngine.cashBalance(o, t)
    }

    val totalBalance: Flow<Double> = combine(bankBalance, cashBalance) { b, c -> b + c }

    val savingsTotal: Flow<Double> = combine(savings, transactions) { s, t ->
        WalletEngine.savingsTotal(s.opening, t)
    }

    val goalsWithSaved: Flow<List<GoalWithSaved>> = combine(goals, transactions) { gs, txs ->
        gs.map { g -> GoalWithSaved(g, WalletEngine.goalSaved(g.opening, g.id, txs)) }
    }

    private suspend fun bankSync(): Double = WalletEngine.bankBalance(settingsRepo.openingBankSync(), db.transactionDao().getAll().map { it.toDomain() })

    private suspend fun cashSync(): Double = WalletEngine.cashBalance(settingsRepo.openingCashSync(), db.transactionDao().getAll().map { it.toDomain() })

    // ---------- العمليات المالية ----------

    suspend fun addTransaction(
        type: TxType, amount: Double, category: String, note: String?, wallet: Wallet, goalId: Long? = null,
    ): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        // حماية: التحويلات بين الصناديق تمر عبر المسار المفحوص فقط (لا يمكن تجاوز قاعدة منع السالب)
        if (type == TxType.TRANSFER) return transfer(category, amount, note)
        // إصلاح A2: فئات النظام لا تُنشأ من المسار العام — لكل منها مسار مخصص يفحص السعة
        // (يمنع خلق المال عبر «تكرار» عملية سحب ادخار/هدف/رصيد عميل)
        if (category in CategoryIds.protectedCategories) return WalletError.ProtectedCategory
        if (type == TxType.EXPENSE) {
            checkExpenseSync(amount, wallet)?.let { return it }
        }
        db.transactionDao().insert(
            Transaction(
                id = Ids.next(), type = type, amount = amount, category = category,
                note = note?.ifBlank { null }, date = System.currentTimeMillis(),
                goalId = goalId, wallet = wallet,
            ).toEntity(),
        )
        return null
    }

    suspend fun updateTransaction(updated: Transaction): WalletError? {
        if (updated.amount <= 0 || updated.amount.isNaN()) return WalletError.InvalidAmount
        val old = db.transactionDao().getById(updated.id)?.toDomain() ?: return WalletError.InvalidAmount
        // إصلاح B1: عمليات فئات النظام لا تُعدَّل من النافذة العامة (لا تُحوَّل لفئة عادية والعكس)
        // تحويل «إضافة لهدف» إلى «طعام» مثلاً يُسكت المنطق المشتق ويُفسد حساب المدخر
        if (updated.category in CategoryIds.protectedCategories || old.category in CategoryIds.protectedCategories) {
            return WalletError.ProtectedCategory
        }
        // التراجع عن أثر العملية القديمة ثم التحقق من الجديدة
        val withoutOld = db.transactionDao().getAll().map { it.toDomain() }.filter { it.id != updated.id }
        if (updated.type == TxType.EXPENSE) {
            val bank = WalletEngine.bankBalance(settingsRepo.openingBankSync(), withoutOld)
            val cash = WalletEngine.cashBalance(settingsRepo.openingCashSync(), withoutOld)
            WalletEngine.checkExpense(updated.amount, updated.wallet, bank, cash)?.let { return it }
        }
        if (updated.category == CategoryIds.SAVINGS_WITHDRAW || updated.category == CategoryIds.GOAL_WITHDRAW) {
            val have = if (updated.category == CategoryIds.SAVINGS_WITHDRAW) {
                WalletEngine.savingsTotal(db.savingsDao().get()?.opening ?: 0.0, withoutOld)
            } else {
                WalletEngine.goalSaved(
                    db.goalDao().getAll().firstOrNull { it.id == updated.goalId }?.opening ?: 0.0,
                    updated.goalId ?: -1, withoutOld,
                )
            }
            WalletEngine.checkWithdraw(updated.amount, have)?.let { return it }
        }
        db.transactionDao().update(updated.toEntity())
        return null
    }

    /**
     * إصلاح A3: حذف عملية يعني اختفاء أثرها المالي كله — يجب ألا يكسر أي ثابت:
     * ١) بنك/كاش لا يصبحان سالبين (حذف دخل أو تحويل بعد صرفه)
     * ٢) الادخار لا يصبح سالباً (حذف «إضافة ادخار» بعد سحبه)
     * ٣) مدخر أي هدف لا يصبح سالباً (حذف «إضافة لهدف» بعد سحبه)
     * هامش 0.000001 لامتصاص أخطاء الفاصلة العائمة في المبالغ العشرية.
     */
    suspend fun deleteTransaction(tx: Transaction): WalletError? {
        val without = db.transactionDao().getAll().map { it.toDomain() }.filterNot { it.id == tx.id }
        val bank = WalletEngine.bankBalance(settingsRepo.openingBankSync(), without)
        val cash = WalletEngine.cashBalance(settingsRepo.openingCashSync(), without)
        if (bank < -0.000001 || cash < -0.000001) return WalletError.UnsafeDelete
        val savingsOpening = db.savingsDao().get()?.opening ?: 0.0
        if (WalletEngine.savingsTotal(savingsOpening, without) < -0.000001) return WalletError.UnsafeDelete
        val goals = db.goalDao().getAll().map { it.toDomain() }
        if (goals.any { g -> WalletEngine.goalSaved(g.opening, g.id, without) < -0.000001 }) return WalletError.UnsafeDelete
        db.transactionDao().delete(tx.toEntity())
        return null
    }

    suspend fun transfer(direction: String, amount: Double, note: String?): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        WalletEngine.checkTransfer(direction, amount, bankSync(), cashSync())?.let { return it }
        db.transactionDao().insert(
            Transaction(
                id = Ids.next(), type = TxType.TRANSFER, amount = amount, category = direction,
                note = note?.ifBlank { null }, date = System.currentTimeMillis(),
                wallet = if (direction == CategoryIds.BANK_TO_CASH) Wallet.BANK else Wallet.CASH,
            ).toEntity(),
        )
        return null
    }

    // ---------- الأهداف ----------

    suspend fun addGoal(name: String, target: Double, opening: Double, icon: String) {
        db.goalDao().insert(Goal(id = Ids.next(), name = name, target = target, icon = icon, opening = opening).toEntity())
    }

    suspend fun contributeGoal(goalId: Long, goalName: String, mode: Boolean, amount: Double): WalletError? {
        // mode: true = إضافة، false = سحب
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        if (mode) {
            checkExpenseSync(amount, Wallet.BANK)?.let { return it }
            db.transactionDao().insert(
                Transaction(
                    id = Ids.next(), type = TxType.EXPENSE, amount = amount,
                    category = CategoryIds.GOAL_ADD, note = "إضافة لهدف: $goalName (من البنك)",
                    date = System.currentTimeMillis(), goalId = goalId, wallet = Wallet.BANK,
                ).toEntity(),
            )
        } else {
            val saved = WalletEngine.goalSaved(
                db.goalDao().getAll().firstOrNull { it.id == goalId }?.opening ?: 0.0,
                goalId, db.transactionDao().getAll().map { it.toDomain() },
            )
            WalletEngine.checkWithdraw(amount, saved)?.let { return it }
            db.transactionDao().insert(
                Transaction(
                    id = Ids.next(), type = TxType.INCOME, amount = amount,
                    category = CategoryIds.GOAL_WITHDRAW, note = "سحب من هدف: $goalName (إلى البنك)",
                    date = System.currentTimeMillis(), goalId = goalId, wallet = Wallet.BANK,
                ).toEntity(),
            )
        }
        return null
    }

    suspend fun deleteGoal(goal: Goal) = db.goalDao().delete(goal.toEntity())

    // ---------- الادخار ----------

    suspend fun setSavingsGoal(value: Double) {
        val current = db.savingsDao().get() ?: SavingsEntity(id = 1, opening = 0.0, goal = value)
        db.savingsDao().upsert(current.copy(goal = value))
        settingsRepo.set("savings_goal", value.toString())
    }

    suspend fun addToSavings(amount: Double): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        checkExpenseSync(amount, Wallet.BANK)?.let { return it }
        db.transactionDao().insert(
            Transaction(
                id = Ids.next(), type = TxType.EXPENSE, amount = amount,
                category = CategoryIds.SAVINGS_ADD, note = "إضافة للادخار - حصالة المستقبل (من البنك)",
                date = System.currentTimeMillis(), wallet = Wallet.BANK,
            ).toEntity(),
        )
        return null
    }

    suspend fun withdrawSavings(amount: Double): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        val total = WalletEngine.savingsTotal(
            db.savingsDao().get()?.opening ?: 0.0, db.transactionDao().getAll().map { it.toDomain() },
        )
        if (total <= 0) return WalletError.EmptySavings
        WalletEngine.checkWithdraw(amount, total)?.let { return it }
        db.transactionDao().insert(
            Transaction(
                id = Ids.next(), type = TxType.INCOME, amount = amount,
                category = CategoryIds.SAVINGS_WITHDRAW, note = "سحب من الادخار - حصالة المستقبل (إلى البنك)",
                date = System.currentTimeMillis(), wallet = Wallet.BANK,
            ).toEntity(),
        )
        return null
    }

    /**
     * فحص كفاية الرصيد (نسخة متزامنة) — عام عمداً:
     * يستدعيه مستودع العملاء داخل معاملاته الذرّية (إصلاح A4) حتى يجري الفحص والكتابة في معاملة واحدة.
     */
    suspend fun checkExpenseSync(amount: Double, wallet: Wallet): WalletError? =
        WalletEngine.checkExpense(amount, wallet, bankSync(), cashSync())

    /**
     * إدراج عملية نظامية (شحن/سحب رصيد حقيقي لعميل) دون حراس المسار العام —
     * للاستخدام الحصري داخل معاملة ذرّية في ClientsRepository حيث تُفحص القيود ضمنياً.
     * إصلاح A4: بهذا تبقى قرارات المال كلها داخل معاملة واحدة لا تقبل التشقق.
     */
    suspend fun insertSystemTransaction(tx: Transaction) {
        db.transactionDao().insert(tx.toEntity())
    }

    // ---------- إدارة البيانات ----------

    suspend fun clearAllData() {
        db.withTransaction {
            db.transactionDao().deleteAll()
            db.goalDao().deleteAll()
            db.clientDao().deleteAll()
            db.accountDao().deleteAll()
            db.operationDao().deleteAll()
            db.transferDao().deleteAll()
            db.settingsDao().deleteAll()
            db.savingsDao().upsert(SavingsEntity(id = 1, opening = 0.0, goal = 50_000.0))
        }
    }

    suspend fun exportBackupJson(): String {
        val txs = db.transactionDao().getAll().map { it.toDomain() }
        val goals = db.goalDao().getAll().map { it.toDomain() }
        val savings = db.savingsDao().get()
        val clients = db.clientDao().getAll()
        val accounts = db.accountDao().getAll()
        val ops = db.operationDao().getAll()
        val transfers = db.transferDao().getAll()
        val settingsMap = db.settingsDao().getAll().associate { it.key to it.value }

        val root = JSONObject()
        root.put("app", "Mahfazty Smart")
        root.put("backupVersion", 1)
        root.put("createdAt", Date().toString())
        val data = JSONObject()
        data.put("transactions", JSONArray().apply { txs.forEach { put(txToJson(it)) } })
        data.put("goals", JSONArray().apply {
            goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id); put("name", g.name); put("target", g.target)
                    put("icon", g.icon); put("opening", g.opening)
                })
            }
        })
        data.put("savings", JSONObject().apply {
            put("opening", savings?.opening ?: 0.0); put("goal", savings?.goal ?: 50_000.0)
        })
        data.put("settings", JSONObject(settingsMap))
        data.put("clients", JSONArray().apply {
            clients.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("phone", c.phone ?: JSONObject.NULL)
                    put("photoPath", c.photoPath ?: JSONObject.NULL)
                })
            }
        })
        data.put("accounts", JSONArray().apply {
            accounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("clientId", a.clientId); put("name", a.name)
                    put("icon", a.icon); put("realBalance", a.realBalance)
                })
            }
        })
        data.put("operations", JSONArray().apply {
            ops.forEach { o ->
                put(JSONObject().apply {
                    put("id", o.id); put("accountId", o.accountId); put("type", o.type)
                    put("amount", o.amount); put("note", o.note ?: JSONObject.NULL)
                    put("date", o.date)
                    put("materials", o.materialsJson ?: JSONObject.NULL)
                    put("receiptPath", o.receiptPath ?: JSONObject.NULL)
                    // نظام «الفواتير وحالة التسليم» — تُحفظ مع النسخة الاحتياطية
                    put("isInvoice", o.isInvoice)
                    put("invoiceRef", o.invoiceRef ?: JSONObject.NULL)
                    put("invoiceDelivered", o.invoiceDelivered)
                })
            }
        })
        data.put("transfers", JSONArray().apply {
            transfers.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("fromClientId", t.fromClientId)
                    put("fromAccountId", t.fromAccountId); put("toClientId", t.toClientId)
                    put("toAccountId", t.toAccountId); put("amount", t.amount); put("date", t.date)
                })
            }
        })
        root.put("data", data)
        return root.toString(2)
    }

    private fun txToJson(t: Transaction): JSONObject = JSONObject().apply {
        put("id", t.id); put("type", t.type.name); put("amount", t.amount)
        put("category", t.category); put("note", t.note ?: JSONObject.NULL)
        put("date", t.date); put("goalId", t.goalId ?: JSONObject.NULL); put("wallet", t.wallet.name)
    }

    data class ImportResult(val ok: Boolean, val message: String)

    suspend fun importBackup(text: String): ImportResult = runCatching {
        val root = JSONObject(text)
        val data = root.optJSONObject("data") ?: return@runCatching ImportResult(false, "الملف لا يحتوي على بيانات صالحة")
        var txCount = 0
        var goalCount = 0
        var clientCount = 0
        var accountCount = 0
        var opCount = 0
        var transferCount = 0
        db.withTransaction {
            // 1) الأهداف أولاً — نبني خريطة إعادة الترميز (معرف قديم ← جديد) لربط العمليات بها
            val goalIdMap = mutableMapOf<Long, Long>()
            data.optJSONArray("goals")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val newId = Ids.next()
                    val oldId = o.optLong("id", -1L)
                    if (oldId > 0) goalIdMap[oldId] = newId
                    db.goalDao().insert(GoalEntity(
                        id = newId, name = o.getString("name"), target = o.getDouble("target"),
                        icon = o.optString("icon", "💻"), opening = o.optDouble("opening"),
                    ))
                    goalCount++
                }
            }
            // 2) العمليات — مع إعادة ربط كل عملية بهدفها الجديد
            data.optJSONArray("transactions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val oldGoal = if (o.isNull("goalId")) null else o.optLong("goalId")
                    val newGoal = oldGoal?.let { goalIdMap[it] }
                    db.transactionDao().insert(TransactionEntity(
                        id = Ids.next(),
                        type = runCatching { TxType.valueOf(o.getString("type")) }.getOrDefault(TxType.EXPENSE).name,
                        amount = o.getDouble("amount"),
                        category = o.getString("category"),
                        note = if (o.isNull("note")) null else o.getString("note"),
                        date = o.getLong("date"),
                        goalId = newGoal,
                        wallet = runCatching { Wallet.valueOf(o.getString("wallet")) }.getOrDefault(Wallet.CASH).name,
                    ))
                    txCount++
                }
            }
            data.optJSONObject("savings")?.let { s ->
                db.savingsDao().upsert(SavingsEntity(1, s.optDouble("opening"), s.optDouble("goal", 50_000.0)))
            }
            // 3) الإعدادات — استيراد كل المفاتيح (الحدود، الإخفاء، الأرصدة الافتتاحية...)
            data.optJSONObject("settings")?.let { s ->
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (!s.isNull(k)) {
                        val v = s.get(k).toString()
                        if (v.isNotBlank()) db.settingsDao().upsert(SettingEntity(k, v))
                    }
                }
            }
            // 4) العملاء — مع خريطة إعادة الترميز
            val clientIdMap = mutableMapOf<Long, Long>()
            data.optJSONArray("clients")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val newId = Ids.next()
                    val oldId = o.optLong("id", -1L)
                    if (oldId > 0) clientIdMap[oldId] = newId
                    db.clientDao().insert(ClientEntity(
                        id = newId, name = o.getString("name"),
                        phone = if (o.isNull("phone")) null else o.getString("phone"),
                        photoPath = if (o.isNull("photoPath")) null else o.getString("photoPath"),
                    ))
                    clientCount++
                }
            }
            // 5) حسابات العملاء — إعادة ربطها بالعميل الجديد
            val accountIdMap = mutableMapOf<Long, Long>()
            data.optJSONArray("accounts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val newId = Ids.next()
                    val oldId = o.optLong("id", -1L)
                    if (oldId > 0) accountIdMap[oldId] = newId
                    val oldClient = o.optLong("clientId", -1L)
                    val mappedClient = clientIdMap[oldClient]
                    if (mappedClient == null) continue   // حساب بلا عميل صالح — يتجاوزه بأمان
                    db.accountDao().insert(AccountEntity(
                        id = newId,
                        clientId = mappedClient,
                        name = o.getString("name"),
                        icon = o.optString("icon", "💰"),
                        realBalance = o.optDouble("realBalance"),
                    ))
                    accountCount++
                }
            }
            // 6) عمليات الحسابات (دين/سداد) — إعادة ربطها بالحساب الجديد
            data.optJSONArray("operations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val oldAccount = o.optLong("accountId", -1L)
                    val mappedAccount = accountIdMap[oldAccount]
                    if (mappedAccount == null) continue   // عملية بلا حساب صالح — يتجاوزها بأمان
                    db.operationDao().insert(OperationEntity(
                        id = Ids.next(),
                        accountId = mappedAccount,
                        type = runCatching { OpType.valueOf(o.getString("type")) }.getOrDefault(OpType.DEBT).name,
                        amount = o.getDouble("amount"),
                        note = if (o.isNull("note")) null else o.getString("note"),
                        date = o.getLong("date"),
                        materialsJson = if (o.isNull("materials")) null else o.getString("materials"),
                        receiptPath = if (o.isNull("receiptPath")) null else o.getString("receiptPath"),
                        // النسخ القديمة بلا هذه المفاتيح → القيم الافتراضية false (عملية عادية)
                        isInvoice = o.optBoolean("isInvoice", false),
                        invoiceRef = if (o.isNull("invoiceRef")) null else o.optString("invoiceRef", "").ifBlank { null },
                        invoiceDelivered = o.optBoolean("invoiceDelivered", false),
                    ))
                    opCount++
                }
            }
            // 7) سجل التحويلات — إعادة ربطه بكل الأطراف الجديدة
            data.optJSONArray("transfers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val fromAcc = accountIdMap[o.optLong("fromAccountId", -1L)]
                    val toAcc = accountIdMap[o.optLong("toAccountId", -1L)]
                    val fromClient = clientIdMap[o.optLong("fromClientId", -1L)]
                    val toClient = clientIdMap[o.optLong("toClientId", -1L)]
                    if (fromAcc == null || toAcc == null || fromClient == null || toClient == null) continue
                    db.transferDao().insert(TransferEntity(
                        id = Ids.next(),
                        fromClientId = fromClient, fromAccountId = fromAcc,
                        toClientId = toClient, toAccountId = toAcc,
                        amount = o.getDouble("amount"), date = o.getLong("date"),
                    ))
                    transferCount++
                }
            }
        }
        ImportResult(
            true,
            "تم الاستيراد: $txCount عملية • $goalCount هدف • $clientCount عميل • $accountCount حساب • $opCount عملية عميل • $transferCount تحويل",
        )
    }.getOrElse { e -> ImportResult(false, "تعذر قراءة الملف: ${e.message}") }

    suspend fun exportCsv(): String {
        val txs = db.transactionDao().getAll().map { it.toDomain() }
        val sb = StringBuilder("\uFEFF")
        sb.append("التاريخ,النوع,الفئة,المبلغ,الصندوق,الملاحظة\n")
        txs.sortedBy { it.date }.forEach { t ->
            val type = when (t.type) {
                TxType.INCOME -> "دخل"
                TxType.EXPENSE -> "مصروف"
                TxType.TRANSFER -> "تحويل"
            }
            val cat = com.mahfazty.smart.domain.categoryName(t.category)
            val wallet = if (t.wallet == Wallet.BANK) "بنك" else "كاش"
            val note = t.note.orEmpty().replace(",", "،")
            sb.append("${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(t.date))},$type,$cat,${com.mahfazty.smart.domain.Money.fmtLat(t.amount)},$wallet,$note\n")
        }
        return sb.toString()
    }
}

// =====================================================================
// 3) مستودع العملاء — ملفات العملاء والحسابات والعمليات والأرصدة الحقيقية
// =====================================================================

/** حساب مع عملياته */
data class AccountWithOps(val account: ClientAccount, val operations: List<ClientOperation>) {
    val opsBalance: Double get() = WalletEngine.opsBalance(operations)
}

/** عميل مع حساباته وعملياته */
data class ClientWithData(val client: Client, val accounts: List<AccountWithOps>) {
    val total: Double get() = accounts.sumOf { it.opsBalance }
}

/** عرض تحويل رصيد حقيقي بأسماء مقروءة */
data class TransferDisplay(
    val transfer: RealTransfer,
    val fromClientName: String,
    val fromAccountName: String,
    val toClientName: String,
    val toAccountName: String,
)

class ClientsRepository(
    private val db: AppDatabase,
    private val walletRepo: WalletRepository,
) {
    val clientsWithData: Flow<List<ClientWithData>> = combine(
        db.clientDao().observeAll(),
        db.accountDao().observeAll(),
        db.operationDao().observeAll(),
    ) { clients, accounts, ops ->
        val opsByAccount = ops.groupBy { it.accountId }
        clients.map { c ->
            ClientWithData(
                client = c.toDomain(),
                accounts = accounts.filter { it.clientId == c.id }.map { a ->
                    AccountWithOps(
                        account = a.toDomain(),
                        operations = (opsByAccount[a.id] ?: emptyList()).map { it.toDomain() },
                    )
                },
            )
        }
    }

    fun clientWithData(clientId: Long): Flow<ClientWithData?> =
        clientsWithData.map { list -> list.firstOrNull { it.client.id == clientId } }

    fun accountWithOps(accountId: Long): Flow<AccountWithOps?> = combine(
        db.accountDao().observeAll(), db.operationDao().observeAll(),
    ) { accounts, ops ->
        accounts.firstOrNull { it.id == accountId }?.let { a ->
            AccountWithOps(a.toDomain(), ops.filter { it.accountId == accountId }.map { it.toDomain() })
        }
    }

    /** كل الحسابات (لمنتقي مصدر التحويل) */
    val allAccounts: Flow<List<Pair<Client, ClientAccount>>> = combine(
        db.clientDao().observeAll(), db.accountDao().observeAll(),
    ) { clients, accounts ->
        accounts.mapNotNull { a ->
            clients.firstOrNull { it.id == a.clientId }?.let { c -> c.toDomain() to a.toDomain() }
        }
    }

    /** سجل التحويلات بأسماء مقروءة */
    val transfersDisplay: Flow<List<TransferDisplay>> = combine(
        db.transferDao().observeAll(),
        db.clientDao().observeAll(),
        db.accountDao().observeAll(),
    ) { transfers, clients, accounts ->
        transfers.map { t ->
            val cFrom = clients.firstOrNull { it.id == t.fromClientId }?.name ?: "عميل محذوف"
            val cTo = clients.firstOrNull { it.id == t.toClientId }?.name ?: "عميل محذوف"
            val aFrom = accounts.firstOrNull { it.id == t.fromAccountId }?.name ?: "حساب محذوف"
            val aTo = accounts.firstOrNull { it.id == t.toAccountId }?.name ?: "حساب محذوف"
            TransferDisplay(t.toDomain(), cFrom, aFrom, cTo, aTo)
        }
    }

    // ---------- العملاء ----------

    suspend fun addClient(name: String, phone: String?, photoPath: String?): Long {
        val id = Ids.next()
        db.clientDao().insert(ClientEntity(id, name, phone?.ifBlank { null }, photoPath))
        return id
    }

    suspend fun updateClient(client: Client) =
        db.clientDao().insert(ClientEntity(client.id, client.name, client.phone, client.photoPath))

    suspend fun deleteClient(clientId: Long) = db.withTransaction {
        val client = db.clientDao().getAll().firstOrNull { it.id == clientId } ?: return@withTransaction
        db.operationDao().getAll().filter { op ->
            db.accountDao().getAll().any { it.id == op.accountId && it.clientId == clientId }
        }.forEach { db.operationDao().delete(it) }
        db.accountDao().deleteByClient(clientId)
        db.clientDao().delete(client)
    }

    // ---------- الحسابات ----------

    suspend fun addAccount(clientId: Long, name: String, icon: String): Long {
        val id = Ids.next()
        db.accountDao().insert(AccountEntity(id, clientId, name, icon, 0.0))
        return id
    }

    suspend fun updateAccount(account: ClientAccount) = db.accountDao().update(
        AccountEntity(account.id, account.clientId, account.name, account.icon, account.realBalance),
    )

    suspend fun deleteAccount(accountId: Long) = db.withTransaction {
        db.operationDao().deleteByAccount(accountId)
        db.accountDao().deleteById(accountId)
    }

    // ---------- العمليات (دين/سداد) ----------

    suspend fun addOperation(
        accountId: Long, type: OpType, amount: Double, note: String?,
        materials: List<MaterialItem>, receiptPath: String?,
        isInvoice: Boolean = false, invoiceRef: String? = null,
    ): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        // حقول الفاتورة وصفية بحتة — تظهر في الإدخال ولا تدخل في الفحص المالي أبداً
        // (إجبارية الحقل تتحقق في واجهة الإدخال برسالة واضحة للمستخدم)
        val ref = if (isInvoice) invoiceRef?.ifBlank { null } else null
        return db.withTransaction {
            val acc = db.accountDao().getById(accountId)
                ?: return@withTransaction WalletError.InvalidAmount
            if (type == OpType.DEBT) {
                WalletEngine.checkDebtAgainstReal(acc.realBalance, amount)?.let { return@withTransaction it }
            }
            db.accountDao().update(
                acc.copy(realBalance = WalletEngine.applyOpToReal(acc.realBalance, type, amount)),
            )
            db.operationDao().insert(
                ClientOperation(
                    id = Ids.next(), accountId = accountId, type = type, amount = amount,
                    note = note?.ifBlank { null }, date = System.currentTimeMillis(),
                    materials = materials, receiptPath = receiptPath,
                    isInvoice = isInvoice, invoiceRef = ref,
                ).toEntity(),
            )
            null
        }
    }

    suspend fun updateOperation(updated: ClientOperation): WalletError? {
        if (updated.amount <= 0 || updated.amount.isNaN()) return WalletError.InvalidAmount
        return db.withTransaction {
            val old = db.operationDao().getAll().firstOrNull { it.id == updated.id }
                ?: return@withTransaction WalletError.InvalidAmount
            val acc = db.accountDao().getById(updated.accountId)
                ?: return@withTransaction WalletError.InvalidAmount
            val oldType = runCatching { OpType.valueOf(old.type) }.getOrDefault(OpType.DEBT)
            val balBefore = WalletEngine.reverseOpFromReal(acc.realBalance, oldType, old.amount)
            if (updated.type == OpType.DEBT) {
                WalletEngine.checkDebtAgainstReal(balBefore, updated.amount)?.let { return@withTransaction it }
            }
            db.accountDao().update(
                acc.copy(realBalance = WalletEngine.applyOpToReal(balBefore, updated.type, updated.amount)),
            )
            db.operationDao().insert(updated.toEntity())
            null
        }
    }

    suspend fun deleteOperation(op: ClientOperation) = db.withTransaction {
        val acc = db.accountDao().getById(op.accountId)
        if (acc != null) {
            db.accountDao().update(
                acc.copy(realBalance = WalletEngine.reverseOpFromReal(acc.realBalance, op.type, op.amount)),
            )
        }
        db.operationDao().delete(op.toEntity())
    }

    /** إصلاح B4: حذف جماعي ذرّي — كل العمليات في معاملة واحدة، فلا حذف جزئي عند انقطاع منتصف الطريق */
    suspend fun deleteOperations(ops: List<ClientOperation>) = db.withTransaction {
        ops.forEach { op ->
            val acc = db.accountDao().getById(op.accountId)
            if (acc != null) {
                db.accountDao().update(
                    acc.copy(realBalance = WalletEngine.reverseOpFromReal(acc.realBalance, op.type, op.amount)),
                )
            }
            db.operationDao().delete(op.toEntity())
        }
    }

    /** تسليم فاتورة: يقلب invoiceDelivered فقط — صفر تأثير على أي رصيد (حقل وصفي بحت) */
    suspend fun markInvoiceDelivered(opId: Long): Boolean = db.withTransaction {
        val entity = db.operationDao().getAll().firstOrNull { it.id == opId }
            ?: return@withTransaction false
        db.operationDao().insert(entity.copy(invoiceDelivered = true))
        true
    }

    // ---------- الرصيد الحقيقي: شحن/سحب/تحويل ----------
    // إصلاح A4: العمليات الثلاث أصبحت ذرّية بالكامل — الفحص والخصم والإدراج
    // في معاملة واحدة على نفس قاعدة البيانات، فلا سباق بين كوروتينين متوازيين
    // ولا «نجاح صامت» يُنشئ مالاً أو يُفقده إذا تغير الرصيد في منتصف الطريق.

    suspend fun fundReal(accountId: Long, amount: Double, from: Wallet): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        return db.withTransaction {
            // فحص كفاية الصندوق داخل المعاملة نفسها (لا فجوة زمنية بعده)
            walletRepo.checkExpenseSync(amount, from)?.let { return@withTransaction it }
            val acc = db.accountDao().getById(accountId) ?: return@withTransaction WalletError.InvalidAmount
            walletRepo.insertSystemTransaction(
                Transaction(
                    id = Ids.next(), type = TxType.EXPENSE, amount = amount,
                    category = CategoryIds.CLIENT_FUND,
                    note = "شحن رصيد حقيقي من ${if (from == Wallet.BANK) "البنك" else "الكاش"}",
                    date = System.currentTimeMillis(), wallet = from,
                ),
            )
            db.accountDao().update(acc.copy(realBalance = acc.realBalance + amount))
            null
        }
    }

    suspend fun withdrawReal(accountId: Long, amount: Double, to: Wallet): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        return db.withTransaction {
            val acc = db.accountDao().getById(accountId) ?: return@withTransaction WalletError.InvalidAmount
            // الفحص والخصم في نفس المعاملة — لا مسار يُنجح السحب دون خصم فعلي
            if (acc.realBalance - amount < 0) {
                return@withTransaction WalletError.InsufficientReal(acc.realBalance, amount)
            }
            walletRepo.insertSystemTransaction(
                Transaction(
                    id = Ids.next(), type = TxType.INCOME, amount = amount,
                    category = CategoryIds.CLIENT_WITHDRAW,
                    note = "سحب من رصيد حقيقي إلى ${if (to == Wallet.BANK) "البنك" else "الكاش"}",
                    date = System.currentTimeMillis(), wallet = to,
                ),
            )
            db.accountDao().update(acc.copy(realBalance = acc.realBalance - amount))
            null
        }
    }

    suspend fun transferReal(
        fromClientId: Long, fromAccountId: Long, toClientId: Long, toAccountId: Long, amount: Double,
    ): WalletError? {
        if (amount <= 0 || amount.isNaN()) return WalletError.InvalidAmount
        // منع تحويل الحساب إلى نفسه — كان يُنشئ سجل تحويل بلا أثر مالي
        if (fromAccountId == toAccountId) return WalletError.InvalidAmount
        return db.withTransaction {
            // القراءة والفحص والخصم للطرفين داخل معاملة واحدة — يمنع فقدان التحديث (lost update)
            val from = db.accountDao().getById(fromAccountId) ?: return@withTransaction WalletError.InvalidAmount
            val to = db.accountDao().getById(toAccountId) ?: return@withTransaction WalletError.InvalidAmount
            if (from.realBalance - amount < 0) {
                return@withTransaction WalletError.InsufficientReal(from.realBalance, amount)
            }
            db.accountDao().update(from.copy(realBalance = from.realBalance - amount))
            db.accountDao().update(to.copy(realBalance = to.realBalance + amount))
            db.transferDao().insert(
                TransferEntity(
                    id = Ids.next(), fromClientId = fromClientId, fromAccountId = fromAccountId,
                    toClientId = toClientId, toAccountId = toAccountId,
                    amount = amount, date = System.currentTimeMillis(),
                ),
            )
            null
        }
    }

    /** نص مشاركة واتساب لكشف حساب */
    fun whatsAppText(client: Client, acc: AccountWithOps): String {
        val bal = acc.opsBalance
        val real = acc.account.realBalance
        val sb = StringBuilder()
        sb.append("🧾 كشف حساب: ${acc.account.name} ${acc.account.icon}\n")
        sb.append("👤 العميل: ${client.name}\n")
        sb.append("💰 الرصيد الحقيقي: ${com.mahfazty.smart.domain.Money.fmt(real)}\n")
        if (bal > 0) sb.append("🔴 عليه: ${com.mahfazty.smart.domain.Money.fmt(bal)}\n")
        else if (bal < 0) sb.append("🟢 له: ${com.mahfazty.smart.domain.Money.fmt(-bal)}\n")
        sb.append("\n📋 العمليات:\n")
        acc.operations.sortedByDescending { it.date }.take(10).forEachIndexed { i, op ->
            val label = if (op.type == OpType.DEBT) "عليه" else "له"
            sb.append("${i + 1}. $label ${com.mahfazty.smart.domain.Money.fmt(op.amount)} - ${op.note ?: ""}")
            if (op.materials.isNotEmpty()) sb.append(" [${op.materials.joinToString(", ") { it.name }}]")
            sb.append("\n")
        }
        sb.append("\nمحفظتي الذكية 💰")
        return sb.toString()
    }
}
