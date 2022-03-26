package io.pleo.antaeus.core.schedule

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.time.LocalDateTime

private const val MONTHLY_CRON = "0 0 0 1 * ? *"
class Scheduler(
    private val invoiceService: InvoiceService,
    private val billingService: BillingService
) {

    private val scheduler = StdSchedulerFactory.getDefaultScheduler()

    private val trigger: Trigger = TriggerBuilder
        .newTrigger()
        .startNow()
        .withSchedule(CronScheduleBuilder.cronSchedule(MONTHLY_CRON))
        .build()


    private fun job(jobName: String): JobDetail {
        return JobBuilder
            .newJob()
            .ofType(InvoicePaymentJob::class.java)
            .usingJobData(JobDataMap(mapOf("invoiceService" to invoiceService, "billingService" to billingService)))
            .withIdentity(jobName)
            .build()
    }


    fun startScheduled() {
        if (scheduler.isStarted) return
        scheduler?.start()
        scheduler?.scheduleJob(
            job("monthly_scheduled_invoice_payment_job"),
            trigger
        )
    }

    fun startNow() {
        scheduler?.scheduleJob(
            job(LocalDateTime.now().toString() + "_invoice_payment_job"),
            TriggerBuilder.newTrigger().startNow().build()
        )
    }
}
