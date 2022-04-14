package com.tkhskt.glsl_tokenizer_kt

data class Token(
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
        WHITE_SPACE,
        EOF;
    }

    interface Meta
}
