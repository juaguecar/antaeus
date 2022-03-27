/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

/*
    Here I added a fetchAllPendingInvoices function that brings all the invoices that has the status PENDING,
    this service should encapsulate all the possible logic to obtain them.
 */

class InvoiceService(private val dal: AntaeusDal) {

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetchAllPendingInvoices(): List<Invoice> {
        return dal.fetchInvoiceByStatus(InvoiceStatus.PENDING)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun update(invoice: Invoice): Invoice {
        return dal.updateInvoice(invoice) ?: throw InvoiceNotFoundException(invoice.id)
    }
}
