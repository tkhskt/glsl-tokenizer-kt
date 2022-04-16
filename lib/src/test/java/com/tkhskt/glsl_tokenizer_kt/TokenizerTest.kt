package com.tkhskt.glsl_tokenizer_kt

import com.google.common.truth.Truth
import org.junit.Test
import java.io.File
import kotlin.io.path.Path


internal class TokenizerTest {

    private val testResourcesPath =
        Path("src/test/java/com/tkhskt/glsl_tokenizer_kt/resources").toAbsolutePath().toString()

    private val fixture
        get() = File(testResourcesPath, "fixture.glsl").readText().excludeLicenseText()
    private val fixture300es
        get() = File(testResourcesPath, "fixture-300es.glsl").readText().excludeLicenseText()
    private val fixtureWindows
        get() = File(testResourcesPath, "fixture-windows.glsl").readText().excludeLicenseText()
    private val invalidChars
        get() = File(testResourcesPath, "invalid-chars.glsl").readText().excludeLicenseText()
    private val blockComment
        get() = File(testResourcesPath, "block-comment.glsl").readText()

    @Test
    fun token_generation_from_string() {
        val result = GlslTokenizer.tokenize(fixture)
        Truth.assertThat(result.size).isEqualTo(expectedTokens.size)
        Truth.assertThat(result).isEqualTo(expectedTokens)
    }

    @Test
    fun windows_carriage_returns() {
        val result = GlslTokenizer.tokenize(fixtureWindows)
        Truth.assertThat(result.size).isAtLeast(1)
        Truth.assertThat(result[0].data).isEqualTo("#define PHYSICAL")
    }

    @Test
    fun token_generation_version_300es() {
        val keywordResult =
            GlslTokenizer.tokenize(fixture300es, GlslTokenizer.Version.ES30).filter {
                it.type == GlslToken.Type.KEYWORD
            }.map {
                it.data
            }
        val keywordExpected = listOf(
            "out",
            "vec4",
            "in",
            "vec2",
            "uniform",
            "usampler2DArray",
            "void",
            "vec4",
            "vec4",
        )
        Truth.assertThat(keywordResult).isEqualTo(keywordExpected)
        val builtInResult =
            GlslTokenizer.tokenize(fixture300es, GlslTokenizer.Version.ES30).filter {
                it.type == GlslToken.Type.BUILT_IN
            }.map {
                it.data
            }
        val builtInExpected = listOf("textureLod")
        Truth.assertThat(builtInResult).isEqualTo(builtInExpected)
    }

    @Test
    fun invalid_characters() {
        val operatorResult = GlslTokenizer.tokenize(invalidChars).filter {
            it.type == GlslToken.Type.OPERATOR
        }.joinToString("") {
            it.data
        }
        val expected = "@{(){();}(){''}}=();.();(){=();}"
        Truth.assertThat(operatorResult).isEqualTo(expected)
    }

    @Test
    fun floats_should_recognize_negative_exp() {
        Truth.assertThat(GlslTokenizer.tokenize("3.0e-2")).isEqualTo(
            listOf(
                GlslToken(
                    column = 6,
                    data = "3.0e-2",
                    line = 1,
                    position = 0,
                    type = GlslToken.Type.FLOAT
                ),
                GlslToken(
                    column = 6,
                    data = "(eof)",
                    line = 1,
                    position = 0,
                    type = GlslToken.Type.EOF
                ),
            )
        )
        Truth.assertThat(GlslTokenizer.tokenize("3.0-2.0")).isEqualTo(
            listOf(
                GlslToken(
                    type = GlslToken.Type.FLOAT,
                    data = "3.0",
                    position = 0,
                    line = 1,
                    column = 3
                ),
                GlslToken(
                    type = GlslToken.Type.OPERATOR,
                    data = "-",
                    position = 3,
                    line = 1,
                    column = 4
                ),
                GlslToken(
                    type = GlslToken.Type.FLOAT,
                    data = "2.0",
                    position = 4,
                    line = 1,
                    column = 7
                ),
                GlslToken(
                    type = GlslToken.Type.EOF,
                    data = "(eof)",
                    position = 4,
                    line = 1,
                    column = 7
                ),
            )
        )
    }

    @Test
    fun uint_int_data_types() {
        Truth.assertThat(GlslTokenizer.tokenize("uint x;")).isEqualTo(
            listOf(
                GlslToken(
                    type = GlslToken.Type.KEYWORD,
                    data = "uint",
                    position = 0,
                    line = 1,
                    column = 4
                ),
                GlslToken(
                    type = GlslToken.Type.WHITESPACE,
                    data = " ",
                    position = 4,
                    line = 1,
                    column = 5
                ),
                GlslToken(
                    type = GlslToken.Type.IDENTIFIER,
                    data = "x",
                    position = 5,
                    line = 1,
                    column = 6
                ),
                GlslToken(
                    type = GlslToken.Type.OPERATOR,
                    data = ";",
                    position = 6,
                    line = 1,
                    column = 6
                ),
                GlslToken(
                    type = GlslToken.Type.EOF,
                    data = "(eof)",
                    position = 6,
                    line = 1,
                    column = 7
                ),
            )
        )
        Truth.assertThat(GlslTokenizer.tokenize("int x;")).isEqualTo(
            listOf(
                GlslToken(
                    type = GlslToken.Type.KEYWORD,
                    data = "int",
                    position = 0,
                    line = 1,
                    column = 3
                ),
                GlslToken(
                    type = GlslToken.Type.WHITESPACE,
                    data = " ",
                    position = 3,
                    line = 1,
                    column = 4
                ),
                GlslToken(
                    type = GlslToken.Type.IDENTIFIER,
                    data = "x",
                    position = 4,
                    line = 1,
                    column = 5
                ),
                GlslToken(
                    type = GlslToken.Type.OPERATOR,
                    data = ";",
                    position = 5,
                    line = 1,
                    column = 5
                ),
                GlslToken(
                    type = GlslToken.Type.EOF,
                    data = "(eof)",
                    position = 5,
                    line = 1,
                    column = 6
                ),
            )
        )
    }

    @Test
    fun block_comment() {
        val result = GlslTokenizer.tokenize(blockComment)
        Truth.assertThat(result).isEqualTo(
            listOf(
                GlslToken(
                    type = GlslToken.Type.BLOCK_COMMENT,
                    data = "/*\nblock comment1\nblock comment2\n*/",
                    position = 0,
                    line = 4,
                    column = 1
                ),
                GlslToken(type= GlslToken.Type.EOF, data="(eof)", position=0, line=4, column=2),
            )
        )
    }

    private fun String.excludeLicenseText(): String {
        val lines = lines()
        return lines.toList().slice(11 until lines.size).joinToString("\n")
    }
}
