package io.pleo.antaeus.core.events

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.events.Event
import java.time.LocalDateTime
import java.util.*

class CurrencyMismatch(
    val eventId: UUID = UUID.randomUUID(),
    val customerId: Int,
    val invoiceId: Int,
    val currency: Currency,
    val date: LocalDateTime
) : Event