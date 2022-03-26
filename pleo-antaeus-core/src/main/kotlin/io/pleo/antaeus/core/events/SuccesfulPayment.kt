package io.pleo.antaeus.core.events

import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.events.Event
import java.time.LocalDateTime
import java.util.*

class SuccesfulPayment(
    val eventId: UUID = UUID.randomUUID(),
    val invoiceId: Int,
    val amount: Money,
    val date: LocalDateTime
) : Event