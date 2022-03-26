package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus


class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
) {
    private companion object {
        const val MAX_RETRIES = 5
    }

    fun charge(invoice: Invoice): Invoice {
        if (invoiceIsAlreadyPaid(invoice)) return invoice
        val customer = findCustomer(invoice) ?: return customerNotFound(invoice)
        if (isCurrencyMismatch(customer, invoice)) return currencyMismatch(invoice)
        return payInvoice(invoice)
    }

    private fun invoiceIsAlreadyPaid(invoice: Invoice) = invoice.status == InvoiceStatus.PAID

    private fun findCustomer(invoice: Invoice): Customer? {
        return try {
            customerService.fetch(invoice.customerId)
        } catch (e : CustomerNotFoundException){
            null
        }

    }

    private fun customerNotFound(invoice: Invoice): Invoice {
        //LOG
        val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
        return invoiceService.update(result)
    }

    private fun isCurrencyMismatch(
        customer: Customer,
        invoice: Invoice
    ) = customer.currency != invoice.amount.currency

    private fun currencyMismatch(invoice: Invoice): Invoice {
        //LOG
        val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
        return invoiceService.update(result)
    }

    private fun payInvoice(invoice: Invoice): Invoice {
        var retries = 0
        for (i in 1..MAX_RETRIES) {
            try {
                val result = when (paymentProvider.charge(invoice)) {
                    true -> updateInvoiceStatus(invoice, InvoiceStatus.PAID)
                    false -> updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
                }
                return invoiceService.update(result)
            } catch (e: CustomerNotFoundException) {
                //LOG
                val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
                return invoiceService.update(result)
            } catch (e: CurrencyMismatchException) {
                //LOG
                val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
                return invoiceService.update(result)

            } catch (e: NetworkException) {
                retries++
                Thread.sleep(1000)
            }
        }

        val result = updateInvoiceStatus(invoice, InvoiceStatus.ERROR)
        return invoiceService.update(result)
    }

    private fun updateInvoiceStatus(invoice: Invoice, newStatus: InvoiceStatus): Invoice {
        return Invoice(invoice.id, invoice.customerId, invoice.amount, newStatus)
    }
}
