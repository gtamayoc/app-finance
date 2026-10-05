package com.gtc.app_finance.di

import com.gtc.app_finance.BuildConfig
import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.database.AppDatabase
import com.gtc.app_finance.data.database.DatabaseConfig
import com.gtc.app_finance.data.database.TursoConfigProvider
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.database.TursoSyncClient
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.ui.screens.analytics.AnalyticsViewModel
import com.gtc.app_finance.ui.screens.credits.CreditsViewModel
import com.gtc.app_finance.ui.screens.dashboard.DashboardViewModel
import com.gtc.app_finance.ui.screens.transactions.TransactionsViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single {
        DatabaseConfig(
            tursoUrl = BuildConfig.TURSO_DB_URL,
            primaryAuthToken = BuildConfig.TURSO_PRIMARY_TOKEN,
            backupAuthToken = BuildConfig.TURSO_BACKUP_TOKEN
        )
    }

    single { TursoConfigProvider(config = get()) }
}

val databaseModule = module {
    single {
        val config: DatabaseConfig = get()
        TursoDatabaseHelper(
            context = androidContext(),
            dbName = config.dbName,
            dbVersion = config.dbVersion
        )
    }

    single { AppDatabase(helper = get()) }

    single { TransactionDao(dbHelper = get()) }
    single { CreditDao(dbHelper = get()) }
    single { PaymentDao(dbHelper = get()) }
}

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    single {
        TursoSyncClient(
            configProvider = get(),
            client = get()
        )
    }
}

val repositoryModule = module {
    single {
        FinanceRepository(
            transactionDao = get(),
            creditDao = get(),
            paymentDao = get(),
            tursoSyncClient = get(),
            configProvider = get()
        )
    }
}

val viewModelModule = module {
    viewModel { DashboardViewModel(repository = get()) }
    viewModel { TransactionsViewModel(repository = get()) }
    viewModel { CreditsViewModel(repository = get()) }
    viewModel { AnalyticsViewModel(repository = get()) }
}

val appModules = listOf(
    appModule,
    databaseModule,
    networkModule,
    repositoryModule,
    viewModelModule
)
