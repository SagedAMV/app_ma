package com.mahfazty.smart.domain

import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.Category
import com.mahfazty.smart.domain.model.CategoryIds
import com.mahfazty.smart.domain.model.CategoryKind

/**
 * الفئات الافتراضية + دمج الفئات المخصصة.
 * نفس تصنيفات نسخة الويب تماماً.
 */
object DefaultCategories {

    val expense: List<Category> = listOf(
        Category("food", "🍔", "طعام", CategoryKind.EXPENSE),
        Category("transport", "🚕", "مواصلات", CategoryKind.EXPENSE),
        Category("shopping", "🛍️", "تسوق", CategoryKind.EXPENSE),
        Category("bills", "💡", "فواتير", CategoryKind.EXPENSE),
        Category("home", "🏠", "سكن", CategoryKind.EXPENSE),
        Category("health", "💊", "صحة", CategoryKind.EXPENSE),
        Category("fun", "🎬", "ترفيه", CategoryKind.EXPENSE),
        Category("other", "📦", "أخرى", CategoryKind.EXPENSE),
    )

    val income: List<Category> = listOf(
        Category("salary", "💼", "راتب", CategoryKind.INCOME),
        Category("freelance", "💻", "عمل حر", CategoryKind.INCOME),
        Category("gift", "🎁", "هدية", CategoryKind.INCOME),
        Category("other", "💰", "أخرى", CategoryKind.INCOME),
    )

    val transfer: List<Category> = listOf(
        Category(CategoryIds.GOAL_ADD, "🎯", "إضافة لهدف", CategoryKind.TRANSFER),
        Category(CategoryIds.GOAL_WITHDRAW, "💸", "سحب من هدف", CategoryKind.TRANSFER),
        Category(CategoryIds.SAVINGS_ADD, "🐷", "إضافة للادخار", CategoryKind.TRANSFER),
        Category(CategoryIds.SAVINGS_WITHDRAW, "🏦", "سحب من الادخار", CategoryKind.TRANSFER),
        Category(CategoryIds.BANK_TO_CASH, "🏦➡️💵", "سحب من البنك للكاش", CategoryKind.TRANSFER),
        Category(CategoryIds.CASH_TO_BANK, "💵➡️🏦", "إيداع من الكاش للبنك", CategoryKind.TRANSFER),
    )
}

/** كل الفئات المتاحة = الافتراضية + المخصصة من الإعدادات */
fun allCategories(settings: AppSettings?): Map<CategoryKind, List<Category>> = mapOf(
    CategoryKind.EXPENSE to DefaultCategories.expense + (settings?.customExpense ?: emptyList()),
    CategoryKind.INCOME to DefaultCategories.income + (settings?.customIncome ?: emptyList()),
    CategoryKind.TRANSFER to DefaultCategories.transfer,
)

/** البحث عن فئة بمعرفها عبر كل الأنواع */
fun findCategory(id: String?, settings: AppSettings? = null): Category? {
    if (id == null) return null
    return allCategories(settings).values.flatten().firstOrNull { it.id == id }
}

/** اسم الفئة (مع بديل آمن للفئات المحذوفة) */
fun categoryName(id: String, settings: AppSettings? = null): String =
    findCategory(id, settings)?.name ?: when (id) {
        CategoryIds.GOAL_ADD -> "إضافة لهدف"
        CategoryIds.GOAL_WITHDRAW -> "سحب من هدف"
        CategoryIds.SAVINGS_ADD -> "إضافة للادخار"
        CategoryIds.SAVINGS_WITHDRAW -> "سحب من الادخار"
        CategoryIds.BANK_TO_CASH -> "سحب من البنك للكاش"
        CategoryIds.CASH_TO_BANK -> "إيداع من الكاش للبنك"
        CategoryIds.CLIENT_FUND -> "شحن رصيد حقيقي"
        CategoryIds.CLIENT_WITHDRAW -> "سحب من رصيد حقيقي"
        else -> "أخرى"
    }

/** أيقونة الفئة */
fun categoryIcon(id: String): String = findCategory(id)?.icon ?: "📦"
