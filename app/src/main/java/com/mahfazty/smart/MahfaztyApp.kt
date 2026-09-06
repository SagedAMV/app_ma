package com.mahfazty.smart

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mahfazty.smart.data.ClientsRepository
import com.mahfazty.smart.data.SettingsRepository
import com.mahfazty.smart.data.WalletRepository
import com.mahfazty.smart.data.db.AppDatabase
import com.mahfazty.smart.ui.util.DuesNotifier

/**
 * نقطة دخول التطبيق + حاوية التبعيات اليدوية (Manual DI).
 * بسيطة وواضحة لمشروع بهذا الحجم — بدون مكتبات حقن خارجية.
 */
class MahfaztyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // إصلاح data-1: قناة إشعارات تذكير استحقاق الديون — تُنشأ مرة واحدة عند بدء التطبيق
        createDuesChannel()
    }

    private fun createDuesChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    DuesNotifier.CHANNEL_ID,
                    "تذكير استحقاق الديون",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "تنبيه ديون مستحقة أو متأخرة لدى عملائك"
                },
            )
        }
    }
}

class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(database) }

    val walletRepository: WalletRepository by lazy {
        WalletRepository(database, settingsRepository)
    }

    val clientsRepository: ClientsRepository by lazy {
        ClientsRepository(database, walletRepository)
    }
}
