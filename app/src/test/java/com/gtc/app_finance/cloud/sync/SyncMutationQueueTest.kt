package com.gtc.app_finance.cloud.sync

import com.gtc.app_finance.cloud.sync.mutation.CloudMutation
import com.gtc.app_finance.cloud.sync.mutation.LocalSyncMutationQueue
import com.gtc.app_finance.cloud.sync.mutation.MutationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncMutationQueueTest {

    private lateinit var queue: LocalSyncMutationQueue

    @Before
    fun setUp() {
        queue = LocalSyncMutationQueue()
    }

    @Test
    fun `enqueue inserts mutation into pending list`() = runBlocking {
        val mutation = CloudMutation(
            collection = "transactions",
            entityId = "tx-1",
            type = MutationType.UPSERT,
            payload = mapOf("title" to "Café")
        )

        queue.enqueue(mutation)

        assertEquals(1, queue.getCount())
        val pending = queue.getPending()
        assertEquals(1, pending.size)
        assertEquals("tx-1", pending[0].entityId)
        assertEquals(MutationType.UPSERT, pending[0].type)
    }

    @Test
    fun `redundant UPSERT for same entity consolidates to latest mutation`() = runBlocking {
        val mutation1 = CloudMutation(
            id = "m-1",
            collection = "transactions",
            entityId = "tx-1",
            type = MutationType.UPSERT,
            payload = mapOf("title" to "Draft")
        )
        val mutation2 = CloudMutation(
            id = "m-2",
            collection = "transactions",
            entityId = "tx-1",
            type = MutationType.UPSERT,
            payload = mapOf("title" to "Final")
        )

        queue.enqueue(mutation1)
        queue.enqueue(mutation2)

        assertEquals(1, queue.getCount())
        val pending = queue.getPending()
        assertEquals("m-2", pending[0].id)
        assertEquals("Final", pending[0].payload?.get("title"))
    }

    @Test
    fun `DELETE mutation purges any prior pending UPSERT for same entity`() = runBlocking {
        val upsert = CloudMutation(
            id = "m-upsert",
            collection = "credits",
            entityId = "cred-1",
            type = MutationType.UPSERT,
            payload = mapOf("title" to "Credit A")
        )
        val delete = CloudMutation(
            id = "m-delete",
            collection = "credits",
            entityId = "cred-1",
            type = MutationType.DELETE
        )

        queue.enqueue(upsert)
        queue.enqueue(delete)

        assertEquals(1, queue.getCount())
        val pending = queue.getPending()
        assertEquals("m-delete", pending[0].id)
        assertEquals(MutationType.DELETE, pending[0].type)
    }

    @Test
    fun `remove by mutation id removes only targeted mutation`() = runBlocking {
        val m1 = CloudMutation(id = "m-1", collection = "transactions", entityId = "tx-1", type = MutationType.UPSERT)
        val m2 = CloudMutation(id = "m-2", collection = "transactions", entityId = "tx-2", type = MutationType.UPSERT)

        queue.enqueue(m1)
        queue.enqueue(m2)
        assertEquals(2, queue.getCount())

        queue.remove("m-1")

        assertEquals(1, queue.getCount())
        assertEquals("m-2", queue.getPending()[0].id)
    }

    @Test
    fun `removeByEntity removes all mutations matching collection and entityId`() = runBlocking {
        val m1 = CloudMutation(id = "m-1", collection = "payments", entityId = "pay-100", type = MutationType.UPSERT)
        val m2 = CloudMutation(id = "m-2", collection = "transactions", entityId = "tx-200", type = MutationType.UPSERT)

        queue.enqueue(m1)
        queue.enqueue(m2)

        queue.removeByEntity("payments", "pay-100")

        assertEquals(1, queue.getCount())
        assertEquals("tx-200", queue.getPending()[0].entityId)
    }

    @Test
    fun `clear empties the entire mutation queue`() = runBlocking {
        queue.enqueue(CloudMutation(collection = "t", entityId = "1", type = MutationType.UPSERT))
        queue.enqueue(CloudMutation(collection = "c", entityId = "2", type = MutationType.UPSERT))
        assertEquals(2, queue.getCount())

        queue.clear()

        assertEquals(0, queue.getCount())
        assertTrue(queue.getPending().isEmpty())
    }
}
