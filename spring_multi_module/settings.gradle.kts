plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "spring_multi_module"
include("domain")
include("adapter")
include("infrastructure")
include("application")
