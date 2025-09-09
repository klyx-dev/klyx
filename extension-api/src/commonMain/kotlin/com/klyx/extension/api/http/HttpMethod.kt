package com.klyx.extension.api.http

enum class HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    PATCH,
}

fun HttpMethod.toKtorHttpMethod() = when (this) {
    HttpMethod.GET -> io.ktor.http.HttpMethod.Get
    HttpMethod.POST -> io.ktor.http.HttpMethod.Post
    HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
    HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
    HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
    HttpMethod.OPTIONS -> io.ktor.http.HttpMethod.Options
    HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
}
