package com.klyx

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Objects

fun nullOutputStream() = object : OutputStream() {
    @Volatile
    private var closed = false

    private fun ensureOpen() {
        if (closed) {
            throw IOException("Stream closed")
        }
    }

    override fun write(b: Int) {
        ensureOpen()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        Objects.checkFromIndexSize(off, len, b.size)
        ensureOpen()
    }

    override fun close() {
        closed = true
    }
}

fun nullInputStream() = object : InputStream() {
    @Volatile
    private var closed = false

    private fun ensureOpen() {
        if (closed) {
            throw IOException("Stream closed")
        }
    }

    override fun available(): Int {
        ensureOpen()
        return 0
    }

    override fun read(): Int {
        ensureOpen()
        return -1
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        Objects.checkFromIndexSize(off, len, b.size)
        if (len == 0) return 0
        ensureOpen()
        return -1
    }

    override fun readAllBytes(): ByteArray {
        ensureOpen()
        return ByteArray(0)
    }

    override fun readNBytes(b: ByteArray, off: Int, len: Int): Int {
        Objects.checkFromIndexSize(off, len, b.size)
        ensureOpen()
        return 0
    }

    override fun readNBytes(len: Int): ByteArray {
        require(len >= 0) { "len < 0: $len" }
        ensureOpen()
        return ByteArray(0)
    }

    override fun skip(n: Long): Long {
        ensureOpen()
        return 0L
    }

    override fun skipNBytes(n: Long) {
        ensureOpen()
        if (n > 0) {
            throw EOFException()
        }
    }

    override fun transferTo(out: OutputStream): Long {
        Objects.requireNonNull(out)
        ensureOpen()
        return 0L
    }

    override fun close() {
        closed = true
    }
}
