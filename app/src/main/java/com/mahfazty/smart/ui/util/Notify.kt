package com.mahfazty.smart.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mahfazty.smart.MainActivity
import com.mahfazty.smart.R

/**
 * تذكير استحقاق الديون (إصلاح data-1).
 *
 * إشعار محلي واحد بمعرّف ثابت (101) وقناة «dues» — عند كل فحص يُستبدل
 * بمعلوماته الجديدة بدل تكديس إشعارات متكررة في درج الإشعارات.
 */
object DuesNotifier {
    const val CHANNEL_ID = "dues"
    const val NOTIFICATION_ID = 101

    /**
     * بث (أو تحديث) إشعار الديون المستحقة.
     * @param count عدد الديون المستحقة أو المتأخرة
     * @param names أمثلة على أسماء «العميل • الحساب» (تُعرض في النص)
     */
    fun post(context: Context, count: Int, names: List<String>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // إنشاء القناة هنا أيضاً (لا تعلق بمكان واحد فقط — أمان إضافي)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "تذكير استحقاق الديون", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "تنبيه ديون مستحقة أو متأخرة لدى عملائك"
                },
            )
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = names.joinToString(" • ")
        nm.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⏰ $count دين مستحق")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build(),
        )
    }
}
