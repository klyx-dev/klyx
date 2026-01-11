package com.klyx.project.lsp

interface LspStore {
    suspend fun stopAllServers()
}
