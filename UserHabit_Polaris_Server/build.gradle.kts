plugins {
	kotlin("jvm") version "1.6.0"
	application
}

group = "io.userhabit.polaris"
<<<<<<< HEAD
version = "v1.1.202205091000-SNAPSHOT"
=======
version = "v1.1.202206090900-SNAPSHOT"
>>>>>>> master

repositories {
	mavenCentral()
//    jcenter()
}

// TODO 운영에 배포하기 전 버전을 확정한다
dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation("io.projectreactor.netty" , "reactor-netty" , "1.+")
//	implementation("org.projectlombok" , "lombok" , "1.18.12")
	implementation("ch.qos.logback" , "logback-classic" , "1.+")
//	implementation("org.slf4j" , "slf4j-api" , "1.7.30")
//	implementation("org.apache.logging.log4j" , "log4j-api" , "2.13.0")
//	implementation("org.apache.logging.log4j" , "log4j-slf4j-impl" , "2.13.0")
//	implementation("org.apache.logging.log4j" , "log4j-core" , "2.13.0")
	implementation("org.mongodb" , "mongodb-driver-reactivestreams" , "4.5.1")
//	implementation("com.fasterxml.jackson.core" , "jackson-databind" , "2.+")
	implementation("com.google.code.gson" , "gson" , "2.+")
	implementation(kotlin("reflect"))

//	implementation("com.maxmind.geoip2" , "geoip2" , "2.15.0")

	// TODO delete
//	implementation("io.jsonwebtoken" , "jjwt-api" , "0.11.2")
//	runtimeOnly("io.jsonwebtoken" , "jjwt-impl" , "0.11.2")
//	runtimeOnly("io.jsonwebtoken" , "jjwt-jackson" , "0.11.2")
//	implementation("com.google.api-client" , "google-api-client" , "1.30.10")
//	implementation("com.google.oauth-client", "google-oauth-client-jetty", "1.23.0")
//	implementation("com.google.apis", "google-api-services-gmail", "v1-rev83-1.23.0")
//	implementation("com.google.auth" ,"google-auth-library-oauth2-http" ,"0.21.1")

	implementation("com.auth0" , "java-jwt" , "3.10.3")

//	implementation("" , "" , "")

}

tasks {
	compileKotlin {
		kotlinOptions.jvmTarget = "11"
	}
	compileTestKotlin {
		kotlinOptions.jvmTarget = "11"
	}
}

application {
	mainClassName = "io.userhabit.Server"
//    mainClass.set("org.gradle.sample.Main")

	val taskNames = gradle.startParameter.taskNames

	if( taskNames.contains("dev") ){
		applicationDefaultJvmArgs = project.ext.properties.map { "-D${it.key}=${it.value}" } + "-Denv=dev"
	}else if( taskNames.contains("stage") ){
		applicationDefaultJvmArgs = project.ext.properties.map { "-D${it.key}=${it.value}" } + "-Denv=stage"
	}else if( taskNames.contains("prod") ){
		applicationDefaultJvmArgs = project.ext.properties.map { "-D${it.key}=${it.value}" } + "-Denv=prod"
	}else if( taskNames.contains("benchmark") ){
		applicationDefaultJvmArgs = project.ext.properties.map { "-D${it.key}=${it.value}" }
		mainClassName = "io.userhabit.test.Benchmark"
	}else if( taskNames.contains("sdk") ){
		applicationDefaultJvmArgs = project.ext.properties.map { "-D${it.key}=${it.value}" }
		mainClassName = "io.userhabit.test.SDKTest"
	}else if( taskNames.contains("batch") ){
		applicationDefaultJvmArgs = project.ext.properties.map { "-D${it.key}=${it.value}" }
		mainClassName = "io.userhabit.batch.BatchUtil"
	}
//    println(System.getProperties().map { it.toString().plus("\n") }.sorted())
}

task("dev"){
	dependsOn("run")
}

task("stage"){
	dependsOn("run")
}

task("prod"){
	dependsOn("run")
}

task("benchmark"){
	dependsOn("run")
}

task("sdk"){
	dependsOn("run")
}

task("batch"){
	dependsOn("run")
}

val createDocs by tasks.registering {
//	val docs = layout.buildDirectory.dir("resources/main")
//	outputs.dir(docs)
//	doLast {
//		docs.get().asFile.mkdirs()
//		docs.get().file("readme.txt").asFile.writeText("Read me!")
//	}

	val docs = layout.buildDirectory.dir("console")
	outputs.dir(docs)
	doLast {
		docs.get().asFile.mkdirs()
		docs.get().file("index.html").asFile.writeText("Hello UserHabit")
	}
}

distributions {
	main {
		contents {
			from(createDocs) {
				into("console")
			}
		}
	}
}

