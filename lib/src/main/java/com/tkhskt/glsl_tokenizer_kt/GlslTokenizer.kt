package com.tkhskt.glsl_tokenizer_kt

import com.tkhskt.glsl_tokenizer_kt.Operators.operators

object GlslTokenizer {

    fun tokenize(data: String, version: Version = Version.ES10): List<GlslToken> {
        val tokenizer = TokenizerInternal(version)
        return tokenizer.tokenize(data)
    }

    enum class Version {
        ES10,
        ES30,
    }

    private class TokenizerInternal(version: Version = Version.ES10) {

        private var total = 0
        private var mode: Mode = Mode.NORMAL
        private var lastCharacter = ""
        private var line = 1
        private var col = 0
        private var start = 0
        private var isNumber = false
        private var isOperator = false
        private var currentCharacter = ""
        private var targetCharacterIndex = 0

        private val content = mutableListOf<String>()
        private val tokens = mutableListOf<GlslToken>()

        private val literals = if (version == Version.ES10) {
            Literals.literals
        } else {
            Literals.literals30
        }

        private val builtIns = if (version == Version.ES10) {
            BuiltIns.builtIns
        } else {
            BuiltIns.builtIns30
        }

        fun tokenize(data: String): List<GlslToken> {
            val charArray = data.replace("\r\n", "\n").toCharArray()
            var lastIndex: Int
            while (targetCharacterIndex < charArray.size) {
                lastIndex = targetCharacterIndex
                currentCharacter = charArray[targetCharacterIndex].toString()
                processCharacter()
                if (lastIndex != targetCharacterIndex) {
                    if ("\n" == charArray[lastIndex].toString()) {
                        col = 0
                        ++line
                    } else {
                        ++col
                    }
                }
            }
            end()
            return tokens
        }

        private fun end() {
            if (content.isNotEmpty()) {
                token(content.joinToString(""))
            }
            mode = Mode.EOF
            token("(eof)")
        }

        private fun processCharacter() {
            targetCharacterIndex = when (mode) {
                Mode.NORMAL -> normal()
                Mode.BLOCK_COMMENT -> blockComment()
                Mode.LINE_COMMENT -> lineComment()
                Mode.PREPROCESSOR -> preprocessor()
                Mode.OPERATOR -> operator()
                Mode.INTEGER -> integer()
                Mode.FLOAT -> decimal()
                Mode.WHITESPACE -> whiteSpace()
                Mode.HEX -> hex()
                Mode.TOKEN -> readToken()
                else -> targetCharacterIndex
            }
        }

        private fun token(data: String) {
            val tokenType = mode.tokenType ?: return
            if (data.isNotEmpty()) {
                tokens.add(
                    GlslToken(
                        type = tokenType,
                        data = data,
                        position = start,
                        line = line,
                        column = col,
                    )
                )
            }
        }

        private fun normal(): Int {
            content.clear()

            if (lastCharacter == "/" && currentCharacter == "*") {
                start = total + targetCharacterIndex - 1
                mode = Mode.BLOCK_COMMENT
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if (lastCharacter == "/" && currentCharacter == "/") {
                start = total + targetCharacterIndex - 1
                mode = Mode.LINE_COMMENT
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if (currentCharacter == "#") {
                mode = Mode.PREPROCESSOR
                start = total + targetCharacterIndex
                return targetCharacterIndex
            }

            if ("""\s""".toRegex().containsMatchIn(currentCharacter)) {
                mode = Mode.WHITESPACE
                start = total + targetCharacterIndex
                return targetCharacterIndex
            }

            isNumber = """\d""".toRegex().containsMatchIn(currentCharacter)
            isOperator = """[^\w_]""".toRegex().containsMatchIn(currentCharacter)
            start = total + targetCharacterIndex
            mode = if (isNumber) {
                Mode.INTEGER
            } else if (isOperator) {
                Mode.OPERATOR
            } else {
                Mode.TOKEN
            }
            return targetCharacterIndex
        }

        private fun blockComment(): Int {
            if (currentCharacter == "/" && lastCharacter == "*") {
                content.add(currentCharacter)
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex + 1
            }
            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun lineComment(): Int {
            return preprocessor()
        }

        private fun preprocessor(): Int {
            if ((currentCharacter == "\r" || currentCharacter == "\n") && lastCharacter != "\\") {
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex
            }
            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun operator(): Int {
            if (lastCharacter == "." && """\d""".toRegex().containsMatchIn(currentCharacter)) {
                mode = Mode.FLOAT
                return targetCharacterIndex
            }

            if (lastCharacter == "/" && currentCharacter == "*") {
                mode = Mode.BLOCK_COMMENT
                return targetCharacterIndex
            }

            if (lastCharacter == "/" && currentCharacter == "/") {
                mode = Mode.LINE_COMMENT
                return targetCharacterIndex
            }

            if (currentCharacter == "." && content.isNotEmpty()) {
                determineOperator(content)
                mode = Mode.FLOAT
                return targetCharacterIndex
            }

            if (currentCharacter == ";" || currentCharacter == ")" || currentCharacter == "(") {
                if (content.isNotEmpty()) determineOperator(content)
                token(currentCharacter)
                mode = Mode.NORMAL
                return targetCharacterIndex + 1
            }

            val isCompositeOperator = content.size == 2 && currentCharacter != "="
            if ("""[\w_\d\s]""".toRegex()
                    .containsMatchIn(currentCharacter) || isCompositeOperator
            ) {
                determineOperator(content)
                mode = Mode.NORMAL
                return targetCharacterIndex
            }

            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun integer(): Int {
            if (currentCharacter == ".") {
                content.add(currentCharacter)
                mode = Mode.FLOAT
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if ("""[eE]""".toRegex().containsMatchIn(currentCharacter)) {
                content.add(currentCharacter)
                mode = Mode.FLOAT
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if (currentCharacter == "x" && content.size == 1 && content[0] == "0") {
                mode = Mode.HEX
                content.add(currentCharacter)
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if ("""[^\d]""".toRegex().containsMatchIn(currentCharacter)) {
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex
            }

            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun whiteSpace(): Int {
            if ("""[^\s]""".toRegex().containsMatchIn(currentCharacter)) {
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex
            }
            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun decimal(): Int {
            if (currentCharacter == "f") {
                content.add(currentCharacter)
                lastCharacter = currentCharacter
                targetCharacterIndex += 1
            }

            if ("""[eE]""".toRegex().containsMatchIn(currentCharacter)) {
                content.add(currentCharacter)
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if ((currentCharacter == "-" || currentCharacter == "+") && """[eE]""".toRegex()
                    .containsMatchIn(lastCharacter)
            ) {
                content.add(currentCharacter)
                lastCharacter = currentCharacter
                return targetCharacterIndex + 1
            }

            if ("""[^\d]""".toRegex().containsMatchIn(currentCharacter)) {
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex
            }

            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun hex(): Int {
            if ("""[^a-fA-F0-9]""".toRegex().containsMatchIn(currentCharacter)) {
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex
            }
            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun readToken(): Int {
            if ("""[^\d\w_]""".toRegex().containsMatchIn(currentCharacter)) {
                val contentString = content.joinToString("")
                mode = if (literals.any { it == contentString }) {
                    Mode.KEYWORD
                } else if (builtIns.any { it == contentString }) { // FIXME
                    Mode.BUILT_IN
                } else {
                    Mode.IDENTIFIER
                }
                token(content.joinToString(""))
                mode = Mode.NORMAL
                return targetCharacterIndex
            }
            content.add(currentCharacter)
            lastCharacter = currentCharacter
            return targetCharacterIndex + 1
        }

        private fun determineOperator(buf: List<String>): Boolean {
            var j = 0
            var idx = 0
            var res = ""

            do {
                idx = operators.indexOf(buf.slice(0 until buf.size + j).joinToString(""))
                res = if (idx < 0) "" else operators[idx]

                if (idx == -1) {
                    if (j-- + buf.size > 0) continue
                    res = buf.slice(0 until 1).joinToString("")
                }

                token(res)

                start += res.length
                val newContent = content.slice(res.length until content.size)
                content.run {
                    clear()
                    addAll(newContent)
                }
                return content.isNotEmpty()
            } while (true)
        }

        private enum class Mode(val tokenType: GlslToken.Type?) {
            NORMAL(null),
            TOKEN(null),
            BLOCK_COMMENT(GlslToken.Type.BLOCK_COMMENT),
            PREPROCESSOR(GlslToken.Type.PREPROCESSOR),
            LINE_COMMENT(GlslToken.Type.LINE_COMMENT),
            OPERATOR(GlslToken.Type.OPERATOR),
            INTEGER(GlslToken.Type.INTEGER),
            FLOAT(GlslToken.Type.FLOAT),
            IDENTIFIER(GlslToken.Type.IDENTIFIER),
            BUILT_IN(GlslToken.Type.BUILT_IN),
            KEYWORD(GlslToken.Type.KEYWORD),
            WHITESPACE(GlslToken.Type.WHITESPACE),
            EOF(GlslToken.Type.EOF),
            HEX(GlslToken.Type.INTEGER),
        }
    }
}
