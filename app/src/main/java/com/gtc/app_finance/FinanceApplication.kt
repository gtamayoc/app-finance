package com.gtc.app_finance

import android.app.Application
import com.gtc.app_finance.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@FinanceApplication)
            modules(appModules)
        }
    }
}
