package com.gtc.app_finance.cloud.infrastructure.factory

enum class CloudProvider {
    GCP,
    FIREBASE,
    AWS,
    SUPABASE,
    IN_MEMORY;

    companion object {
        fun fromString(value: String?): CloudProvider {
            return when (value?.trim()?.uppercase()) {
                "GCP", "GOOGLE_CLOUD", "GOOGLE" -> GCP
                "FIREBASE" -> FIREBASE
                "AWS" -> AWS
                "SUPABASE" -> SUPABASE
                "IN_MEMORY", "MEMORY", "MOCK", "TEST" -> IN_MEMORY
                else -> IN_MEMORY
            }
        }
    }
}
