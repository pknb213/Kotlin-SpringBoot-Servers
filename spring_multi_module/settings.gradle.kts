plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "spring_multi_module"
include("infrastructure")
include("adapter")
include("core")
include("domain")
include("adapter:out")
include("adapter:in")
//findProject(":adapter:in")?.name = "in"
//findProject(":adapter:out")?.name = "out"
