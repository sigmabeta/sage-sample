package net.sigmabeta.sage.sample.identity

import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.database.SampleDatabases
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DemoAccountsTest {
    private val database = SampleDatabases.inMemory()
    private val directory = RoomUserDirectory(database, iterations = 1_000)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `the published demo accounts are demo and demo2 with password sage`() {
        assertEquals(
            listOf(DemoAccount("demo", "sage"), DemoAccount("demo2", "sage")),
            DemoAccounts.all,
        )
    }

    @Test
    fun `seeding makes both demo accounts sign in as separate users`() = runTest {
        DemoAccounts.seed(directory)

        val demo = directory.verify("demo", "sage")
        val demo2 = directory.verify("demo2", "sage")

        assertNotNull(demo)
        assertNotNull(demo2)
        assertNotEquals(demo!!.id, demo2!!.id)
    }

    @Test
    fun `seeding again keeps the same users`() = runTest {
        DemoAccounts.seed(directory)
        val first = DemoAccounts.all.map { directory.verify(it.username, it.password) }

        DemoAccounts.seed(directory)
        DemoAccounts.seed(directory)

        assertEquals(first, DemoAccounts.all.map { directory.verify(it.username, it.password) })
    }

    @Test
    fun `seeding a database reopened from disk keeps the same users`() = runTest {
        val path = kotlin.io.path.createTempFile("identity", ".db").toFile().apply { delete() }
        try {
            val firstDb = SampleDatabases.file(path.absolutePath)
            val firstDirectory = RoomUserDirectory(firstDb, iterations = 1_000)
            DemoAccounts.seed(firstDirectory)
            val demo = firstDirectory.verify("demo", "sage")
            firstDb.close()

            val reopened = SampleDatabases.file(path.absolutePath)
            val reopenedDirectory = RoomUserDirectory(reopened, iterations = 1_000)
            DemoAccounts.seed(reopenedDirectory)

            assertEquals(demo, reopenedDirectory.verify("demo", "sage"))
            reopened.close()
        } finally {
            path.parentFile.listFiles { file -> file.name.startsWith(path.name) }?.forEach { it.delete() }
        }
    }
}
