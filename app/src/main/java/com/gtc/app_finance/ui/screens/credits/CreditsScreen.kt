package com.gtc.app_finance.ui.screens.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.ui.components.AddCreditSheet
import com.gtc.app_finance.ui.components.AddPaymentSheet
import com.gtc.app_finance.ui.components.CreditCardItem
import com.gtc.app_finance.ui.theme.CupertinoBackground
import com.gtc.app_finance.ui.theme.IndigoBlue
import com.gtc.app_finance.ui.theme.TextPrimary
import com.gtc.app_finance.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    viewModel: CreditsViewModel
) {
    val credits by viewModel.credits.collectAsState()

    var showAddCreditSheet by remember { mutableStateOf(false) }
    var selectedCreditForPayment by remember { mutableStateOf<Credit?>(null) }

    val addCreditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addPaymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = CupertinoBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCreditSheet = true },
                containerColor = IndigoBlue,
                contentColor = TextPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Crédito")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Gestión de Créditos",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                text = "Control de deudas y abonos en tiempo real",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (credits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes créditos registrados",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(credits, key = { it.id }) { credit ->
                        CreditCardItem(
                            credit = credit,
                            onAddPaymentClick = { selectedCreditForPayment = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddCreditSheet) {
        AddCreditSheet(
            sheetState = addCreditSheetState,
            onDismissRequest = { showAddCreditSheet = false },
            onSaveCredit = { title, totalAmount, dueDate ->
                viewModel.addCredit(title, totalAmount, dueDate)
                showAddCreditSheet = false
            }
        )
    }

    selectedCreditForPayment?.let { credit ->
        AddPaymentSheet(
            credit = credit,
            sheetState = addPaymentSheetState,
            onDismissRequest = { selectedCreditForPayment = null },
            onSavePayment = { creditId, amount ->
                viewModel.recordPayment(creditId, amount)
                selectedCreditForPayment = null
            }
        )
    }
}
