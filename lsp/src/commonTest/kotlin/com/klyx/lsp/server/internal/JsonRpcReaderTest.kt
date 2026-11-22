package com.klyx.lsp.server.internal

import com.klyx.lsp.IntRequestId
import com.klyx.lsp.RequestMessage
import com.klyx.lsp.TextEdit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.Buffer
import kotlinx.serialization.json.Json

class JsonRpcReaderTest : FunSpec({
    test("read message") {
        val request = RequestMessage(IntRequestId(1), "test/test", params = null)

        val json = Json { prettyPrint = false; explicitNulls = false }
        val buffer = Buffer()
        val writer = JsonRpcWriter(buffer, json)
        writer.writeMessage(request)

        val jsonRpcReader = JsonRpcReader(buffer, json)
        val message = jsonRpcReader.readMessage()
        message shouldBe request
    }
})
