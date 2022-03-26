package io.pleo.antaeus.models.events

interface EventPublisher {
    fun publish(event:Event)
}