plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
}