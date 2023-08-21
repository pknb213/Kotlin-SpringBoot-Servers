package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil

/**
 * 전체 사용 흐름 지표
 * 키 및 인덱스
 * stz, ai, an, flow
 * @author cjh
 */
object IndicatorByFlow {
    val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
    val COLLECTION_NAME_PT1H = BatchUtil.getNameOneHour(this.javaClass)
    val COLLECTION_NAME_PT24H = BatchUtil.getNameOneDay(this.javaClass)
}