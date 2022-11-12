import org.jetbrains.kotlinx.spark.api.*
import org.apache.log4j.Level
import org.apache.log4j.Logger

fun main(args: Array<String>) {
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
    /**
    --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:log4j.properties" \
    --conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:log4j.properties" \
    --files "/absolute/path/to/your/log4j.properties" \

    --files /Users/cyj/Documents/IntelliJ/spark-3.0.3-bin-hadoop2.7/conf/log4j.properties
     */
//    Logger.getLogger("org").setLevel(Level.OFF)
//    Logger.getLogger("akka").setLevel(Level.OFF)
//    Logger.getRootLogger().setLevel(Level.OFF)
    val spark = SparkSession
        .builder()
        .config("spark.sql.codegen.wholeStage", false)
        .master("local[2]")
        .appName("Simple Application").orCreate
    withSpark {
//        spark.sparkContext().setLogLevel(SparkLogLevel.OFF)
        dsOf(1, 2, 3, 4, 5)
            .map { it to (it + 2) }
            .withCached {
                showDS()
                filter { it.first % 2 == 0 }.showDS()
            }
            .map { c(it.first, it.second, (it.first + it.second) * 2) }
            .show()
    }
//    println(MapAndListOperation().app(spark))
//    spark.sparkContext().setLogLevel("OFF")
//    spark.dsOf(mapOf(1 to c(1, 2, 3), 2 to c(1, 2, 3)), mapOf(3 to c(1, 2, 3), 4 to c(1, 2, 3)))
//        .flatMap { it.toList().map { p -> listOf(p.first, p.second._1, p.second._2, p.second._3) }.iterator() }
//        .flatten()
//        .map { c(it) }
//        .also { it.printSchema() }
//        .distinct()
//        .sort("_1")
////        .debugCodegen()
//        .show()
}