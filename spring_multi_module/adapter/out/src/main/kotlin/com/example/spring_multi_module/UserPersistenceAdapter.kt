package com.example.spring_multi_module

import com.example.spring_multi_module.entitys.user.User
import com.example.spring_multi_module.entitys.user.UserRole
import com.example.spring_multi_module.ports.out.CreateUserPort
import com.example.spring_multi_module.ports.out.DeleteUserPort
import com.example.spring_multi_module.ports.out.GetAllUserPort
import com.example.spring_multi_module.ports.out.PutUserPort
import org.springframework.stereotype.Component

/**
 * Outgoing adapter는 application-core에서 선언한 outgoing port 인터페이스를 구현하는 클래스이다.
 * Usecase 별로 존재하는 복수개의 outgoing port가 1개의 persistence adapter에 대응되는 것이 보통이다.
 * DDD aggregate 하나 당 persistence adapter를 한 개 씩 생성한다.
 *
 * 생성자 파라미터는 Spring JPA repository 또는 개인적으로 선언한 인터페이스가 될 수 있다.
 * JpaRepository를 상속한 인퍼페이스인 경우 Spring이 자동으로 구현체를 주입해준다.
 */
@Component
class UserPersistenceAdapter(
    private val userRepository: IUserRepository
): CreateUserPort, GetAllUserPort, PutUserPort, DeleteUserPort {
    /**
     * JPA 입장에서는 Jpa Entity가 입력 모델이다.
     * Usecase -> Persistence로 요청할 때는 별도의 command 객체를 생성하지 않고
     * Account에서 필요한 데이터만 추출해서 파라미터로 넘기거나 Account 엔터티 객체를 통째로 넘긴다.
     */
    override fun create(user: User): User {
        /**
         * Persistence --> Usecase로의 출력 모델도 별도의 출력 모델을 정의하지 않고
         * Account 엔터티 모델을 그대로 사용한다.
         * 즉, Web <-> Usecase 간에 완전 매핑 전략을, Usecase <-> Persistence 간에는 two-way 매핑 전략을 사용한다.
         */
        return userRepository.save(
            UserEntity.fromDomain(user)
        ).toDomain()
    }
    override fun getAll(): List<User> {
//        println("Get PersistenceAdapter")
        return userRepository.findAll().map {
            it.toDomain()
        }
    }

    override fun put(
        email: String,
        password: String,
        name: String,
        roleId: UserRole
    ): Boolean {
        try {
            val user = userRepository.save(
                UserEntity(
                    email = email,
                    password = password,
                    name = name,
                    roleId = roleId
                )
            )
            println("Put User => ${user}")
            return user.id != null
        } catch (e: Exception) {
            return false
        }
    }

    override fun deleteUser(id: Long): Boolean {
        try {
            userRepository.deleteById(id)
            println("Delete User => ${id}")
            return true
        }catch (e: Exception) {
            return false
        }
    }
}