package com.mahfazty.smart.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mahfazty.smart.MahfaztyApp
import com.mahfazty.smart.domain.model.OpType
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * تذكير يومي بالديون المستحقة (إضافة 3.4 من تقرير الفحص).
 *
 * عامل WorkManager يعمل مرة كل يوم حتى لو لم يفتح المستخدم التطبيق:
 * يفحص الديون التي حان أو تجاوز تاريخ استحقاقها عبر كل العملاء،
 * وإذا وُجدت يبثّ إشعار [DuesNotifier] (معرّف ثابت — يُحدَّث ولا يتكدس).
 *
 * يعمل فقط مع إذن الإشعارات (أندرويد 13+) — وإلا يخرج بهدوء دون فشل.
 */
class DuesWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // أندرويد 13+: بلا إذن POST_NOTIFICATIONS لا معنى للبث — نجاح صامت
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val container = (applicationContext as MahfaztyApp).container
        val data = container.clientsRepository.clientsWithData.first()

        // نهاية «اليوم» = منتصف ليلة الغد — كل استحقاق قبلها حان وقته
        val endOfToday = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var due = 0
        val names = mutableListOf<String>()
        data.forEach { c ->
            c.accounts.forEach { a ->
                a.operations.forEach { op ->
                    val d = op.dueDate
                    if (op.type == OpType.DEBT && d != null && d < endOfToday) {
                        due++
                        if (names.size < 3) names.add("${c.client.name} • ${a.account.name}")
                    }
                }
            }
        }
        if (due > 0) DuesNotifier.post(applicationContext, due, names)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "dues_daily_reminder"

        /**
         * جدولة التذكير اليومي (مرة واحدة — KEEP لا تعيد الجدولة إن وُجدت أصلاً).
         * تُستدعى من MahfaztyApp.onCreate.
         */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DuesWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
