package org.example.flow.model

/**
 * 分类结果：单次窗口信息的判定。
 * @param mode 判定结果
 * @param matchedKeyword 命中的娱乐关键词，null 表示未命中任何关键词（即 WORK）
 */
data class ClassificationResult(
    val mode: Mode,
    val matchedKeyword: String?,
    val timestamp: Long,
)
