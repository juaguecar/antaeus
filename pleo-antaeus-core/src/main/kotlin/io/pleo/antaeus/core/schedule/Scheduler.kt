package io.pleo.antaeus.core.schedule

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

class Scheduler(
    invoiceService: InvoiceService,
    billingService: BillingService) {

    private val scheduler = StdSchedulerFactory.getDefaultScheduler()

    private val trigger: Trigger = TriggerBuilder
        .newTrigger()
        .startNow()
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 * ? *"))
        .build()

    private val job: JobDetail = JobBuilder
        .newJob()
        .ofType(InvoicePaymentJob::class.java)
        .usingJobData(JobDataMap(mapOf("invoiceService" to invoiceService, "billingService" to billingService)))
        .build()


    fun start() {
        scheduler?.start()
        scheduler?.scheduleJob(
            job,
            trigger
        )
    }
}
