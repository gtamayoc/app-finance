package com.gtc.app_finance.data.database

import android.content.Context

class AppDatabase(val helper: TursoDatabaseHelper) {

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context,
            databaseName: String = "finance_app.db",
            databaseVersion: Int = 3
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(
                    TursoDatabaseHelper(context.applicationContext, databaseName, databaseVersion)
                ).also { INSTANCE = it }
            }
        }

        fun getInstance(helper: TursoDatabaseHelper): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(helper).also { INSTANCE = it }
            }
        }
    }
}
