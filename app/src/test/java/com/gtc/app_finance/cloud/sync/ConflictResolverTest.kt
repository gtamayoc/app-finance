package com.gtc.app_finance.cloud.sync

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.sync.conflict.ConflictDecision
import com.gtc.app_finance.cloud.sync.conflict.ConflictResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConflictResolverTest {

    private lateinit var resolver: ConflictResolver

    @Before
    fun setUp() {
        resolver = ConflictResolver()
    }

    @Test
    fun `shouldApplyRemote returns KEEP_LOCAL when unpushed local mutation exists`() {
        val remoteDoc = DatabaseDocument(
            id = "tx-1",
            collection = "transactions",
            data = mapOf("amount" to 200.0),
            updatedAtEpochMs = 2000L
        )

        val decision = resolver.shouldApplyRemote(
            remoteDoc = remoteDoc,
            localUpdatedAtEpochMs = 1000L,
            hasPendingLocalMutation = true
        )

        assertEquals(ConflictDecision.KEEP_LOCAL, decision)
    }

    @Test
    fun `shouldApplyRemote returns APPLY_REMOTE when no local timestamp exists and no pending mutation`() {
        val remoteDoc = DatabaseDocument(
            id = "tx-1",
            collection = "transactions",
            data = mapOf("amount" to 200.0),
            updatedAtEpochMs = 2000L
        )

        val decision = resolver.shouldApplyRemote(
            remoteDoc = remoteDoc,
            localUpdatedAtEpochMs = null,
            hasPendingLocalMutation = false
        )

        assertEquals(ConflictDecision.APPLY_REMOTE, decision)
    }

    @Test
    fun `shouldApplyRemote applies LWW logic correctly`() {
        val remoteDoc = DatabaseDocument(
            id = "tx-1",
            collection = "transactions",
            data = mapOf("amount" to 200.0),
            updatedAtEpochMs = 2500L
        )

        // Remote is newer -> APPLY_REMOTE
        val applyDecision = resolver.shouldApplyRemote(
            remoteDoc = remoteDoc,
            localUpdatedAtEpochMs = 2000L,
            hasPendingLocalMutation = false
        )
        assertEquals(ConflictDecision.APPLY_REMOTE, applyDecision)

        // Remote has identical timestamp -> IGNORE_DUPLICATE
        val duplicateDecision = resolver.shouldApplyRemote(
            remoteDoc = remoteDoc,
            localUpdatedAtEpochMs = 2500L,
            hasPendingLocalMutation = false
        )
        assertEquals(ConflictDecision.IGNORE_DUPLICATE, duplicateDecision)

        // Remote is older than local -> KEEP_LOCAL
        val keepLocalDecision = resolver.shouldApplyRemote(
            remoteDoc = remoteDoc,
            localUpdatedAtEpochMs = 3000L,
            hasPendingLocalMutation = false
        )
        assertEquals(ConflictDecision.KEEP_LOCAL, keepLocalDecision)
    }

    @Test
    fun `isReferentiallyValid always approves non-payment collections`() {
        val txDoc = DatabaseDocument(
            id = "tx-99",
            collection = "transactions",
            data = mapOf("title" to "Groceries")
        )
        assertTrue(resolver.isReferentiallyValid("transactions", txDoc, emptySet()))

        val creditDoc = DatabaseDocument(
            id = "cred-99",
            collection = "credits",
            data = mapOf("title" to "Car Loan")
        )
        assertTrue(resolver.isReferentiallyValid("credits", creditDoc, emptySet()))
    }

    @Test
    fun `isReferentiallyValid approves payment when creditId is known locally`() {
        val paymentDoc = DatabaseDocument(
            id = "pay-1",
            collection = "payments",
            data = mapOf("creditId" to "cred-valid-1", "amount" to 100.0)
        )
        val validCreditIds = setOf("cred-valid-1", "cred-valid-2")

        assertTrue(resolver.isReferentiallyValid("payments", paymentDoc, validCreditIds))
    }

    @Test
    fun `isReferentiallyValid rejects payment when creditId is missing or unknown locally`() {
        val validCreditIds = setOf("cred-valid-1")

        val missingCreditIdDoc = DatabaseDocument(
            id = "pay-bad-1",
            collection = "payments",
            data = mapOf("amount" to 100.0)
        )
        assertFalse(resolver.isReferentiallyValid("payments", missingCreditIdDoc, validCreditIds))

        val unknownCreditIdDoc = DatabaseDocument(
            id = "pay-bad-2",
            collection = "payments",
            data = mapOf("creditId" to "cred-unknown", "amount" to 100.0)
        )
        assertFalse(resolver.isReferentiallyValid("payments", unknownCreditIdDoc, validCreditIds))
    }
}
