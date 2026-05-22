package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 书源数据模型
 * 
 * 对应后端 BookSource 实体
 */
@Serializable
data class BookSource(
    val id: Int = 0,
    val name: String,
    val path: String,
    val enabled: Boolean
)
