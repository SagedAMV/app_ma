package com.mahfazty.smart.domain

import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات عزل منطق استيراد النسخة الاحتياطية (إصلاح استيراد النسخ الاحتياطية).
 *
 * الاختبار الأول يعمل على **ملف نسخة احتياطية حقيقي** أنتجه التطبيق فعليًا
 * (474 عملية، 8 عملاء، 12 حسابًا، 86 عملية عميل، 54 تحويلًا) — وهو الملف الذي
 * رفضه التطبيق سابقًا عند الاستيراد.
 */
class BackupParserTest {

    private fun realBackupText(): String =
        javaClass.classLoader!!.getResourceAsStream("backup/real-backup-2026-11-17.json")!!
            .readBytes().toString(Charsets.UTF_8)

    // ============ 1) الملف الحقيقي ============

    @Test
    fun `النسخة الاحتياطية الحقيقية تُحلل بالكامل بلا أي خطأ`() {
        val result = BackupParser.parse(realBackupText())
        assertTrue("يجب أن يقبل الملف الحقيقي — الأخطاء: ${result.errors.take(3)}", result.ok)
        assertTrue(result.errors.isEmpty())
        val b = result.backup!!

        assertEquals(474, b.transactions.size)
        assertEquals(0, b.goals.size)
        assertEquals(8, b.clients.size)
        assertEquals(12, b.accounts.size)
        assertEquals(86, b.operations.size)
        assertEquals(54, b.transfers.size)
        assertTrue(b.hasSavings)
        assertEquals(16, b.settings.size)
        assertEquals("ساجد", b.settings["name"])
    }

    @Test
    fun `أنواع العمليات والمحافظ تُحفظ كما هي بلا فساد صامت`() {
        val b = BackupParser.parse(realBackupText()).backup!!
        // الملف يحتوي عمليات TRANSFER — يجب ألا تُفسد إلى EXPENSE كما كان يفعل الكود القديم
        val byType = b.transactions.groupingBy { it.type }.eachCount()
        assertEquals(54, byType[TxType.INCOME])
        assertEquals(397, byType[TxType.EXPENSE])
        assertEquals(23, byType[TxType.TRANSFER])
        val byWallet = b.transactions.groupingBy { it.wallet }.eachCount()
        assertEquals(386, byWallet[Wallet.CASH])
        assertEquals(88, byWallet[Wallet.BANK])
    }

    @Test
    fun `حقول الملف الحقيقي بقيمها الدقيقة`() {
        val b = BackupParser.parse(realBackupText()).backup!!
        // أول عملية في الملف: دخل 6000 كاش من «سحب من رصيد حقيقي إلى الكاش»
        val first = b.transactions.first()
        assertEquals(TxType.INCOME, first.type)
        assertEquals(6000.0, first.amount, 0.0)
        assertEquals("client_withdraw", first.category)
        assertEquals("سحب من رصيد حقيقي إلى الكاش", first.note)
        assertEquals(Wallet.CASH, first.wallet)
        assertNull(first.oldGoalId)
        // ملاحظة فارغة (null) تظل null لا ""
        assertTrue(b.transactions.any { it.note == null })
        // المبالغ كلها موجبة في الملف الحقيقي
        assertTrue(b.transactions.all { it.amount > 0 })
    }

    // ============ 2) التحمّل: BOM، حالة الأحرف، أرقام نصية، حقول ناقصة ============

    @Test
    fun `يتحمّل BOM وحالة الأحرف والأرقام النصية والحقول الناقصة`() {
        val text = "\uFEFF" + """
        {
          "app": "Mahfazty Smart",
          "backupVersion": 1,
          "data": {
            "transactions": [
              {"id": 1, "type": "income", "amount": "250", "category": "food",
               "note": null, "date": "1792776510719", "goalId": null, "wallet": "cash"}
            ],
            "goals": [],
            "clients": [
              {"id": 7, "name": "عميل قديم", "phone": null, "photoPath": null}
            ],
            "accounts": [
              {"id": 70, "clientId": 7, "name": "حساب", "icon": null, "realBalance": "1500"}
            ],
            "operations": [
              {"id": 700, "accountId": 70, "type": "debt", "amount": 900,
               "note": "دين", "date": 1792776510719}
            ]
          }
        }
        """.trimIndent()
        val result = BackupParser.parse(text)
        assertTrue("الأخطاء: ${result.errors}", result.ok)
        val b = result.backup!!
        assertEquals(TxType.INCOME, b.transactions[0].type)
        assertEquals(250.0, b.transactions[0].amount, 0.0)
        assertEquals(1792776510719L, b.transactions[0].date)
        assertEquals(Wallet.CASH, b.transactions[0].wallet)
        assertEquals("active", b.clients[0].status)          // إصلاح data-3
        assertFalse(b.hasSavings)                            // لا قسم ادخار → لا يُمسّ
        assertEquals(0.0, b.savingsOpening, 0.0)
        val op = b.operations[0]
        assertEquals(OpType.DEBT, op.type)
        assertFalse(op.isInvoice)                            // إصلاح data-1
        assertNull(op.dueDate)                               // إصلاح data-4
        assertNull(op.currency)
        assertNull(op.invoiceRef)
        assertEquals(1500.0, b.accounts[0].realBalance, 0.0)
    }

    // ============ 3) رسائل الخطأ الدقيقة ============

    @Test
    fun `خطأ نوع غير معروف يذكر القسم ورقم السجل والقيمة`() {
        val text = """
        {"data": {"transactions": [
          {"type": "INCOME", "amount": 10, "category": "food", "date": 1, "wallet": "CASH"},
          {"type": "TRNSFER", "amount": 5, "category": "other", "date": 2, "wallet": "BANK"}
        ]}}
        """.trimIndent()
        val result = BackupParser.parse(text)
        assertFalse(result.ok)
        val msg = result.errorMessage()
        assertTrue("الرسالة: $msg", msg.contains("العمليات المالية"))
        assertTrue("الرسالة: $msg", msg.contains("السجل رقم 2"))
        assertTrue("الرسالة: $msg", msg.contains("TRNSFER"))
    }

    @Test
    fun `مفتاح مطلوب مفقود يذكر اسم المفتاح وموقعه`() {
        val text = """
        {"data": {"transactions": [
          {"type": "EXPENSE", "category": "food", "date": 1, "wallet": "CASH"},
          {"type": "INCOME", "amount": 10, "category": "food", "date": 2, "wallet": "CASH"},
          {"type": "EXPENSE", "amount": 7, "date": 3, "wallet": "CASH"}
        ]}}
        """.trimIndent()
        val result = BackupParser.parse(text)
        assertFalse(result.ok)
        val msg = result.errorMessage()
        assertTrue("الرسالة: $msg", msg.contains("«amount» مفقود"))
        assertTrue("الرسالة: $msg", msg.contains("السجل رقم 1"))
    }

    @Test
    fun `الإعدادات الفارغة وnull لا تُستورد`() {
        val text = """
        {"data": {"settings": {"name": "ساجد", "hide_savings": null, "empty": "  "}}}
        """.trimIndent()
        val result = BackupParser.parse(text)
        assertTrue(result.ok)
        val s = result.backup!!.settings
        assertEquals(mapOf("name" to "ساجد"), s)
    }

    // ============ 4) الرفض الصحيح للملفات غير الصالحة ============

    @Test
    fun `نص ليس JSON يُرفض برسالة واضحة`() {
        val result = BackupParser.parse("هذا ملف تالف ليس JSON")
        assertFalse(result.ok)
        assertTrue(result.errorMessage().contains("ليس ملف JSON صالحًا"))
    }

    @Test
    fun `JSON بلا قسم data يُرفض`() {
        val result = BackupParser.parse("""{"app": "something-else"}""")
        assertFalse(result.ok)
        assertTrue(result.errorMessage().contains("قسم البيانات"))
    }

    @Test
    fun `ملف فارغ يُرفض`() {
        assertFalse(BackupParser.parse("   ").ok)
        assertFalse(BackupParser.parse("").ok)
    }

    @Test
    fun `قسم data ليس كائنًا يُرفض بأمان`() {
        val result = BackupParser.parse("""{"data": "مصفوفة نصية"}""")
        assertFalse(result.ok)
        assertTrue(result.errorMessage().contains("قسم البيانات"))
    }
}
