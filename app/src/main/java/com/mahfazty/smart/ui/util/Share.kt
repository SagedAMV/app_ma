package com.mahfazty.smart.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** مشاركة ملف عبر نافذة المشاركة */
fun shareFile(context: Context, uri: Uri, mimeType: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

/** مشاركة نص عبر واتساب (مع بديل عام عند غياب التطبيق) */
fun shareViaWhatsApp(context: Context, text: String, phone: String?) {
    val digits = phone?.filter { it.isDigit() }.orEmpty()
    val url = if (digits.isNotEmpty()) "https://wa.me/$digits?text=${Uri.encode(text)}"
    else "https://wa.me/?text=${Uri.encode(text)}"
    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
    runCatching { context.startActivity(intent) }.onFailure {
        // بديل: مشاركة نصية عامة
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "مشاركة")) }
    }
}
