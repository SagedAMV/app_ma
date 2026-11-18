package com.mahfazty.smart.ui.util

import android.content.ContentResolver
import android.net.Uri
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream

/**
 * قراءة ملف النسخة الاحتياطية من منتقي الملفات بمحاولات متعددة وتشخيص كامل
 * (إصلاح رسالة «الملف فارغ - تعذر قراءة محتواه» المضللة).
 *
 * سبب العيب السابق: القراءة كانت بمحاولة واحدة عبر openInputStream، وأي فشل —
 * SecurityException من مزود الملفات، تيار فارغ من بعض مديري الملفات، خطأ I/O،
 * أو مزود يعيد null — يُبتلع بـ getOrNull() فيظهر للمستخدم «الملف فارغ» رغم أن
 * ملفه سليم (تأكدنا من ذلك: الملف الحقيقي 174KB وصالح كـ JSON).
 *
 * الآن ثلاث محاولات بترتيب يغطي أنماط فشل الهواتف الشائعة:
 *  1) openInputStream            — المسار القياسي عبر ContentResolver
 *  2) openFileDescriptor("r")    — مسار بديل ينجح مع مزودين يفشل تيارهم الأول
 *  3) openInputStream (إعادة)    — بعض المزودين يفشلون أول فتح بعد منح الإذن مباشرة
 *
 * كل محاولة تُسجل نتيجتها في التشخيص: نجاح بعدد البايتات، أو اسم الاستثناء ورسالته،
 * أو «المزود أعاد null» — فتظهر للمستخدم رسالة تقول السبب الحقيقي بدل «فارغ».
 *
 * النواة منفصلة عن android.content.ContentResolver عبر واجهة AttemptOpener
 * لتكون قابلة للاختبار على JVM بدون Robolectric.
 */
object BackupFileReader {

    /** مزود محاولة — يفتح تيارًا جديدًا عند النداء وقد يرمي استثناء (نمط المزودين الحقيقيين) */
    fun interface AttemptOpener {
        fun open(): InputStream?
    }

    /** نتيجة محاولة واحدة للتشخيص */
    data class Diagnostic(val attempt: String, val outcome: String) {
        override fun toString(): String = "$attempt — $outcome"
    }

    data class ReadResult(
        val ok: Boolean,                    // وصلنا نصًا غير فارغ
        val text: String?,                  // النص المقروء عند النجاح
        val diagnostics: List<Diagnostic>,  // تقرير كل محاولة بالترتيب
        val sourceDescription: String,      // من أين جاء الملف (المزود) للتشخيص
    ) {
        /** رسالة فشل القراءة للعرض في نافذة: الأسباب الحقيقية + نصيحة عملية */
        fun failureMessage(): String {
            val reasons = diagnostics.takeLast(4).joinToString("\n")
            return "تعذر قراءة الملف من: $sourceDescription\n\n$reasons\n\n" +
                "💡 انقل الملف إلى مجلد «التنزيلات» عبر مدير الملفات ثم أعد المحاولة واختره من هناك."
        }
    }

    /**
     * ينفذ المحاولات بالترتيب ويعيد أول نص غير فارغ، وإلا نتيجة فشل بتشخيص كامل.
     * لا تُرمى استثناءات للمستدعي — كلها تتحول لتشخيص.
     */
    fun read(sourceDescription: String, attempts: List<Pair<String, AttemptOpener>>): ReadResult {
        val diagnostics = mutableListOf<Diagnostic>()
        for ((label, opener) in attempts) {
            val stream = try {
                opener.open()
            } catch (e: Exception) {
                diagnostics += Diagnostic(label, "${e.javaClass.simpleName}: ${e.message ?: "بدون رسالة"}")
                continue
            }
            if (stream == null) {
                diagnostics += Diagnostic(label, "المزود أعاد null بدون سبب")
                continue
            }
            val bytes = try {
                stream.use { it.readBytes() }
            } catch (e: Exception) {
                diagnostics += Diagnostic(label, "فشل أثناء القراءة — ${e.javaClass.simpleName}: ${e.message ?: "بدون رسالة"}")
                continue
            }
            if (bytes.isEmpty()) {
                diagnostics += Diagnostic(label, "نجح الفتح لكن التيار فارغ (0 بايت)")
                continue
            }
            diagnostics += Diagnostic(label, "نجح — ${bytes.size} بايت")
            return ReadResult(true, String(bytes, Charsets.UTF_8), diagnostics, sourceDescription)
        }
        return ReadResult(false, null, diagnostics, sourceDescription)
    }
}

/**
 * المحاولات الحقيقية عبر ContentResolver — تُستخدم في شاشة الإعدادات عند الاستيراد.
 * sourceDescription يعرض مزود الملف ونوعه المُعلن لتشخيص يوضح من أين جاء الملف.
 */
fun ContentResolver.readBackupText(uri: Uri): BackupFileReader.ReadResult {
    val source = try {
        val type = getType(uri)
        "المزود ${uri.authority ?: "غير معروف"}" + if (type != null) " (نوع الملف المُعلن: $type)" else ""
    } catch (e: Exception) {
        "المزود ${uri.authority ?: uri.toString().take(60)}"
    }
    val attempts = listOf(
        "المحاولة 1 — openInputStream" to BackupFileReader.AttemptOpener { openInputStream(uri) },
        "المحاولة 2 — openFileDescriptor" to BackupFileReader.AttemptOpener {
            val pfd = openFileDescriptor(uri, "r") ?: return@AttemptOpener null
            // الفتح عبر FileDescriptor مع غلق pfd عند غلق التيار (لا تسريب موارد)
            object : FilterInputStream(FileInputStream(pfd.fileDescriptor)) {
                override fun close() {
                    super.close()
                    pfd.close()
                }
            }
        },
        "المحاولة 3 — إعادة فتح التيار" to BackupFileReader.AttemptOpener { openInputStream(uri) },
    )
    return BackupFileReader.read(source, attempts)
}
