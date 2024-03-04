package com.example.spring_multi_module.domain.entitys.user

import com.example.spring_multi_module.domain.entitys.common.CommonEntity
import jakarta.persistence.*
import java.time.ZonedDateTime

/**
 * 헥사고날 아키텍처의 중심을 application-core라고 하며 가장 안쪽의 domain 역영과 바로 바깥의 application 영역을 합쳐 부르는 말이다.
 * DDD에 따라 domain 영역은 domain entity와 value object들로 구성된다.
 * 이러한 객체들이 같은 비지니스 그룹으로 묶일 때 root 객체를 aggregate root entity라고 부른다.
 * 비지니스 로직을 작성할 때는 aggregate root를 가져와서 읽고 수정한 다음, aggregate 단위 통째로 DB에 저장하여야 한다.
 * 비지니스를 우선으로 하는 개발을 위해서는 가장 안쪽에 있는 domain 객체들을 가장 먼저 개발하고 바깥쪽으로 이동하여야 한다.
 *
 * Account처럼 ID를 가지고 객체 생명주기가 있는 도메인 객체를 도메인 엔터티라고 부른다.
 */
/**
 * Money와 같이 ID나 생명주기가 없는 도메인 객체를 Value Object라고 부른다.
 * 블록체인에 비유하자면 domain entity는 NFT이고, value object는 교환가능한 fungible token이다.
 */
//data class Money(val amount: Long, val currency: Currency = Currency.KRW) {
//    companion object {
//        val ZERO = Money(0L)
//    }
//}
//
//operator fun Money.plus(money: Money) = Money(amount + money.amount)
//operator fun Money.minus(money: Money) = Money(amount - money.amount)

//@Table(name = "users")
//@Entity
//class UserEntity(
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0L,
//    @Column(name = "email") val email: String,
//    @Column(name = "password") val password: String,
//    @Column(name = "name") val name: String,
//    @Column(name = "roleId") val roleId: Long
//): CommonEntity() {
//}

data class User (
    val id: Long,
    val email: String,
    val password: String,
    val name: String,
    val roleId: UserRole,
):CommonEntity() {

}