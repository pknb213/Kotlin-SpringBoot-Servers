import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.84.Final:osx-aarch_64") // Mac M1 Exception
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc") // r2dbc
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2") // h2
    runtimeOnly("io.r2dbc:r2dbc-h2") // h2'
    runtimeOnly("org.postgresql:postgresql") // postgresql
//    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("com.github.jasync-sql:jasync-r2dbc-mysql:2.1.23") // mysql (miku는 2020 이후 업데이트가 없음, spring3.0 이상에서 안되는 듯)
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.kotest:kotest-runner-junit5:5.4.2") // kotlin junit 처럼 쓸 수 있는 Spec 들이 정의 됨
    testImplementation("io.kotest:kotest-assertions-core:5.4.2") // shouldBe... etc 와같이 Assertions 의 기능을 제공
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2") // spring boot test 를 위해서 추가
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
