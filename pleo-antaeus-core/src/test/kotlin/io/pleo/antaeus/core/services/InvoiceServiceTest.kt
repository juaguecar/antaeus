package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

private const val INVOICE_ID = 1

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoice(INVOICE_ID)} returns Invoice(INVOICE_ID, 1,
            Money(BigDecimal.valueOf(10000), Currency.EUR), InvoiceStatus.PENDING)
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `fetch will return a complete invoice`() {
        val expected = Invoice(INVOICE_ID, 1,
            Money(BigDecimal.valueOf(10000), Currency.EUR), InvoiceStatus.PENDING)
        assertEquals(expected, invoiceService.fetch(INVOICE_ID))
    }

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `If we try to update an non existing invoice it will throw`() {
        assertThrows<InvoiceNotFoundException> {
            val invoice = Invoice(404, INVOICE_ID, Money(BigDecimal.valueOf(100), Currency.GBP), InvoiceStatus.PENDING)
            every { dal.updateInvoice(any()) } returns null
            invoiceService.update(invoice)
        }
    }
}
