package com.gtc.app_finance.cloud.sync.conflict

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument

enum class ConflictDecision {
    APPLY_REMOTE,
    KEEP_LOCAL,
    IGNORE_DUPLICATE
}

class ConflictResolver {

    fun shouldApplyRemote(
        remoteDoc: DatabaseDocument,
        localUpdatedAtEpochMs: Long?,
        hasPendingLocalMutation: Boolean
    ): ConflictDecision {
        if (hasPendingLocalMutation) {
            return ConflictDecision.KEEP_LOCAL
        }

        if (localUpdatedAtEpochMs == null) {
            return ConflictDecision.APPLY_REMOTE
        }

        return when {
            remoteDoc.updatedAtEpochMs > localUpdatedAtEpochMs -> ConflictDecision.APPLY_REMOTE
            remoteDoc.updatedAtEpochMs == localUpdatedAtEpochMs -> ConflictDecision.IGNORE_DUPLICATE
            else -> ConflictDecision.KEEP_LOCAL
        }
    }

    fun isReferentiallyValid(
        collection: String,
        document: DatabaseDocument,
        validParentIds: Set<String>
    ): Boolean {
        if (collection == "payments") {
            val creditId = document.data["creditId"] as? String
            return !creditId.isNullOrBlank() && creditId in validParentIds
        }
        return true
    }
}
