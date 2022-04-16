# glsl-tokenizer-kt [![](https://jitpack.io/v/com.tkhskt/glsl-tokenizer-kt.svg)](https://jitpack.io/#com.tkhskt/glsl-tokenizer-kt) ![CI](https://github.com/tkhskt/glsl-tokenizer-kt/actions/workflows/tokenizer-test.yml/badge.svg)

Kotlin implementation of [glslify/glsl-tokenizer](https://github.com/glslify/glsl-tokenizer)

## Setup

Repository is now **Jitpack**:

```gradle
repositories {
   maven { url "https://jitpack.io" }
}
```

Check the [latest-version](https://jitpack.io/#com.tkhskt/glsl-tokenizer-kt)

```gradle
implementation "com.tkhskt:glsl-tokenizer-kt:[latest-version]"
```

## Usage

Tokens can be obtained from the target GLSL string by calling `GlslTokenizer#tokenize`.

```kotlin
val glslString = "vec4(1.0,1.0,1.0,1.0)"

val tokens = GlslTokenizer.tokenize(glslString) // returns List<GlslToken>
```

The definition of a GlslToken object is as follows.

```kotlin
data class GlslToken(
    val type: Type,    // token type
    val data: String,  // token
    val position: Int, // position within the entire GLSL string
    val line: Int,     // line number
    val column: Int,   // column number
)
```

### Options

By default, the token is obtained as GLSL ES 1.0.

You can obtain tokens from a string written in GLSL ES 3.0 by passing `GlslTokenizer.Version.ES30` as the second argument of the tokenize method.

```kotlin
GlslTokenizer.tokenize(glslString, GlslTokenizer.Version.ES30)
```

### Token Type

Tokens are classified into the following types.

```kotlin
BLOCK_COMMENT
LINE_COMMENT
PREPROCESSOR
OPERATOR
INTEGER
FLOAT
IDENTIFIER
BUILT_IN
KEYWORD
WHITESPACE
EOF
```

## License

MIT
