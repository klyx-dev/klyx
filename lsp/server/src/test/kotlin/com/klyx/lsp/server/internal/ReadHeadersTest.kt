package com.klyx.lsp.server.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.snapshot
import kotlinx.io.writeString

class ReadHeadersTest : FunSpec({
    fun newReader(content: String): Source {
        return Buffer().apply { writeString(content) }
    }

    test("read headers") {
        val buffer = Buffer()
        val reader = newReader("Content-Length: 123\r\n\r\n")
        readHeaders(reader, buffer)

        buffer.snapshot().decodeToString() shouldBe "Content-Length: 123\r\n\r\n"
        buffer.readByteArray() shouldBe "Content-Length: 123\r\n\r\n".encodeToByteArray()

        val buffer1 = Buffer()
        val reader1 =
            newReader("Content-Type: application/vscode-jsonrpc\r\nContent-Length: 1235\r\n\r\n{\"somecontent\":123}")
        readHeaders(reader1, buffer1)

        buffer1.readString() shouldBe "Content-Type: application/vscode-jsonrpc\r\nContent-Length: 1235\r\n\r\n"

        val buffer2 = Buffer()
        val reader2 =
            newReader("Content-Length: 1235\r\nContent-Type: application/vscode-jsonrpc\r\n\r\n{\"somecontent\":true}")
        readHeaders(reader2, buffer2)

        val headers = buffer2.snapshot().decodeToString()
        buffer2.readString() shouldBe "Content-Length: 1235\r\nContent-Type: application/vscode-jsonrpc\r\n\r\n"

        val messageLength = headers
            .split('\n')
            .find { it.startsWith(CONTENT_LEN_HEADER) }
            ?.removePrefix(CONTENT_LEN_HEADER)
            ?.trimEnd()
            ?.toLongOrNull()
            ?: throw JsonRpcException("invalid LSP message header $headers")
        messageLength shouldBe 1235
    }
})
