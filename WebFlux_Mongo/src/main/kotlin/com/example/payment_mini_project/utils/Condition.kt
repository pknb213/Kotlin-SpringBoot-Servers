package com.example.payment_mini_project.utils

object Condition {
    private val operatorMap = mapOf(
        "equals" to "\$eq",
        "notequals" to "\$ne",
        "lessThan" to "\$lt",
        "greaterThan" to "\$gt",
        "in" to "\$in",
        "notin" to "\$nin",
        "between" to "\$range"
    )

    private val enToKrMap = mapOf(
        "CARD" to "카드",
        "SEND" to "송금",
        "BOOK" to "도서",
        "FOOD" to "식품",
        "BEAUTY" to "뷰티",
        "SPORT" to "스포츠",
        "FASHION" to "패션",
        "lessThan" to "미만",
        "greaterThan" to "초과",
        "equals" to "같은",
        "notequals" to "아닌"
    )

    fun convertOperation(op: String): String =
        operatorMap[op] ?: op

    fun convertKr(str: String): String =
        enToKrMap[str] ?: str

    fun createDescription(key: String, op: String? = null, value: Any): String {
        val keyMap = mapOf(
            "paymentId" to String.format("%s 의 결제", value),
            "accountId" to String.format("%s 결제자의 결제", value),
            "amount" to String.format("%s원 %s의 결제", value, op),
            "methodType" to String.format("%s 결제", value),
            "region" to String.format("%s 지역에서의 결제, value")
        )
        return keyMap[key] ?: "Description 생성 실패"
    }
}