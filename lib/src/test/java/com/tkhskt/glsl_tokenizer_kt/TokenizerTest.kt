package com.tkhskt.glsl_tokenizer_kt

import com.google.common.truth.Truth
import org.junit.Test
import java.io.File
import kotlin.io.path.Path


class TokenizerTest {

    private val testResourcesPath =
        Path("src/test/java/com/tkhskt/glsl_tokenizer_kt/resources").toAbsolutePath().toString()

    private val fixture
        get() = File(testResourcesPath, "fixture.glsl").readText().removeLicenseText()
    private val fixture300es
        get() = File(testResourcesPath, "fixture-300es.glsl").readText().removeLicenseText()
    private val fixtureWindows
        get() = File(testResourcesPath, "fixture-windows.glsl").readText().removeLicenseText()
    private val invalidChars
        get() = File(testResourcesPath, "invalid-chars.glsl").readText().removeLicenseText()

    @Test
    fun token_generation_from_string() {
        val result = GlslTokenizer.tokenize(fixture)
        Truth.assertThat(result.size).isEqualTo(expectedTokens.size)
        result.forEachIndexed { index, res ->
            Truth.assertThat(res).isEqualTo(expectedTokens[index])
        }
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
                it.type == Token.Type.KEYWORD
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
        keywordResult.forEachIndexed { index, result ->
            Truth.assertThat(result).isEqualTo(keywordExpected[index])
        }
        val builtInResult =
            GlslTokenizer.tokenize(fixture300es, GlslTokenizer.Version.ES30).filter {
                it.type == Token.Type.BUILT_IN
            }.map {
                it.data
            }
        val builtInExpected = listOf("textureLod")
        builtInResult.forEachIndexed { index, result ->
            Truth.assertThat(result).isEqualTo(builtInExpected[index])
        }
    }

    @Test
    fun invalid_characters() {
        val operatorResult = GlslTokenizer.tokenize(invalidChars).filter {
            it.type == Token.Type.OPERATOR
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
                Token(column = 6, data = "3.0e-2", line = 1, position = 0, type = Token.Type.FLOAT),
                Token(column = 6, data = "(eof)", line = 1, position = 0, type = Token.Type.EOF),
            )
        )
        Truth.assertThat(GlslTokenizer.tokenize("3.0-2.0")).isEqualTo(
            listOf(
                Token(type = Token.Type.FLOAT, data = "3.0", position = 0, line = 1, column = 3),
                Token(type = Token.Type.OPERATOR, data = "-", position = 3, line = 1, column = 4),
                Token(type = Token.Type.FLOAT, data = "2.0", position = 4, line = 1, column = 7),
                Token(type = Token.Type.EOF, data = "(eof)", position = 4, line = 1, column = 7),
            )
        )
    }

    @Test
    fun uint_int_data_types() {
        Truth.assertThat(GlslTokenizer.tokenize("uint x;")).isEqualTo(
            listOf(
                Token(type = Token.Type.KEYWORD, data = "uint", position = 0, line = 1, column = 4),
                Token(
                    type = Token.Type.WHITE_SPACE,
                    data = " ",
                    position = 4,
                    line = 1,
                    column = 5
                ),
                Token(type = Token.Type.IDENTIFIER, data = "x", position = 5, line = 1, column = 6),
                Token(type = Token.Type.OPERATOR, data = ";", position = 6, line = 1, column = 6),
                Token(type = Token.Type.EOF, data = "(eof)", position = 6, line = 1, column = 7),
            )
        )
        Truth.assertThat(GlslTokenizer.tokenize("int x;")).isEqualTo(
            listOf(
                Token(type = Token.Type.KEYWORD, data = "int", position = 0, line = 1, column = 3),
                Token(
                    type = Token.Type.WHITE_SPACE,
                    data = " ",
                    position = 3,
                    line = 1,
                    column = 4
                ),
                Token(type = Token.Type.IDENTIFIER, data = "x", position = 4, line = 1, column = 5),
                Token(type = Token.Type.OPERATOR, data = ";", position = 5, line = 1, column = 5),
                Token(type = Token.Type.EOF, data = "(eof)", position = 5, line = 1, column = 6),
            )
        )
    }

    private fun String.removeLicenseText(): String {
        val lines = lines()
        return lines.toList().slice(11 until lines.size).joinToString("\n")
    }
}
