/*
import kotlin.api.*

class MapAndListOperation {

    fun app(spark: SparkSession){
        return withSpark() {
            spark.dsOf(mapOf(1 to c(1, 2, 3), 2 to c(1, 2, 3)), mapOf(3 to c(1, 2, 3), 4 to c(1, 2, 3)))
                .flatMap { it.toList().map { p -> listOf(p.first, p.second._1, p.second._2, p.second._3) }.iterator() }
                .flatten()
                .map { c(it) }
                .also { it.printSchema() }
                .distinct()
                .sort("_1")
                .debugCodegen()
                .show()
        }
    }
}*/
