package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 标签数据模型
 */
@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val createdAt: String? = null
)
