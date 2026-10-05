package com.gtc.app_finance.data.database

import android.content.Context

class AppDatabase private constructor(context: Context) {
    val helper: TursoDatabaseHelper = TursoDatabaseHelper(context.applicationContext)

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}
