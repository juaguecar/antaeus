package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val CUSTOMER_ID = 1
private const val NON_EXISTENT_CUSTOMER_ID = 404


class CustomerServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(NON_EXISTENT_CUSTOMER_ID) } returns null
        every { fetchCustomer(CUSTOMER_ID)} returns Customer(CUSTOMER_ID, Currency.EUR)
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `fetch customers returns complete customer`() {
        val expectedCustomer = Customer(CUSTOMER_ID, Currency.EUR)
        assertEquals(expectedCustomer, customerService.fetch(CUSTOMER_ID))
    }

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(NON_EXISTENT_CUSTOMER_ID)
        }
    }
}
