plugins {
    kotlin("jvm")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()

    systemProperty(
        "measurement.contract.path",
        rootProject.projectDir.resolve("../contracts/measurement-mean-cases.csv").normalize().path,
    )
}
