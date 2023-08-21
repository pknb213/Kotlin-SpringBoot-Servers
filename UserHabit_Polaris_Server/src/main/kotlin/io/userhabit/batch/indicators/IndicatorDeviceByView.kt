package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil


/**
 * 디바이스 지표
 * 키 및 인덱스
 * stz, ai, an, ek, di(10분까지만 유지)
 * 내용 : 10분 배치 시 append 하지 않음. 이미 저장 된 값에서 st, stz 만 변경
 * @author cjh
 */
object IndicatorDeviceByView {
  val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
  val COLLECTION_NAME_PT1H = BatchUtil.getNameOneHour(this.javaClass)
  val COLLECTION_NAME_PT24H = BatchUtil.getNameOneDay(this.javaClass)
}