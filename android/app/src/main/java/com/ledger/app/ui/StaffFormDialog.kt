package com.ledger.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StaffFormDialog(
    title: String,
    initialName: String = "",
    initialMobile: String = "",
    initialRole: String = "",
    initialJoiningDate: Long = System.currentTimeMillis(),
    initialSalary: String = "",
    showSalary: Boolean = false,
    showDelete: Boolean = false,
    saving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String) -> Unit,
    onDelete: () -> Unit = {}
) {
    key(title, initialName, initialMobile, initialRole, initialJoiningDate, initialSalary) {
        var name by remember { mutableStateOf(initialName) }
        var mobile by remember { mutableStateOf(initialMobile) }
        var role by remember { mutableStateOf(initialRole) }
        var joiningDate by remember {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(Date(initialJoiningDate))
            )
        }
        var salary by remember { mutableStateOf(initialSalary) }
        var dateError by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
        val displayDateFormat = remember {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        }

        val openJoiningDatePicker = {
            val calendar = Calendar.getInstance()
            runCatching { dateFormat.parse(joiningDate) }
                .getOrNull()
                ?.let { calendar.time = it }

            DatePickerDialog(
                context,
                { _, year, month, day ->
                    joiningDate = String.format(
                        Locale.US, "%04d-%02d-%02d",
                        year, month + 1, day
                    )
                    dateError = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { if (!saving) onDismiss() },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Staff name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { if (it.length <= 15) mobile = it },
                        label = { Text("Mobile number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Role / Designation") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = runCatching {
                            displayDateFormat.format(dateFormat.parse(joiningDate) ?: Date(initialJoiningDate))
                        }.getOrDefault(joiningDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Joining Date") },
                        isError = dateError,
                        trailingIcon = {
                            TextButton(
                                onClick = { openJoiningDatePicker() },
                                enabled = !saving
                            ) { Text("Choose") }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !saving) {
                                openJoiningDatePicker()
                            }
                    )
                    if (showSalary) {
                        OutlinedTextField(
                            value = salary,
                            onValueChange = { salary = it },
                            label = { Text("Starting monthly salary (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (dateError) Text("Please select a valid joining date.")
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            val parsed = try {
                                SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    .apply { isLenient = false }
                                    .parse(joiningDate.trim())?.time
                            } catch (_: Exception) {
                                null
                            }
                            val salaryValue = salary.toDoubleOrNull()
                            val validSalary = !showSalary || salary.isBlank() ||
                                (salaryValue != null && salaryValue >= 0.0)

                            if (parsed == null || !validSalary || name.isBlank()) {
                                dateError = parsed == null
                            } else {
                                onSave(name.trim(), mobile.trim(), role.trim(), parsed, salary)
                            }
                        },
                        enabled = !saving && name.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (saving) "Saving..."
                            else if (showDelete) "Save" else "Save Staff"
                        )
                    }
                }
            },
            dismissButton = if (showDelete) {
                {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onDelete,
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFDC2626)
                            )
                        ) {
                            Text("Delete Staff")
                        }
                    }
                }
            } else null
        )
    }
}
