package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil

object CohortPreview {
	val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
}