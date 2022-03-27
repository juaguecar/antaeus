package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.events.EventPublisher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val USER_ID = 1
private const val NON_EXISTING_USER_ID = 0

class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()
    private val customerService = mockk<CustomerService> {
        every { fetch(USER_ID) } returns Customer(USER_ID, Currency.EUR)
        every { fetch(NON_EXISTING_USER_ID) } throws CustomerNotFoundException(NON_EXISTING_USER_ID)
    }
    private val invoiceService = mockk<InvoiceService>()

    private val eventBus = mockk<EventPublisher>(relaxed = true)

    private val billingService = BillingService(paymentProvider, customerService, invoiceService, eventBus)

    @Test
    fun `If user exists and payment is successful we should update the invoice with status PAID`() = runBlocking {
        every { paymentProvider.charge(any()) } returns true
        every { customerService.fetch(any()) } returns Customer(1, Currency.EUR)

        val expected =
            Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PAID)

        every { invoiceService.update(expected) } returns expected

        val pendingInvoice =
            Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PENDING)

        assertEquals(expected, billingService.charge(pendingInvoice))


        verify { invoiceService.update(expected) }
    }

    @Test
    fun `If invoice is already PAID we return same invoice and dont interact with any other service`() = runBlocking {
        val expected =
            Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PAID)

        every { invoiceService.update(expected) } returns expected

        val pendingInvoice =
            Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PAID)

        assertEquals(expected, billingService.charge(pendingInvoice))

        verify(exactly = 0) { paymentProvider.charge(any()) }
        verify(exactly = 0) { customerService.fetch(any()) }
        verify(exactly = 0) { invoiceService.update(any()) }
    }

    @Test
    fun `If user exists but payment is not successful we should update the invoice with status ERROR`() = runBlocking {
        every { paymentProvider.charge(any()) } returns false

        val expected =
            Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.ERROR)

        every { invoiceService.update(expected) } returns expected

        val pendingInvoice =
            Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PENDING)

        assertEquals(expected, billingService.charge(pendingInvoice))


        verify { invoiceService.update(expected) }
    }

    @Test
    fun `If user don't exists we should update the invoice with status ERROR and dont call payment provider`() =
        runBlocking {
            val expected =
                Invoice(1, 0, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.ERROR)

            every { invoiceService.update(expected) } returns expected

            val pendingInvoice =
                Invoice(1, 0, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PENDING)

            assertEquals(expected, billingService.charge(pendingInvoice))


            verify { invoiceService.update(expected) }
            verify(exactly = 0) { paymentProvider.charge(any()) }
        }

    @Test
    fun `If user currency don't match invoice currency we should update the invoice with status ERROR and dont call payment provider`() =
        runBlocking {
            every { customerService.fetch(any()) } returns Customer(USER_ID, Currency.GBP)

            val expected =
                Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.ERROR)

            every { invoiceService.update(expected) } returns expected

            val pendingInvoice =
                Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PENDING)

            assertEquals(expected, billingService.charge(pendingInvoice))


            verify { invoiceService.update(expected) }
            verify(exactly = 0) { paymentProvider.charge(any()) }
        }

    @Test
    fun `If PaymentProvider throws NetworkException should retry 5 times and if all fails update with ERROR status`() =
        runBlocking {
            every { paymentProvider.charge(any()) } throws NetworkException()

            val expected =
                Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.ERROR)

            every { invoiceService.update(expected) } returns expected

            val pendingInvoice =
                Invoice(1, 1, Money(BigDecimal.valueOf(10000000), Currency.EUR), InvoiceStatus.PENDING)

            assertEquals(expected, billingService.charge(pendingInvoice))


            verify(exactly = 5) { paymentProvider.charge(any()) }
        }
}