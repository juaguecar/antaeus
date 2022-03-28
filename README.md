## Run
To test my solution you can access the "/rest/v1/jobs/charge-all-pending-invoices" endpoint.

## Pleo's antaeus journey:

When I started this challenge the first step was to read all the instructions and requirements carefully and take some notes. The assigment itself gave me a lot of freedom to create and to decide how much time I would invest. I decided that between 6 -8 hours would be an appropriate amount of time .
The next step was setting up the environment and running the application. It had been a while since the last time I used docker, so I had to familiarize myself with it again. Also, versions of Kotlin JVM caused me some problems while trying to run existing tests but managed to fix everything within the first hour.

With all the environment set and the app working I looked through the code and read  the requirements again.
The app domain was payments, so I realized that the most crucial factor would be reliability so the BillingService should be the project's focal-point.
I made the BillingService with some guard clauses to avoid calling the payment provider in case of duplicated payments or currency mismatch.
That's when I decided to add a new status for the invoice, the status ERROR for the invoices that had tried to be charged but something went wrong.
When I read "charge monthly" I thought about creating a batch job. I have a lot of experience with Spring Batch, but it seemed so overkill for this project, so I decided to search for something lightweight. Then I decided to use Quartz. I did a Quartz job with a cron to charge all pending invoices on the 1st day of the month. Also, I added and endpoint to launch the jobs.
This helped me while testing and could be useful in a production environment in case something failed, and we wanted to launch the job manually or for another type of implementations avoiding the job scheduler.

Once I got my job working I though that it would process a lot of data, so we would need some coverage. Once more, we are talking about payments we have to be very careful with problems like double charges or charges that haven't been achieved. I decided that event domains could be my solution to that.
We would have a eventPublisher that everytime we process a payment it publishes an event. This event could be consumed by other service. It would be very useful for failed charges, so I decided to create an event for all the possible outcomes of the BillingService.

With the job and event done, another issue came to mind. The process of charging an invoice could be very time-consuming, and we should avoid blocking our app during this process. After some research I discovered that kotlin coroutines could be my solution. What is more, these coroutines would help with the possible Network Exception using a delay when retrying the calls to the payment provider.

After all, I added some more test-coverage and everything was done. The whole process lasted around 7 hours.

## Decisions

- I added one more status for the InvoiceStatus: ERROR for when an invoice has tried to be charged but something went wrong.

- In the case that the payment provider launched an NetworkError, I decided to retry it 5 times with a delay of 1s between calls (this decision is arbitrary and the real decision would depend on the payment provider chosen)

- The billing service holds all the logic about charging the invoices and the information about the payment provider interaction.

- The events are produced every time a payment is processed, there is an event type for every possible outcome with the information that I thought would be relevant.

- There is a mocked KafkaEventPublisher that only logs the events, I decided that the implementation was out of the scope of the challenge.

## Possible next steps

- Change to event sourcing: In a real production scenario with a million of invoices to be handled, a batch job would not be the best solution because it is not scalable. A job that generates events monthly with the invoices pending could be consumed by a lot of different machines.

- Move the services to a hexagonal architecture. This could help isolate domains and make the code easy to understand.

- More testing (especially integration testing)

- Another job to fix the invoice with status ERROR. This job would be in charge of discovering what the problem was and how to solve it with the information of the domain events.

- Improve logging and configuration about the job.



## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
