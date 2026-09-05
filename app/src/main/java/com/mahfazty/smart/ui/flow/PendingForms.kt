package com.mahfazty.smart.ui.flow

import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.MaterialItem
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet

/**
 * ترتيب الواجهات: مسودات تُحفظ عند رفض العملية لنقص الرصيد،
 * ثم تُعاد نفس الواجهة بنفس البيانات بعد الشحن/السحب.
 */

data class PendingClientOp(
    val type: OpType,
    val amount: Double,
    val note: String?,
    val materials: List<MaterialItem>,
    val receiptPath: String?,
    val isInvoice: Boolean = false,
    val invoiceRef: String? = null,
    val editing: ClientOperation? = null,
) {
    fun toInitial(): ClientOperation {
        val base = editing
        return ClientOperation(
            id = base?.id ?: 0L,
            accountId = base?.accountId ?: 0L,
            type = type,
            amount = amount,
            note = note,
            date = base?.date ?: 0L,
            materials = materials,
            receiptPath = receiptPath,
            isInvoice = isInvoice,
            invoiceRef = invoiceRef,
            invoiceDelivered = base?.invoiceDelivered ?: false,
        )
    }
}

sealed class PendingWalletAction {
    data class Tx(
        val type: TxType,
        val amount: Double,
        val category: String,
        val note: String?,
        val wallet: Wallet,
        val editing: Transaction? = null,
    ) : PendingWalletAction()

    data class Transfer(
        val direction: String,
        val amount: Double,
        val note: String?,
    ) : PendingWalletAction()

    data class Contribute(
        val goalId: Long,
        val goalName: String,
        val add: Boolean,
        val amount: Double,
    ) : PendingWalletAction()

    data class SavingsAdd(val amount: Double) : PendingWalletAction()
}
