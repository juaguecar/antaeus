package io.pleo.antaeus.core.events.kafka

import io.pleo.antaeus.models.events.Event
import io.pleo.antaeus.models.events.EventPublisher

/*
    This only wants to show one possible implementation of the EventBus interface, in this case one possible solution
    would be publish events on a kafka topic, but we could either store it in database, use RabbitMQ...
 */
class KafkaEventPublisher : EventPublisher {
    override fun publish(event: Event) {
        println(event.toString())
    }
}