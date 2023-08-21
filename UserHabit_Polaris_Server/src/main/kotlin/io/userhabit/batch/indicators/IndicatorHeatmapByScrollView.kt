package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil

/**
 * 스크롤 뷰 히트맵 지표
 * 키 및 인덱스
 * stz, ai, an, vhi, t
 * @author cjh
 */
object IndicatorHeatmapByScrollView {
	val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
	val COLLECTION_NAME_PT1H = BatchUtil.getNameOneHour(this.javaClass)
	val COLLECTION_NAME_PT24H = BatchUtil.getNameOneDay(this.javaClass)
}