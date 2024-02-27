description = "Infrastructure module"

allOpen {

}

noArg {
    // @Entity
//    annotation("javax.persistence.Entity")
}

dependencies {
    // Springboot
//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation("org.springframework.boot:spring-boot-starter-aop")
//    implementation("org.springframework.boot:spring-boot-starter-web")
//    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // Format Dependency
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Test Fixtures
//    testImplementation(testFixtures(project(":domain")))
//    testImplementation(testFixtures(project(":adapter:out")))
}

//dependencyManagement {
//    imports {
//        mavenBom()
//    }
//}

//tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
//    enabled = true
//}

//tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
//    enabled = true
//}

/**
 * plugins {
 *     kotlin("kapt") //querydsl
 * }
 *
 * val querydslVersion = "5.0.0"
 *
 * allOpen {
 *     annotation("javax.persistence.Entity")
 *     annotation("javax.persistence.Embeddable")
 *     annotation("javax.persistence.MappedSuperclass")
 * }
 *
 * noArg {
 *     annotation("javax.persistence.Entity") // @Entity가 붙은 클래스에 한해서만 no arg 플러그인을 적용
 *     annotation("javax.persistence.Embeddable")
 *     annotation("javax.persistence.MappedSuperclass")
 * }
 *
 * dependencies {
 *     //querydsl
 *     implementation("com.querydsl:querydsl-jpa:$querydslVersion")
 *     kapt("com.querydsl:querydsl-apt:$querydslVersion:jpa")
 *     kapt("org.springframework.boot:spring-boot-configuration-processor")
 *
 *     // jdbc
 *     implementation("org.mariadb.jdbc:mariadb-java-client") // AWS Secrets Manager JDBC 는 Wrapper 이기 때문에, 별도로 DB에 맞는 Driver 의존성을 추가해야한다.
 *     implementation("mysql:mysql-connector-java")
 *     implementation("org.springframework.boot:spring-boot-starter-thymeleaf:2.7.5")
 *     implementation("org.apache.commons:commons-lang3:3.12.0")
 * }
 *
 * //querydsl
 * sourceSets["main"].withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
 *     kotlin.srcDir("$buildDir/generated/source/kapt/main")
 * }
 */