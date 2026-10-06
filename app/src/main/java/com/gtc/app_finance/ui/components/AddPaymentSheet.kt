package com.gtc.app_finance.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gtc.app_finance.domain.model.Credit
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.gtc.app_finance.ui.utils.TouchHapticType
import com.gtc.app_finance.ui.utils.bouncePress
import com.gtc.app_finance.ui.utils.rememberTouchFeedback
import com.gtc.app_finance.ui.theme.CupertinoSheetShape
import com.gtc.app_finance.ui.theme.CupertinoSurface
import com.gtc.app_finance.ui.theme.IndigoBlue
import com.gtc.app_finance.ui.theme.TextPrimary
import com.gtc.app_finance.ui.theme.TextSecondary
import com.gtc.app_finance.ui.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentSheet(
    credit: Credit,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSavePayment: (creditId: String, amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = CupertinoSheetShape,
        containerColor = CupertinoSurface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Abonar a Crédito",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = credit.title,
                style = MaterialTheme.typography.titleLarge,
                color = IndigoBlue
            )

            Text(
                text = "Saldo pendiente: ${CurrencyFormatter.formatPesos(credit.remainingAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Monto a Abonar ($)", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = IndigoBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextSecondary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                val parsedAmount = amountText.toDoubleOrNull()
                val isValid = parsedAmount != null && parsedAmount > 0

                val feedback = rememberTouchFeedback()
                val saveInteractionSource = remember { MutableInteractionSource() }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        feedback.perform(TouchHapticType.SUCCESS)
                        val amount = parsedAmount ?: 0.0
                        onSavePayment(credit.id, amount)
                    },
                    enabled = isValid && !isSaving,
                    interactionSource = saveInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .bouncePress(minScale = 0.95f, interactionSource = saveInteractionSource),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Registrar Abono")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
