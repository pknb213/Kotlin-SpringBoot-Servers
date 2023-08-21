package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil

object IndicatorDeviceByViewIsCrash {
	val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
	val COLLECTION_NAME_PT1H = BatchUtil.getNameOneHour(this.javaClass)
	val COLLECTION_NAME_PT24H = BatchUtil.getNameOneDay(this.javaClass)
}