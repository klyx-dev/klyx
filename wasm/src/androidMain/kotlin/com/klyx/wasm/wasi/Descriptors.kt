package com.klyx.wasm.wasi

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

internal class Descriptors {
    private val descriptors = mutableListOf<Descriptor?>()
    private val freeFds = sortedSetOf<Int>()

    operator fun get(fd: Int): Descriptor? {
        if (fd < 0 || fd >= descriptors.size) {
            return null
        }
        return descriptors[fd]
    }

    fun allocate(descriptor: Descriptor): Int {
        val fd = freeFds.firstOrNull()
        return if (fd != null) {
            freeFds.remove(fd)
            descriptors[fd] = descriptor
            fd
        } else {
            descriptors.add(descriptor)
            descriptors.size - 1
        }
    }

    fun free(fd: Int) {
        descriptors[fd] = null
        freeFds.add(fd)
    }

    operator fun set(fd: Int, descriptor: Descriptor) {
        descriptors[fd] = descriptor
    }

    fun closeAll() {
        val exceptions = mutableListOf<Throwable>()
        for (descriptor in descriptors) {
            try {
                if (descriptor is AutoCloseable) {
                    descriptor.close()
                }
            } catch (t: Throwable) {
                exceptions.add(t)
            }
        }
        if (exceptions.isNotEmpty()) {
            val exception = RuntimeException("Failed to close descriptors")
            exceptions.forEach { exception.addSuppressed(it) }
            throw exception
        }
    }

    interface Descriptor

    interface DataReader {
        @Throws(IOException::class)
        fun read(data: ByteArray): Int
    }

    interface DataWriter {
        @Throws(IOException::class)
        fun write(data: ByteArray): Int
    }

    interface Directory {
        val path: Path
    }

    class InStream(private val source: Source) : Descriptor, DataReader {

        @Throws(IOException::class)
        override fun read(data: ByteArray): Int {
            return try {
                val buffer = source.buffered()
                val bytesRead = buffer.readAtMostTo(data)
                if (bytesRead == 0 && buffer.exhausted()) -1 else bytesRead
            } catch (e: Exception) {
                throw IOException("Failed to read from source", e)
            }
        }

        @Throws(IOException::class)
        fun available(): Int {
            // kotlinx.io doesn't have a direct equivalent to available()
            // This would need to be implemented based on specific source type
            return 0
        }
    }

    class OutStream(private val sink: Sink) : Descriptor, DataWriter {
        @Throws(IOException::class)
        override fun write(data: ByteArray): Int {
            return try {
                val buffer = sink.buffered()
                buffer.write(data)
                buffer.flush()
                data.size
            } catch (e: Exception) {
                throw IOException("Failed to write to sink", e)
            }
        }
    }

    class PreopenedDirectory(
        val name: ByteArray,
        override val path: Path
    ) : Descriptor, Directory

    class OpenDirectory(
        override val path: Path
    ) : Descriptor, Directory

    class OpenFile(
        private val filePath: Path,
        private val fileSystem: FileSystem = SystemFileSystem,
        val fdFlags: Int = 0,
        val rights: Int = 0
    ) : Descriptor, AutoCloseable, DataReader, DataWriter {

        var position: Long = 0
        private var isOpen = true

        val path: Path get() = filePath

        @Throws(IOException::class)
        override fun read(data: ByteArray): Int {
            checkOpen()
            return try {
                val source = fileSystem.source(filePath).buffered()
                // Skip to current position
                if (position > 0) {
                    source.skip(position)
                }
                val bytesRead = source.readAtMostTo(data)
                position += bytesRead
                source.close()
                if (bytesRead == 0) -1 else bytesRead
            } catch (e: Exception) {
                throw IOException("Failed to read from file", e)
            }
        }

        @Throws(IOException::class)
        fun read(data: ByteArray, readPosition: Long): Int {
            checkOpen()
            return try {
                val source = fileSystem.source(filePath).buffered()
                if (readPosition > 0) {
                    source.skip(readPosition)
                }
                val bytesRead = source.readAtMostTo(data)
                source.close()
                if (bytesRead == 0) -1 else bytesRead
            } catch (e: Exception) {
                throw IOException("Failed to read from file at position", e)
            }
        }

        @Throws(IOException::class)
        override fun write(data: ByteArray): Int {
            checkOpen()
            return try {
                // For append mode, we need to read existing content first
                val existingData = if (fileSystem.exists(filePath)) {
                    val source = fileSystem.source(filePath).buffered().peek()
                    val existing = source.readByteArray()
                    source.close()
                    existing
                } else {
                    byteArrayOf()
                }

                val sink = fileSystem.sink(filePath).buffered()

                // Write existing data up to position
                if (position < existingData.size) {
                    sink.write(existingData, 0, position.toInt())
                }

                // Write new data
                sink.write(data)
                position += data.size

                // Write remaining existing data if any
                if (position < existingData.size) {
                    sink.write(existingData, position.toInt(), existingData.size - position.toInt())
                }

                sink.flush()
                sink.close()
                data.size
            } catch (e: Exception) {
                throw IOException("Failed to write to file", e)
            }
        }

        @Throws(IOException::class)
        fun write(data: ByteArray, writePosition: Long): Int {
            checkOpen()
            return try {
                val existingData = if (fileSystem.exists(filePath)) {
                    val source = fileSystem.source(filePath).buffered().peek()
                    val existing = source.readByteArray()
                    source.close()
                    existing
                } else {
                    byteArrayOf()
                }

                val sink = fileSystem.sink(filePath).buffered()

                // Write existing data up to write position
                if (writePosition < existingData.size) {
                    sink.write(existingData, 0, writePosition.toInt())
                }

                // Write new data at position
                sink.write(data)

                // Write remaining existing data if any
                val afterPosition = writePosition + data.size
                if (afterPosition < existingData.size) {
                    sink.write(
                        existingData,
                        afterPosition.toInt(),
                        existingData.size - afterPosition.toInt()
                    )
                }

                sink.flush()
                sink.close()
                data.size
            } catch (e: Exception) {
                throw IOException("Failed to write to file at position", e)
            }
        }

        @Throws(IOException::class)
        override fun close() {
            isOpen = false
        }

        private fun checkOpen() {
            if (!isOpen) {
                throw IOException("File is closed")
            }
        }
    }
}
