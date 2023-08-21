package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil

/**
 * 스크롤뷰 도달율 지표
 * @author cjh
 */
object IndicatorReachRateByScrollView {
	val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
	val COLLECTION_NAME_PT1H = BatchUtil.getNameOneHour(this.javaClass)
	val COLLECTION_NAME_PT24H = BatchUtil.getNameOneDay(this.javaClass)
}