package com.klyx.lsp.server.internal

import com.klyx.lsp.NotificationMessage
import com.klyx.lsp.RequestId
import com.klyx.lsp.RequestMessage
import com.klyx.lsp.ResponseMessage

internal typealias ResponseHandler = suspend (ResponseMessage) -> Unit
internal typealias RequestHandler = suspend (RequestMessage) -> ResponseMessage
internal typealias NotificationHandler = suspend (RequestId?, NotificationMessage) -> Unit
