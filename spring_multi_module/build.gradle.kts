import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22" apply false
    kotlin("plugin.jpa") version "1.9.22" apply false
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring") // all-open
    apply(plugin = "kotlin-jpa")

    dependencies {
        // Springboot
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-security")
        developmentOnly("org.springframework.boot:spring-boot-devtools")

        // Kotlin
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation(kotlin("stdlib"))

        // Lombook
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // Jwt
        implementation("io.jsonwebtoken:jjwt-api:0.11.2")

        // DB connect
//        implementation("org.mariadb.jdbc:mariadb-java-client") // AWS Secrets Manager JDBC 는 Wrapper 이기 때문에, 별도로 DB에 맞는 Driver 의존성을 추가해야한다.
        runtimeOnly("com.mysql:mysql-connector-j")
//        runtimeOnly 'com.mysql:mysql-connector-java  8.0.31 이하버전
        runtimeOnly("com.h2database:h2")
        implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.2")

        // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
        implementation("org.apache.commons:commons-lang3:3.12.0")

        // Test
        implementation("org.springframework.boot:spring-boot-starter-jdbc")
        testImplementation("io.mockk:mockk:1.12.0")
        testImplementation("com.ninja-squad:springmockk:2.0.3")
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            setExceptionFormat("full")
        }
    }

    tasks.test {
        useJUnitPlatform() {
            includeTags("unitTest")
            excludeTags("integrationTest")
        }
    }

    task<Test>("integration") {
        useJUnitPlatform() {
            excludeTags("unitTest")
            includeTags("integrationTest")
        }
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }
}

// application <- domain dependency
project(":core") {
    dependencies {
        implementation(project(":domain"))
        implementation(project(":common"))
    }
}

project(":domain") {
    dependencies {
    }
}

project(":adapter:in") {
    dependencies {
        implementation(project(":domain"))
        implementation(project(":core"))
        implementation(project(":common"))
//        testImplementation(testFixtures(project(":domain")))
    }
}
project(":adapter:out") {
    dependencies {
        implementation(project(":domain"))
        implementation(project(":core"))
        implementation(project(":common"))
    }
}

project(":infrastructure") {
    dependencies {
        implementation(project(":adapter:in"))
        implementation(project(":adapter:out"))
        implementation(project(":core"))
        implementation(project(":domain"))
        implementation(project(":common"))
//        testImplementation(testFixtures(project(":domain")))
//        testImplementation(testFixtures(project(":adapters:out")))
    }
}


// domain 설정
project(":domain") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = false
    jar.enabled = true
}

project(":adapter:in") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = false
    jar.enabled = true
}

project(":adapter:out") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = false
    jar.enabled = true
}

project(":core") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = false
    jar.enabled = true
}

project(":common") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = false
    jar.enabled = true
}


// Todo: 개별 build.gradle.kts에 옮김
project(":infrastructure") {
    val jar: Jar by tasks
    val bootJar: BootJar by tasks

    bootJar.enabled = true
    jar.enabled = false
}
