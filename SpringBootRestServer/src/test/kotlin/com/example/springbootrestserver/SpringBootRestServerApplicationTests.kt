package com.example.springbootrestserver

import com.example.springbootrestserver.domain.user.dto.UserDto
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SpringBootRestServerApplicationTests {

    @Test
    fun contextLoads() {
    }


}
class CalDescribeSpec : DescribeSpec({
    val stub = UserDto("User1")

    describe("calculate") {
        context("식이 주어지면") {
            it("해당 식에 대한 결과 값이 반환 된다") {

//                calculations.forAll { (expression, data) ->
//                    val result = stub.calculate(expression)
//
//                    result shouldBe data
//                }
            }
        }
    }
})