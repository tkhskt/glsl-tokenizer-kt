package com.tkhskt.glsl_tokenizer_kt

data class GlslToken(
    val type: Type,
    val data: String,
    val position: Int,
    val line: Int,
    val column: Int,
) {

    private val _meta = mutableListOf<Meta>()
    val meta: List<Meta> get() = _meta

    fun addMetaData(metaData: Meta) {
        _meta.add(metaData)
    }

    enum class Type {
        BLOCK_COMMENT,
        LINE_COMMENT,
        PREPROCESSOR,
        OPERATOR,
        INTEGER,
        FLOAT,
        IDENTIFIER,
        BUILT_IN,
        KEYWORD,
        WHITESPACE,
        EOF;
    }

    interface Meta
}
