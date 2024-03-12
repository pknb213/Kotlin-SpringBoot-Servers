package com.example.spring_multi_module.board

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select

@Mapper
interface BoardMapper {
    @Select("SELECT * FROM board")
    fun selectAll(): List<BoardEntity>
}