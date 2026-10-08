package com.servicecenter.app.ui.common

import com.servicecenter.app.core.AppException
import com.servicecenter.app.core.Money
import com.servicecenter.app.data.repository.PaymentPart

/** Amount typed in rupees -> paise. Throws a message the screen can show. */
fun parseAmount(text: String, what: String, allowZero: Boolean = false): Long {
    val paise = Money.parseRupeesToPaise(text) ?: throw AppException.Validation("Enter $what")
    if (paise < 0 || (!allowZero && paise == 0L)) throw AppException.Validation("Enter $what")
    return paise
}

/** One text field per payment mode. Blank or zero fields are skipped, so "UPI and Cash" is just two filled fields. */
fun parsePaymentParts(amounts: Map<String, String>): List<PaymentPart> =
    amounts.mapNotNull { (modeId, text) ->
        if (text.isBlank()) return@mapNotNull null
        val paise = Money.parseRupeesToPaise(text) ?: throw AppException.Validation("Enter a valid amount")
        if (paise <= 0L) null else PaymentPart(modeId, paise)
    }

/** Paise -> text for an input box, for example 130000 -> "1300". */
fun paiseToInput(paise: Long): String =
    if (paise % 100 == 0L) (paise / 100).toString()
    else "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
