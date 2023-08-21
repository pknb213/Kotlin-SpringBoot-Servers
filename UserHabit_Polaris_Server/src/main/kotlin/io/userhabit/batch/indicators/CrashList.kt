package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil

object CrashList {
	val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
}