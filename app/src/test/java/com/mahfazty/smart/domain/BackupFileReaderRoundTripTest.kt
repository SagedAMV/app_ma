package com.mahfazty.smart.domain

import com.mahfazty.smart.ui.util.BackupFileReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream

/**
 * اختبار الوحدة الحقيقي لمشكلة «الملف فارغ - تعذر قراءة محتواه» (v2.8.2).
 *
 * القصة الفعلية: المستخدم صدّر نسخة احتياطية سليمة (174KB، JSON صالح)، ثم عند
 * الاستيراد ظهرت «الملف فارغ» — لأن كود القراءة القديم كان بمحاولة واحدة عبر
 * openInputStream وأي فشل (SecurityException، تيار فارغ من مزود الملفات، خطأ I/O)
 * يُبتلع بـ getOrNull() ويتحول للرسالة المضللة نفسها.
 *
 * هذه الاختبارات تحاكي المستخدم خطوة بخطوة:
 *  1) الملف الحقيقي نفسه (مورد الاختبار = نسخة طبق الأصل من الملف المرفوع للمطور)
 *  2) القراءة عبر مسارات مزود الملفات بأنماط فشل الهواتف الحقيقية
 *  3) ملف مُفرَّق/مقطوع أثناء النقل
 *  4) تيار مُجزَّأ بقراءات صغيرة عبر حدود المقاطع
 *  5) BOM من أدوات المشاركة
 * ويؤكد أن القراءة الجديدة (BackupFileReader بمحاولاتها الثلاث وتشخيصها) تنجح
 * حيث فشل الكود القديم، وأن رسالة «الملف فارغ» لا تظهر إلا للملف الفارغ فعلاً.
 */
class BackupFileReaderRoundTripTest {

    /** نسخة طبق الأصل من الملف الذي رفعه المستخدم (174,428 بايت، JSON صالح) */
    private fun realBackupBytes(): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("backup/real-backup-2026-11-17.json")!!
            .readBytes()

    private fun realBackupText(): String = String(realBackupBytes(), Charsets.UTF_8)

    /** تيار يسلّم عدة بايتات صغيرة في كل read() — يحاكي مزودًا يقطع البيانات */
    private class ChunkedStream(private val data: ByteArray, private val chunkSize: Int) : InputStream() {
        private var pos = 0
        override fun read(): Int = if (pos >= data.size) -1 else (data[pos++].toInt() and 0xFF)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (pos >= data.size) return -1
            val n = minOf(chunkSize, len, data.size - pos)
            System.arraycopy(data, pos, b, off, n)
            pos += n
            return n
        }
    }

    // ============ 1) رحلة المستخدم الكاملة: ملف سليم → قراءة → تحليل ============

    @Test
    fun `الملف الحقيقي يُقرأ عبر مسار المزود السليم ويُحلل ويُستورد بنجاح`() {
        val bytes = realBackupBytes()
        assertEquals("حجم الملف الحقيقي", 174428, bytes.size)

        // كود القراءة الجديد نفسه: محاولة openInputStream ناجحة (مزود صحي)
        val result = BackupFileReader.read(
            "المزود com.android.providers.downloads (نوع الملف المُعلن: application/json)",
            listOf("المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                ByteArrayInputStream(bytes)
            }),
        )
        assertTrue("القراءة يجب أن تنجح: ${result.diagnostics}", result.ok)
        assertEquals("النص المقروء = الملف كاملاً بلا فقدان", String(bytes, Charsets.UTF_8), result.text)

        // ثم يمر النص على BackupParser ثم إلى importBackup — هنا نؤكد التحليل
        val parsed = BackupParser.parse(result.text!!)
        assertTrue("التحليل يجب أن ينجح — الأخطاء: ${parsed.errors.take(3)}", parsed.ok)
        val b = parsed.backup!!
        assertEquals("عدد العمليات المالية", 474, b.transactions.size)
        assertEquals("عدد الأهداف", 0, b.goals.size)
        assertEquals("عدد العملاء", 8, b.clients.size)
        assertEquals("عدد الحسابات", 12, b.accounts.size)
        assertEquals("عدد عمليات العملاء", 86, b.operations.size)
        assertEquals("عدد التحويلات", 54, b.transfers.size)
        assertEquals("عدد الإعدادات", 16, b.settings.size)
        assertTrue("قسم الادخار موجود", b.hasSavings)
        assertEquals("هدف الادخار", 800_000.0, b.savingsGoal, 0.0)
    }

    // ============ 2) محاكاة فشل الهاتف الفعلي ============

    @Test
    fun `فشل الهاتف — استثناء صلاحية ثم تيار فارغ ثم نجاح عبر FileDescriptor والاستيراد ينجح`() {
        val bytes = realBackupBytes()
        // نمط فشل حقيقي: المزود يرفض التيار الأول، التيار الثاني يأتي فارغًا،
        // والفتح عبر openFileDescriptor ينجح — الكود القديم كان يظهر «الملف فارغ» هنا
        val result = BackupFileReader.read(
            "المزود com.android.externalstorage.documents",
            listOf(
                "المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                    throw SecurityException("Permission Denial: opening provider com.android.providers.downloads")
                },
                "المحاولة 2 — openFileDescriptor" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(ByteArray(0)) // مزود يفتح لكن يسلم 0 بايت
                },
                "المحاولة 3 — إعادة فتح التيار" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(bytes)
                },
            ),
        )
        assertTrue("القارئ الجديد يتعافى حيث فشل القديم: ${result.diagnostics}", result.ok)
        assertEquals(String(bytes, Charsets.UTF_8), result.text)
        val parsed = BackupParser.parse(result.text!!)
        assertTrue(parsed.ok)
        assertEquals(474, parsed.backup!!.transactions.size)
        // التشخيص يحفظ السبب الحقيقي للمحاولتين الفاشلتين
        val diagText = result.diagnostics.joinToString()
        assertTrue("يجب ذكر SecurityException في التشخيص", diagText.contains("SecurityException"))
        assertTrue("يجب ذكر التيار الفارغ في التشخيص", diagText.contains("0 بايت"))
    }

    @Test
    fun `المزود يعيد null مرتين ثم المحاولة الثالثة تنجح — تعافٍ وتشخيص كاملان`() {
        val bytes = realBackupBytes()
        val result = BackupFileReader.read(
            "المزود com.mi.android.providers.files",
            listOf(
                "المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener { null },
                "المحاولة 2 — openFileDescriptor" to BackupFileReader.AttemptOpener { null },
                "المحاولة 3 — إعادة فتح التيار" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(bytes)
                },
            ),
        )
        assertTrue(result.ok)
        assertEquals(String(bytes, Charsets.UTF_8), result.text)
        assertEquals(2, result.diagnostics.count { it.outcome.contains("أعاد null") })
    }

    // ============ 3) فشل القراءة كله = سبب حقيقي بدل «الملف فارغ» ============

    @Test
    fun `فشل كل المحاولات يظهر الأسباب الحقيقية ولا يقول «الملف فارغ» أبداً`() {
        val result = BackupFileReader.read(
            "المزود com.some.cloud.provider",
            listOf(
                "المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                    throw SecurityException("Permission Denial: require READ_EXTERNAL_STORAGE")
                },
                "المحاولة 2 — openFileDescriptor" to BackupFileReader.AttemptOpener {
                    throw IOException("Connection closed by peer")
                },
                "المحاولة 3 — إعادة فتح التيار" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(ByteArray(0))
                },
            ),
        )
        assertFalse("لا نجاح بلا نص", result.ok)
        assertNull("لا نص عند الفشل الكامل", result.text)

        val msg = result.failureMessage()
        // الرسالة تقول السبب الحقيقي لكل محاولة + اسم المزود + نصيحة عملية
        assertTrue("تذكر اسم الاستثناء الحقيقي", msg.contains("SecurityException"))
        assertTrue("تذكر خطأ I/O الحقيقي", msg.contains("IOException"))
        assertTrue("تذكر التيار الفارغ", msg.contains("0 بايت"))
        assertTrue("تذكر المزود", msg.contains("المزود com.some.cloud.provider"))
        assertTrue("تحتوي نصيحة النقل إلى التنزيلات", msg.contains("التنزيلات"))
        // والأهم: لا تكرر الكذبة القديمة
        assertFalse("لا تظهر رسالة «الملف فارغ» المضللة", msg.contains("الملف فارغ"))
    }

    // ============ 4) الملف المُفرَّق/المقطوع ============

    @Test
    fun `ملف مفرق مقطوع أثناء النقل يعطي رسالة JSON واضحة لا «الملف فارغ»`() {
        val full = realBackupText()
        // نصف الملف الأول — نمط ملف نُقل ناقصاً (نقل متقطع / مزود أغلق مبكراً)
        val truncated = full.substring(0, full.length / 2)

        val result = BackupFileReader.read(
            "المزود com.android.providers.downloads",
            listOf("المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                ByteArrayInputStream(truncated.toByteArray(Charsets.UTF_8))
            }),
        )
        // القراءة تنجح — الملف مقطوع لكن ليس فارغًا
        assertTrue(result.ok)
        assertNotEquals(full, result.text)

        // التحليل يرفضه برسالة تقول أنه ليس JSON صالحًا — لا «فارغ» ولا انهيار
        val parsed = BackupParser.parse(result.text!!)
        assertFalse("المقطوع يجب أن يُرفض", parsed.ok)
        val msg = parsed.errorMessage()
        assertTrue("الرسالة تتحدث عن JSON: $msg", msg.contains("JSON"))
        assertFalse("لا رسالة «فارغ» المضللة للملف المقطوع", msg.contains("الملف فارغ"))
        assertEquals("الخطأ في قسم الملف", "الملف", parsed.errors.first().section)
    }

    @Test
    fun `دمج ملف مفرق إلى قطعتين ينجح بعد دمج القطع بالترتيب`() {
        val bytes = realBackupBytes()
        // المستخدم وصلته القطعة الأولى فقط ثم اكتمل لديه الملف بدمج القطعتين
        val cut = bytes.size / 2
        val part1 = bytes.copyOfRange(0, cut)
        val part2 = bytes.copyOfRange(cut, bytes.size)
        val merged = SequenceInputStream(
            ByteArrayInputStream(part1),
            ByteArrayInputStream(part2),
        )
        val result = BackupFileReader.read(
            "المزود com.android.providers.downloads",
            listOf("المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener { merged }),
        )
        assertTrue(result.ok)
        assertEquals(String(bytes, Charsets.UTF_8), result.text)
        val parsed = BackupParser.parse(result.text!!)
        assertTrue(parsed.ok)
        assertEquals(474, parsed.backup!!.transactions.size)
    }

    // ============ 5) تيار مُجزَّأ عبر حدود المقاطع ============

    @Test
    fun `تيار يسلّم مقاطع صغيرة عبر حدود الوسم المتعدد البايتات لا يفقد بايتاً واحداً`() {
        val bytes = realBackupBytes()
        val result = BackupFileReader.read(
            "المزود com.android.providers.downloads",
            listOf("المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                ChunkedStream(bytes, chunkSize = 7) // 174428 ليس من مضاعفات 7 — حد مقطع داخل البيانات
            }),
        )
        assertTrue(result.ok)
        assertEquals("لا فقدان عبر حدود المقاطع", bytes.size, result.text!!.toByteArray(Charsets.UTF_8).size)
        val parsed = BackupParser.parse(result.text!!)
        assertTrue(parsed.ok)
        assertEquals(474, parsed.backup!!.transactions.size)
    }

    // ============ 6) BOM من أدوات المشاركة ============

    @Test
    fun `BOM يضيفه تطبيق المشاركة يُتحمل والاستيراد ينجح`() {
        val withBom = "\uFEFF" + realBackupText()
        val result = BackupFileReader.read(
            "المزود com.whatsapp (نوع الملف المُعلن: application/octet-stream)",
            listOf("المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                ByteArrayInputStream(withBom.toByteArray(Charsets.UTF_8))
            }),
        )
        assertTrue(result.ok)
        val parsed = BackupParser.parse(result.text!!)
        assertTrue("المحلل يتحمل BOM: ${parsed.errors.take(2)}", parsed.ok)
        assertEquals(474, parsed.backup!!.transactions.size)
    }

    // ============ 7) الملف الفارغ فعلاً = تشخيص دقيق لا رسالة مضللة ============

    @Test
    fun `ملف فارغ فعلاً يُشخص بدقة التيار فارغ 0 بايت مع المزود`() {
        // فتح ناجح لمحتوى فارغ فعلاً (مثلًا المستخدم اختار ملفًا كتبه تطبيق آخر بلا محتوى)
        val result = BackupFileReader.read(
            "المزود com.android.providers.downloads",
            listOf(
                "المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(ByteArray(0))
                },
                "المحاولة 2 — openFileDescriptor" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(ByteArray(0))
                },
                "المحاولة 3 — إعادة فتح التيار" to BackupFileReader.AttemptOpener {
                    ByteArrayInputStream(ByteArray(0))
                },
            ),
        )
        assertFalse("لا نص للملف الفارغ فعلاً", result.ok)
        val msg = result.failureMessage()
        // التشخيص يقول الحقيقة الدقيقة بدل «الملف فارغ» العامة: كل محاولة فتحت بنجاح
        // لكن المزود سلم 0 بايت — أي أن الملف على الجهاز فارغ المحتوى فعلاً
        assertEquals(3, result.diagnostics.count { it.outcome.contains("0 بايت") })
        assertTrue("التشخيص يذكر المزود", msg.contains("المزود com.android.providers.downloads"))
        assertTrue("التشخيص يذكر عدد البايتات الصفري", msg.contains("0 بايت"))
    }

    // ============ 8) كود القراءة القديم: إثبات أن العيب كان فيه ============

    @Test
    fun `كود القراءة القديم يفشل مع نمط فشل الهاتف بينما الجديد ينجح — إثبات الإصلاح`() {
        val bytes = realBackupBytes()

        // مزود الهاتف الحقيقي: أول فتح يفشل (يعيد null)، الفتحات التالية تنجح
        var openCount = 0
        val phoneProvider = BackupFileReader.AttemptOpener {
            openCount++
            if (openCount == 1) null else ByteArrayInputStream(bytes)
        }

        // الكود القديم بالحرف (SettingsScreen قبل الإصلاح): محاولة واحدة ثم getOrNull()
        val oldText = runCatching {
            phoneProvider.open()?.use { input -> input.bufferedReader(Charsets.UTF_8).readText() }
        }.getOrNull()
        assertNull("القديم: المزود فشل مرة → نص null → يُمرر \"\" → تظهر «الملف فارغ» المضللة", oldText)

        // الجديد على نفس المزود وبنفس التسلسل: المحاولة الثالثة تنجح
        val newResult = BackupFileReader.read(
            "المزود com.android.providers.downloads",
            listOf(
                "المحاولة 1 — openInputStream" to phoneProvider,
                "المحاولة 2 — openFileDescriptor" to phoneProvider,
                "المحاولة 3 — إعادة فتح التيار" to phoneProvider,
            ),
        )
        assertTrue("الجديد يتعافى حيث فشل القديم", newResult.ok)
        assertNotNull(newResult.text)
        assertEquals("نفس الملف يُستورد كاملاً بعد الإصلاح", 474, BackupParser.parse(newResult.text!!).backup!!.transactions.size)
    }
}
