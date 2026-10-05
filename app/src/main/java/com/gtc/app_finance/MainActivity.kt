package com.gtc.app_finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.database.AppDatabase
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.ui.main.MainScreen
import com.gtc.app_finance.ui.theme.AppfinanceTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: FinanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDb = AppDatabase.getInstance(applicationContext)
        val transactionDao = TransactionDao(appDb.helper)
        val creditDao = CreditDao(appDb.helper)
        val paymentDao = PaymentDao(appDb.helper)

        repository = FinanceRepository(transactionDao, creditDao, paymentDao)

        setContent {
            AppfinanceTheme {
                MainScreen(repository = repository)
            }
        }
    }
}