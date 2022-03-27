package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.events.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.events.EventPublisher
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/*
    This service holds all the logic about charge a payment, it ensures there is no problem with the invoice
    before calling payment provider to avoid unsuccessful calls, also has a eventPublisher that handles all
    events generated in the process.
    It is also important to note that every function here is a suspend function, this allows us to delay the calls
    to the payment provider in case of a network error and not blocking the main process.
 */

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService,
    private val eventBus: EventPublisher
) {
    private companion object {
        const val MAX_RETRIES = 5
    }

    suspend fun charge(invoice: Invoice): Invoice {
        if (invoiceIsAlreadyPaid(invoice)) return duplicatedPayment(invoice)
        val customer = findCustomer(invoice) ?: return customerNotFound(invoice)
        if (isCurrencyMismatch(customer, invoice)) return currencyMismatch(invoice)
        return chargeInvoice(invoice)
    }

    private fun duplicatedPayment(invoice: Invoice): Invoice {
        eventBus.publish(DuplicatePayment(invoiceId = invoice.id, date= LocalDateTime.now()))
        return invoice
    }

    private suspend fun chargeInvoice(invoice: Invoice): Invoice {
        var retries = 0
        while (retries < MAX_RETRIES) {
            try {
                val result = when (paymentProvider.charge(invoice)) {
                    true -> successfulPayment(invoice)
                    false -> failedPayment(invoice)
                }
                return invoiceService.update(result)
            } catch (e: CustomerNotFoundException) {
                return customerNotFound(invoice)
            } catch (e: CurrencyMismatchException) {
                return currencyMismatch(invoice)

            } catch (e: NetworkException) {
                delay(1000)
                retries++
            }
        }

        return networkError(invoice)
    }

    private fun invoiceIsAlreadyPaid(invoice: Invoice) = invoice.status == InvoiceStatus.PAID

    private fun findCustomer(invoice: Invoice): Customer? {
        return try {
            customerService.fetch(invoice.customerId)
        } catch (e: CustomerNotFoundException) {
            null
        }

    }

    private fun customerNotFound(invoice: Invoice): Invoice {
        eventBus.publish(
            CustomerNotFound(invoiceId = invoice.id, customerId = invoice.customerId, date = LocalDateTime.now())
        )

        val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
        return invoiceService.update(result)
    }


    private fun isCurrencyMismatch(
        customer: Customer,
        invoice: Invoice
    ) = customer.currency != invoice.amount.currency

    private fun currencyMismatch(invoice: Invoice): Invoice {
        eventBus.publish(
            CurrencyMismatch(
                invoiceId = invoice.id, customerId = invoice.customerId,
                currency = invoice.amount.currency, date = LocalDateTime.now()
            )
        )

        val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
        return invoiceService.update(result)
    }

    private fun networkError(invoice: Invoice): Invoice {
        eventBus.publish(NetworkError(invoiceId = invoice.id, date = LocalDateTime.now()))
        val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
        return invoiceService.update(result)
    }

    private fun updateInvoiceStatus(invoice: Invoice, status: InvoiceStatus): Invoice {
        return Invoice(invoice.id, invoice.customerId, invoice.amount, status)
    }

    private fun successfulPayment(invoice: Invoice): Invoice {
        eventBus.publish(SuccesfulPayment(invoiceId = invoice.id, amount = invoice.amount, date = LocalDateTime.now()))
        return updateInvoiceStatus(invoice, InvoiceStatus.PAID)
    }

    private fun failedPayment(invoice: Invoice): Invoice {
        eventBus.publish(FailedPayment(invoiceId = invoice.id, amount = invoice.amount, date = LocalDateTime.now()))
        return updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
    }
}
