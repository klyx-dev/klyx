package com.klyx.data.repository

import com.klyx.data.database.TrashDao
import com.klyx.data.database.TrashEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.rules.TemporaryFolder
import java.io.File

private class FakeTrashDao : TrashDao {
    val items = MutableStateFlow<List<TrashEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(entity: TrashEntity): Long {
        val id = nextId++
        items.update { it + entity.copy(id = id) }
        return id
    }

    override fun observeAll(): Flow<List<TrashEntity>> = items

    override fun observeCount(): Flow<Int> = MutableStateFlow(items.value.size)

    override suspend fun getById(id: Long): TrashEntity? = items.value.firstOrNull { it.id == id }

    override suspend fun getOlderThan(cutoff: Long): List<TrashEntity> =
        items.value.filter { it.deletedAt < cutoff }

    override suspend fun deleteById(id: Long) =
        items.update { list -> list.filterNot { it.id == id } }

    override suspend fun deleteAll() = items.update { emptyList() }
}

class TrashRepositoryTest : FunSpec({

    val tmp = TemporaryFolder()

    beforeSpec { tmp.create() }
    afterSpec { tmp.delete() }

    fun newRepo(dao: FakeTrashDao = FakeTrashDao()): Pair<TrashRepository, File> {
        val root = tmp.newFolder("trash-${System.nanoTime()}").resolve("items")
        return TrashRepository(dao, root) to root
    }

    test("moveToTrash moves item into trash root and records metadata") {
        runBlocking {
            val (repo, root) = newRepo()
            val project = tmp.newFolder("myproject").also {
                File(it, "a.txt").writeText("hello")
            }

            val result = repo.moveToTrash(project)

            result.shouldBeInstanceOf<TrashRepository.Result.Success>()
            val entity = repo.observeItems() as MutableStateFlow
            val record = entity.value.single()
            record.displayName shouldBe "myproject"
            record.isDirectory shouldBe true
            val entry = root.resolve(record.trashName)
            entry.isDirectory shouldBe true
            File(entry, "a.txt").readText() shouldBe "hello"
            project.exists() shouldBe false
        }
    }

    test("restore puts item back at original path") {
        runBlocking {
            val (repo, _) = newRepo()
            val file = tmp.newFile("note.txt").apply { writeText("data") }
            repo.moveToTrash(file)

            val entity = (repo.observeItems() as MutableStateFlow).value.single()
            val result = repo.restore(entity.id)

            result.shouldBeInstanceOf<TrashRepository.Result.Success>()
            file.readText() shouldBe "data"
            (repo.observeItems() as MutableStateFlow).value.isEmpty() shouldBe true
        }
    }

    test("restore renames when original path is occupied") {
        runBlocking {
            val (repo, _) = newRepo()
            val file = tmp.newFile("occupied.txt").apply { writeText("old") }
            repo.moveToTrash(file)
            file.writeText("new")

            val entity = (repo.observeItems() as MutableStateFlow).value.single()
            repo.restore(entity.id)

            file.readText() shouldBe "new"
            file.resolveSibling("occupied (1).txt").readText() shouldBe "old"
        }
    }

    test("deletePermanently removes disk entry and metadata") {
        runBlocking {
            val (repo, root) = newRepo()
            val file = tmp.newFile("gone.txt").apply { writeText("bye") }
            repo.moveToTrash(file)
            val entity = (repo.observeItems() as MutableStateFlow).value.single()

            repo.deletePermanently(entity.id)

            root.resolve(entity.trashName).exists() shouldBe false
            (repo.observeItems() as MutableStateFlow).value.isEmpty() shouldBe true
        }
    }

    test("purgeExpired removes only entries older than cutoff") {
        runBlocking {
            val dao = FakeTrashDao()
            val (repo, root) = newRepo(dao)

            val f1 = tmp.newFile("old.txt").apply { writeText("1") }
            val f2 = tmp.newFile("fresh.txt").apply { writeText("2") }
            repo.moveToTrash(f1)
            repo.moveToTrash(f2)

            val aged = dao.items.value.first()
                .copy(deletedAt = System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000)
            dao.items.value = listOf(aged) + dao.items.value.drop(1)

            repo.purgeExpired(retentionDays = 30)

            val remaining = (dao.items as MutableStateFlow).value
            remaining.size shouldBe 1
            remaining.single().displayName shouldBe "fresh.txt"
            root.resolve(aged.trashName).exists() shouldBe false
            root.resolve(remaining.single().trashName).exists() shouldBe true
        }
    }

    test("failed move leaves a Failure result") {
        runBlocking {
            val (repo, _) = newRepo()

            val result = repo.moveToTrash(File(tmp.root, "never-existed.txt"))

            result.shouldBeInstanceOf<TrashRepository.Result.Failure>()
        }
    }
})
