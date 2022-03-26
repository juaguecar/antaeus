package io.pleo.antaeus.core.schedule

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

class InvoicePaymentJob(var billingService: BillingService? = null,
                        var invoiceService: InvoiceService? = null) : Job {

    private val logger = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        if (billingService == null || invoiceService == null) return
        logger.info { "Job starting:${context.toString()}" }

        val pendingInvoices = invoiceService!!.fetchAllPendingInvoices()

        runBlocking {
            pendingInvoices.forEach{
                launch { chargePayment(it) }
            }
        }

        logger.info { "Job finished" }
    }

    private fun chargePayment(invoice : Invoice){
        billingService!!.charge(invoice)
    }
}