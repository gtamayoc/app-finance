package com.gtc.app_finance.ui.screens.credits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gtc.app_finance.R
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.ui.components.AddCreditSheet
import com.gtc.app_finance.ui.components.AddPaymentSheet
import com.gtc.app_finance.ui.components.CreditCardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    viewModel: CreditsViewModel
) {
    val haptic = LocalHapticFeedback.current
    val credits by viewModel.credits.collectAsState()

    var showAddCreditSheet by remember { mutableStateOf(false) }
    var selectedCreditForPayment by remember { mutableStateOf<Credit?>(null) }

    val addCreditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addPaymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAddCreditSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.credits_fab_new)
                )
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
                text = stringResource(R.string.credits_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.credits_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = credits.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.credits_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = credits.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(
                        items = credits,
                        key = { it.id },
                        contentType = { "credit" }
                    ) { credit ->
                        CreditCardItem(
                            credit = credit,
                            onAddPaymentClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCreditForPayment = it
                            }
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
