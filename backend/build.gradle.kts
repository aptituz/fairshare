plugins {
    id("org.springframework.boot") version "3.3.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "com.fairshare"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
    implementation("org.liquibase:liquibase-core")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("org.postgresql:postgresql:42.7.8")
    runtimeOnly("com.mysql:mysql-connector-j:9.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
