package io.userhabit.polaris.service


import io.userhabit.common.SafeRequest
import io.userhabit.common.Status
import io.userhabit.polaris.service.batch.BatchLaterServiceButler
import reactor.core.publisher.Mono
import reactor.util.Loggers


/**
 * use crontab to run this at night
 */
object BatchLaterService {
    private val log = Loggers.getLogger(this.javaClass)

    fun doLater(req: SafeRequest): Mono<Map<String, Any>> {

        // @see BatchUtil._getFluxAllBatches
        val batchList = listOf(
            "HeatmapByViewBatch", "PreviewByCohort",
            "ReachRateByScrollViewBatch", "AllFlowBatch"
        )

        val blButler = BatchLaterServiceButler()
        val runBatchStream = blButler.runBatches(batchList)

        return runBatchStream.map {
            log.info(it.toString())
            Status.status200Ok(it)
        }
    }
}
