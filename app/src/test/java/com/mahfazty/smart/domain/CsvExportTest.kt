package com.mahfazty.smart.domain

import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.Category
import com.mahfazty.smart.domain.model.CategoryKind
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبار عزل وإصلاح «الجدول ينشاء خطأ بالكامل» في تصدير CSV (v2.8.3).
 *
 * المنهج المطلوب من المستخدم: عزل الأكواد → اختبارها → إيجاد المشكلة بالضبط →
 * الإصلاح → مراجعة نقدية → اختبار حقيقي. هذا الملف يفعل ذلك بالترتيب:
 *
 * 1) محاكاة الكود القديم **حرفيًا** (تركيب نصي بلا اقتباس + Money.fmtLat بفواصل
 *    التجميع) على بيانات المستخدم الحقيقية (474 عملية من مورد الاختبار) —
 *    ويثبت العطل بعدّ الصفوف المشطوبة: كل مبلغ ≥ 1000 يصير عمودين زيدين،
 *    وملاحظة تحوي \n تكسر الصف وتزيح كل ما بعده.
 * 2) المنطق الجديد عبر وحدات الإصلاح الحقيقية (Csv + Money.csvAmount +
 *    categoryName مع settings) على نفس البيانات — ويثبت سلامة الجدول كليًا:
 *    كل صف 6 خلايا، والنصوص تعود حرفيًا كما دخلت، والمبالغ بلا فواصل.
 * 3) اختبار كشف حساب العميل (عادي/فاتورة/مختلط) بصفوف Csv — تكامل حرفي.
 * 4) حالات حدّية لمحرك RFC 4180 نفسه.
 *
 * المحلل هنا محلل RFC 4180 مستقل مكتوب للقراءة الصارمة — لا يعتمد على المولد.
 */
class CsvExportTest {

    // ============ نماذج مبسطة + قراءة البيانات الحقيقية ============

    private data class TxRow(
        val date: Long,
        val type: TxType,
        val amount: Double,
        val category: String,
        val note: String?,
        val wallet: Wallet,
    )

    /** 474 عملية حقيقية من ملف النسخة الاحتياطية الفعلي (نفس بيانات المستخدم) */
    private fun realTxs(): List<TxRow> {
        val root = JSONObject(
            javaClass.classLoader!!.getResourceAsStream("backup/real-backup-2026-11-17.json")!!
                .readBytes().toString(Charsets.UTF_8)
        )
        val arr = root.getJSONObject("data").getJSONArray("transactions")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TxRow(
                date = o.getLong("date"),
                type = TxType.valueOf(o.getString("type")),
                amount = o.getDouble("amount"),
                category = o.getString("category"),
                note = if (o.isNull("note")) null else o.getString("note"),
                wallet = Wallet.valueOf(o.getString("wallet")),
            )
        }
    }

    /** الملاحظة الحقيقية التي فيها سطر جديد (ثابتة في ملف المستخدم) */
    private val realNewlineNoteTx: TxRow
        get() = realTxs().first { (it.note ?: "").contains("\n") }

    // ============ محاكاة الكود القديم — حرفيًا كما كان قبل الإصلاح ============

    /** fmtLat القديم: NumberFormat(Locale.US) الافتراضي — فواصل التجميع مفعّلة */
    private fun oldFmtLat(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "0"
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
        fmt.maximumFractionDigits = 0
        return fmt.format(kotlin.math.round(n))
    }

    /** exportCsv القديم بالحرف: تركيب نصي مباشر، فاصلة→«،»، بلا اقتباس */
    private fun oldExportCsv(txs: List<TxRow>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append("التاريخ,النوع,الفئة,المبلغ,الصندوق,الملاحظة\n")
        txs.sortedBy { it.date }.forEach { t ->
            val type = when (t.type) {
                TxType.INCOME -> "دخل"
                TxType.EXPENSE -> "مصروف"
                TxType.TRANSFER -> "تحويل"
            }
            val cat = categoryName(t.category)
            val wallet = if (t.wallet == Wallet.BANK) "بنك" else "كاش"
            val note = t.note.orEmpty().replace(",", "،")
            sb.append("${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(t.date))},$type,$cat,${oldFmtLat(t.amount)},$wallet,$note\n")
        }
        return sb.toString()
    }

    // ============ المحلل الصارم المستقل (RFC 4180) ============

    private class Rfc4180Parser {
        fun parse(text: String): List<List<String>> {
            val s = text.removePrefix("\uFEFF")
            val rows = mutableListOf<List<String>>()
            var cell = StringBuilder()
            var row = mutableListOf<String>()
            var inQuotes = false
            var i = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    inQuotes -> when {
                        c == '"' -> {
                            if (i + 1 < s.length && s[i + 1] == '"') { cell.append('"'); i++ }
                            else inQuotes = false
                        }
                        else -> cell.append(c)
                    }
                    c == '"' && cell.isEmpty() -> inQuotes = true
                    c == ',' -> { row += cell.toString(); cell = StringBuilder() }
                    c == '\n' -> { row += cell.toString(); cell = StringBuilder(); rows += row; row = mutableListOf() }
                    else -> cell.append(c)
                }
                i++
            }
            if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
            return rows
        }
    }

    private fun parseCsv(text: String) = Rfc4180Parser().parse(text)

    // ============ 1) إثبات المشكلة بالضبط على بيانات المستخدم الحقيقية ============

    @Test
    fun `الكود القديم على البيانات الحقيقية يشطر كل صف مبلغه 1000 فأكثر إلى 8 أعمدة`() {
        val txs = realTxs()
        assertEquals("عدد العمليات الحقيقية", 474, txs.size)

        val rows = parseCsv(oldExportCsv(txs))
        // المتوقع سليمًا: 1 رأس + 474 صفًا = 475 صفًا بأعمدة 6
        val wrongCols = rows.count { it.size != 6 }
        val bigAmounts = txs.count { it.amount >= 1000 }

        assertTrue("البيانات الحقيقية فيها مبالغ ≥1000 (194 صفًا)", bigAmounts >= 190)
        assertTrue(
            "إثبات العطل: ${wrongCols} صفًا بأعمدة ≠6 (الجدول كله مشطوب) — المتوقع ≥ ${bigAmounts}",
            wrongCols >= bigAmounts,
        )
        // مثال ملموس: "23,000" انشطرت إلى خليتين "23" و"000"
        val sampleBig = txs.first { it.amount >= 1000 }
        val expectedSplit = oldFmtLat(sampleBig.amount) // مثل "23,000"
        assertTrue(expectedSplit.contains(","))
        val splitCells = expectedSplit.split(",")
        assertTrue(
            "الصف المنشطر يحتوي جزأي المبلغ كخليتين منفصلتين",
            rows.any { it.size > 6 && it.contains(splitCells[0]) && it.contains(splitCells[1]) },
        )
    }

    @Test
    fun `الكود القديم يكسر الجدول كله عند ملاحظة فيها سطر جديد — الملاحظة الحقيقية للمستخدم`() {
        val tx = realNewlineNoteTx
        assertTrue(tx.note!!.contains("\n"))
        val rows = parseCsv(oldExportCsv(listOf(tx)))
        // صف واحد كان يجب أن يكون صفاً واحداً — انكسر إلى سطرين والجزء الثاني عالة
        assertTrue("السطر الجديد أنتج صفوفًا زيادة", rows.size > 2) // رأس + الجزء الأول + الجزء الثاني
        assertTrue("جزء الملاحظة الثاني ظهر كصف مبتور", rows.any { it.size < 6 })
        // المعيار الحقيقي للفقد: الملاحظة لم تعد خلية واحدة سليمة في أي صف —
        // (إعادة تركيب النص الخام قد تعيد مطابقته صدفة، لكن Excel يقرأ خلايا لا نصًا خامًا)
        assertTrue(
            "الملاحظة الحقيقية لم تعد خلية سليمة — تُقرأ منشطرًا",
            rows.none { it.size == 6 && it[5] == tx.note },
        )
    }

    @Test
    fun `الكود القديم يشوه نص الملاحظة — يستبدل الفاصلة اللاتينية بعربية`() {
        val tx = TxRow(0L, TxType.INCOME, 250.0, "salary", "راتب, إضافي", Wallet.CASH)
        val rows = parseCsv(oldExportCsv(listOf(tx)))
        assertFalse("الكود القديم غيّر النص الأصلي", rows[1][5] == "راتب, إضافي")
        assertEquals("استبدله بفاصلة عربية — تشويه بيانات", "راتب، إضافي", rows[1][5])
    }

    // ============ 2) المنطق الجديد على نفس البيانات الحقيقية ============

    /** بناء الصفوف بالمنطق الجديد — نفس تركيبة exportCsv الفعلية عبر وحدات الإصلاح */
    private fun newExportCsv(txs: List<TxRow>): String {
        val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val sb = StringBuilder(Csv.BOM)
        sb.append(Csv.row("التاريخ", "النوع", "الفئة", "المبلغ", "الصندوق", "الملاحظة"))
        txs.sortedBy { it.date }.forEach { t ->
            val type = when (t.type) {
                TxType.INCOME -> "دخل"
                TxType.EXPENSE -> "مصروف"
                TxType.TRANSFER -> "تحويل"
            }
            sb.append(
                Csv.row(
                    dateFmt.format(java.util.Date(t.date)),
                    type,
                    categoryName(t.category),
                    Money.csvAmount(t.amount),
                    if (t.wallet == Wallet.BANK) "بنك" else "كاش",
                    t.note.orEmpty(),
                )
            )
        }
        return sb.toString()
    }

    @Test
    fun `المنطق الجديد على 474 عملية حقيقية — كل صف 6 خلايا بالضبط ولا صف مشطوب`() {
        val txs = realTxs()
        val text = newExportCsv(txs)

        assertTrue("يبدأ بـ BOM ليقرأ Excel العربي صحيحًا", text.startsWith("\uFEFF"))
        val rows = parseCsv(text)
        assertEquals("عدد الصفوف: رأس + 474 عملية", 475, rows.size)
        assertEquals("الرأس", listOf("التاريخ", "النوع", "الفئة", "المبلغ", "الصندوق", "الملاحظة"), rows[0])
        assertEquals("لا يوجد أي صف بأعمدة ≠6 — الجدول سليم بالكامل", 0, rows.count { it.size != 6 })

        // كل صف سجل حقيقي بالترتيب الزمني الصحيح
        val sorted = txs.sortedBy { it.date }
        rows.drop(1).forEachIndexed { i, r ->
            assertEquals("ترتيب زمني صاعد", sorted[i].date / 60_000, parseStamp(r[0]) / 60_000)
            assertEquals("المبلغ بلا فواصل", Money.csvAmount(sorted[i].amount), r[3])
            assertFalse("لا فاصل تجميع في المبلغ إطلاقًا", r[3].contains(","))
        }
    }

    @Test
    fun `المنطق الجديد يحفظ الملاحظة الحقيقية بسطر جديد حرفياً داخل خلية واحدة`() {
        val tx = realNewlineNoteTx
        val rows = parseCsv(newExportCsv(listOf(tx)))
        assertEquals("صف واحد فقط رغم \\n داخل الملاحظة", 2, rows.size)
        assertEquals("الملاحظة تعود حرفيًا كما دخلت", tx.note!!, rows[1][5])
    }

    @Test
    fun `المنطق الجديد يحفظ الفواصل والاقتباسات والمبالغ الكبيرة كما هي`() {
        val tricky = listOf(
            TxRow(1_700_000_000_000L, TxType.INCOME, 23_456_789.4, "salary", "راتب, إضافي", Wallet.BANK),
            TxRow(1_700_000_100_000L, TxType.EXPENSE, 500.0, "other", "قال: \"ادفع بالضبط\"", Wallet.CASH),
            TxRow(1_700_000_200_000L, TxType.TRANSFER, 1_000.0, "bank_to_cash", null, Wallet.BANK),
        )
        val rows = parseCsv(newExportCsv(tricky))
        assertEquals(4, rows.size)
        assertEquals("فاصلة لاتينية تعود كما هي (لا استبدال)", "راتب, إضافي", rows[1][5])
        assertEquals("اقتباس يعود كما هو", "قال: \"ادفع بالضبط\"", rows[2][5])
        assertEquals("ملاحظة null تصير خلية فارغة", "", rows[3][5])
        assertEquals("مبلغ كبير بلا فواصل", "23456789", rows[1][3])
        assertEquals("1000 بلا فاصل", "1000", rows[3][3])
    }

    @Test
    fun `الصيغة الجديدة تُقرأ قراءة Excel نفسها عند وجود فاصلة داخل اقتباس متجاور`() {
        // حالة أعقد: خلية مقتبسة تليها خلية عادية — حدود الأعمدة يجب أن تظل دقيقة
        val csv = Csv.row("قبل", "ملاحظة, مع فاصلة", "بعد")
        assertEquals(listOf("قبل", "ملاحظة, مع فاصلة", "بعد"), parseCsv(csv)[0])
        val csv2 = Csv.row("أ", "\"متداخل\", خطر", "ب")
        assertEquals(listOf("أ", "\"متداخل\", خطر", "ب"), parseCsv(csv2)[0])
    }

    // ============ 3) كشف حساب العميل: عادي / فاتورة / مختلط ============

    private data class OpRow(
        val date: Long,
        val debt: Boolean,
        val amount: Double,
        val currency: String?,
        val note: String?,
        val materials: List<Triple<String, Double, Double>>, // اسم, كمية, سعر
        val isInvoice: Boolean,
        val invoiceRef: String?,
    )

    /** نفس تركيبة exportAccountCsv الجديدة عبر وحدات الإصلاح */
    private fun newStatementCsv(ops: List<OpRow>): String {
        val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val sb = StringBuilder(Csv.BOM)
        sb.append(Csv.row("التاريخ", "النوع", "المبلغ", "العملة", "البيان", "المواد", "الفاتورة"))
        ops.sortedBy { it.date }.forEach { op ->
            val materials = op.materials.joinToString("؛ ") { (name, qty, price) ->
                "$name (${Csv.num(qty)}×${Csv.num(price)})"
            }
            sb.append(
                Csv.row(
                    dateFmt.format(java.util.Date(op.date)),
                    if (op.debt) "عليه" else "له",
                    Money.csvAmount(op.amount),
                    op.currency.orEmpty(),
                    op.note.orEmpty(),
                    materials,
                    if (op.isInvoice) op.invoiceRef.orEmpty() else "",
                )
            )
        }
        return sb.toString()
    }

    @Test
    fun `كشف الحساب المختلط عادي وفواتير يبقى 7 أعمدة والنصوص تكمل حرفياً`() {
        val ops = listOf(
            // عادية
            OpRow(1_700_000_000_000L, true, 45_000.0, "YER", "تمديد ماطور — دفعة أولى", emptyList(), false, null),
            // فاتورة بمواد كسور عشرية ومرجع فيه فاصلة
            OpRow(
                1_700_000_100_000L, false, 12_500.0, null,
                "فاتورة صيانة", listOf(Triple("طابلوت", 2.5, 1_250.0), Triple("سلك, لف", 3.0, 900.0)),
                true, "ف-2026, بند 4",
            ),
            // ملاحظة فيها اقتباس وسطر جديد
            OpRow(1_700_000_200_000L, true, 7_000.0, "USD", "قال: \"غدا\"\nوبعدها", emptyList(), false, null),
        )
        val rows = parseCsv(newStatementCsv(ops))
        assertEquals("رأس + 3 عمليات", 4, rows.size)
        assertEquals(listOf("التاريخ", "النوع", "المبلغ", "العملة", "البيان", "المواد", "الفاتورة"), rows[0])
        assertEquals("لا صف مشطوب", 0, rows.count { it.size != 7 })

        assertEquals("مبلغ 45,000 بلا فاصل", "45000", rows[1][2])
        assertEquals("مرجع الفاتورة يكمل حرفيًا", "ف-2026, بند 4", rows[2][6])
        assertEquals("مواد بلا كسور زائدة وبفواصل محفوظة", "طابلوت (2.5×1250)؛ سلك, لف (3×900)", rows[2][5])
        assertEquals("ملاحظة الاقتباس والسطر الجديد تكمل حرفيًا", "قال: \"غدا\"\nوبعدها", rows[3][4])
        assertEquals("عملة فارغة تصير خلية فارغة", "", rows[2][3])
    }

    // ============ 4) حالات حدّية لمحرك RFC 4180 والمبالغ ============

    @Test
    fun `محرك Csv ياقتبس فقط عند الحاجة ولا يضاف اقتباس زائد`() {
        assertEquals("نص عادي", Csv.field("نص عادي"))
        assertEquals("\"به, فاصلة\"", Csv.field("به, فاصلة")) // فاصلة حقيقية تستوجب الاقتباس
        assertEquals("\"به \"\"اقتباس\"\"\"", Csv.field("به \"اقتباس\""))
        assertEquals("\"سطر\nجديد\"", Csv.field("سطر\nجديد"))
        assertEquals("", Csv.field(null))
        assertEquals("", Csv.field(""))
        assertEquals("\"\r\"", Csv.field("\r")) // CR وحده يستوجب الاقتباس حوله
        assertFalse("لا اقتباس زائد للنص السليم", Csv.field("مرحبا").startsWith("\""))
    }

    @Test
    fun `csvAmount يحول بدقة ويعالج الحالات الحدية`() {
        assertEquals("23000", Money.csvAmount(23_000.0))
        assertEquals("2345679", Money.csvAmount(2_345_678.9)) // تقريب مثل fmtLat القديم
        assertEquals("0", Money.csvAmount(0.0))
        assertEquals("-500", Money.csvAmount(-500.2)) // سالب محفوظ
        assertEquals("0", Money.csvAmount(Double.NaN))
        assertEquals("0", Money.csvAmount(Double.POSITIVE_INFINITY))
        assertEquals("1000", Money.csvAmount(1_000.0)) // حد الـ 1000 بلا فاصل
    }

    @Test
    fun `num يزيل الكسور الزائدة فقط`() {
        assertEquals("5", Csv.num(5.0))
        assertEquals("2.5", Csv.num(2.5))
        assertEquals("1250", Csv.num(1250.0))
        assertEquals("0.75", Csv.num(0.75))
    }

    @Test
    fun `الفئة المخصصة باسم فيه فاصلة تظهر باسمها الصحيح مقتبسة`() {
        // إصلاح فقدان الفئات المخصصة: categoryName كان يستدعى بلا settings في التصدير
        val settings = AppSettings(
            customExpense = listOf(Category("custom-1", "🛠️", "نقطة فني, السور", CategoryKind.EXPENSE)),
        )
        assertEquals("نقطة فني, السور", categoryName("custom-1", settings))
        val cell = Csv.field(categoryName("custom-1", settings))
        assertEquals("نقطة فني, السور", parseCsv(cell + "\n")[0][0])
    }

    @Test
    fun `الصيغة الجديدة كاملة تختلف جذريا عن القديمة في سلامة الأعمدة`() {
        val txs = realTxs()
        val oldRows = parseCsv(oldExportCsv(txs))
        val newRows = parseCsv(newExportCsv(txs))
        val oldBad = oldRows.count { it.size != 6 }
        val newBad = newRows.count { it.size != 6 }
        assertTrue("القديم مشطوب بجسامة", oldBad >= 190)
        assertEquals("الجديد بلا أي صف مشطوب", 0, newBad)
        assertNotEquals(oldBad, newBad)
    }

    // فك "yyyy-MM-dd HH:mm" إلى طابع زمني (تقريبًا — للمقارنة بدقيقة)
    private fun parseStamp(s: String): Long {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        return f.parse(s)!!.time
    }
}
