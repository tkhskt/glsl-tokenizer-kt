package com.tkhskt.glsl_tokenizer_kt

data class GlslToken(
    val type: Type,
    val data: String,
    val position: Int,
    val line: Int,
    val column: Int,
) {

    val meta = listOf<Meta>()

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
