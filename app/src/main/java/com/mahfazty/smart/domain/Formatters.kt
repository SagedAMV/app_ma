package com.mahfazty.smart.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** مولّد معرفات فريدة (نفس نهج نسخة الويب: زمن + حماية من التكرار) */
object Ids {
    private var last = 0L

    @Synchronized
    fun next(): Long {
        var id = System.currentTimeMillis()
        if (id <= last) id = last + 1
        last = id
        return id
    }
}

/** تنسيق التواريخ بالعربية */
object Dates {
    private val dayDateFmt = SimpleDateFormat("EEEE d MMMM", Locale("ar"))
    private val shortDateFmt = SimpleDateFormat("d MMMM", Locale("ar"))
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale("ar"))
    private val dateTimeFmt = SimpleDateFormat("d MMMM yyyy • h:mm a", Locale("ar"))
    private val fileFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** "الخميس 12 أغسطس" */
    fun day(ts: Long): String = dayDateFmt.format(Date(ts))

    /** "12 أغسطس" */
    fun short(ts: Long): String = shortDateFmt.format(Date(ts))

    /** "أغسطس 2026" */
    fun month(ts: Long): String = monthFmt.format(Date(ts))

    // ملاحظة فحص: حُذفت الدالة time() — كانت ميتة لا يستدعيها أي مكان في التطبيق.

    /** "12 أغسطس 2026 • 10:30 م" */
    fun dateTime(ts: Long): String = dateTimeFmt.format(Date(ts))

    fun fileStamp(ts: Long): String = fileFmt.format(Date(ts))

    /** تسمية المجموعة في السجل: اليوم / أمس / التاريخ */
    fun groupLabel(ts: Long, now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val startOfDay: (Calendar) -> Unit = {
            it.set(Calendar.HOUR_OF_DAY, 0); it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0); it.set(Calendar.MILLISECOND, 0)
        }
        startOfDay(cal); startOfDay(nowCal)
        val diffDays = ((nowCal.timeInMillis - cal.timeInMillis) / 86_400_000L).toInt()
        return when {
            diffDays == 0 -> "اليوم"
            diffDays == 1 -> "أمس"
            diffDays < 7 -> short(ts)
            else -> short(ts)
        }
    }

    /** مفتاح فريد للمجموعة (اليوم 1/أمس 2/يوم محدد 3) */
    fun groupKey(ts: Long, now: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val startOfDay: (Calendar) -> Unit = {
            it.set(Calendar.HOUR_OF_DAY, 0); it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0); it.set(Calendar.MILLISECOND, 0)
        }
        startOfDay(cal); startOfDay(nowCal)
        val diffDays = ((nowCal.timeInMillis - cal.timeInMillis) / 86_400_000L).toInt()
        return when (diffDays) {
            0 -> 0
            1 -> 1
            else -> 2 + diffDays * 100
        }
    }
}
