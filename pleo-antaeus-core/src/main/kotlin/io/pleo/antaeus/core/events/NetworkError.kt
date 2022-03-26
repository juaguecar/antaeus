package io.pleo.antaeus.core.events

import io.pleo.antaeus.models.events.Event
import java.time.LocalDateTime
import java.util.*

class NetworkError(
    val eventId: UUID = UUID.randomUUID(),
    val invoiceId: Int,
    val date: LocalDateTime
) : Event