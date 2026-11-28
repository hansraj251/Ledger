package com.ledger.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

private data class CalcDef(
    val name: String,
    val category: String,
    val description: String,
    val labels: List<String>,
    val defaults: List<String>,
    val formula: String
)

private val calcDefs = listOf(
    CalcDef("EMI Calculator", "Loans", "Monthly loan instalment", listOf("Loan amount (₹)", "Annual interest (%)", "Tenure (months)"), listOf("500000", "10", "60"), "emi"),
    CalcDef("Loan Eligibility", "Loans", "Estimate loan eligibility", listOf("Monthly EMI capacity (₹)", "Annual interest (%)", "Tenure (months)"), listOf("15000", "10", "120"), "eligibility"),
    CalcDef("Loan Prepayment", "Loans", "Estimate principal after prepayment", listOf("Outstanding principal (₹)", "Prepayment amount (₹)", "Annual interest (%)"), listOf("500000", "50000", "10"), "prepayment"),
    CalcDef("SIP Calculator", "Investments", "Monthly investment future value", listOf("Monthly investment (₹)", "Expected annual return (%)", "Period (years)"), listOf("5000", "12", "10"), "sip"),
    CalcDef("Step-up SIP", "Investments", "SIP with 10% annual contribution increase", listOf("Monthly starting SIP (₹)", "Expected annual return (%)", "Period (years)"), listOf("5000", "12", "10"), "stepup"),
    CalcDef("Lumpsum Calculator", "Investments", "One-time investment growth", listOf("Investment amount (₹)", "Expected annual return (%)", "Period (years)"), listOf("100000", "12", "10"), "lumpsum"),
    CalcDef("SWP Calculator", "Investments", "Corpus after monthly withdrawals for 50 years", listOf("Initial corpus (₹)", "Monthly withdrawal (₹)", "Annual return (%)"), listOf("1000000", "10000", "8"), "swp"),
    CalcDef("CAGR Calculator", "Investments", "Compound annual growth rate", listOf("Starting value (₹)", "Ending value (₹)", "Period (years)"), listOf("100000", "200000", "5"), "cagr"),
    CalcDef("FD Calculator", "Savings", "Quarterly-compounded FD maturity", listOf("Deposit amount (₹)", "Annual interest (%)", "Period (years)"), listOf("100000", "7", "5"), "fd"),
    CalcDef("RD Calculator", "Savings", "Recurring deposit maturity", listOf("Monthly deposit (₹)", "Annual interest (%)", "Period (months)"), listOf("5000", "7", "24"), "rd"),
    CalcDef("PPF Calculator", "Savings", "Annual PPF deposits with annual compounding", listOf("Yearly investment (₹)", "Annual interest (%)", "Period (years)"), listOf("150000", "7.1", "15"), "ppf"),
    CalcDef("Simple Interest", "Everyday", "Simple interest", listOf("Principal (₹)", "Annual interest (%)", "Period (years)"), listOf("100000", "8", "3"), "simple"),
    CalcDef("Compound Interest", "Everyday", "Annual compound interest", listOf("Principal (₹)", "Annual interest (%)", "Period (years)"), listOf("100000", "8", "5"), "compound"),
    CalcDef("GST Calculator", "Business", "Add or remove GST", listOf("Amount (₹)", "GST rate (%)", "Unused"), listOf("10000", "18", ""), "gst"),
    CalcDef("Discount Calculator", "Business", "Discount and final price", listOf("Marked price (₹)", "Discount (%)", "Unused"), listOf("10000", "15", ""), "discount"),
    CalcDef("Profit & Loss", "Business", "Profit or loss", listOf("Cost price (₹)", "Selling price (₹)", "Unused"), listOf("8000", "10000", ""), "profit"),
    CalcDef("Profit Margin", "Business", "Profit margin on selling price", listOf("Cost price (₹)", "Selling price (₹)", "Unused"), listOf("8000", "10000", ""), "margin"),
    CalcDef("Percentage Calculator", "Everyday", "Percentage of an amount", listOf("Amount (₹)", "Percentage (%)", "Unused"), listOf("2500", "15", ""), "percentage"),
    CalcDef("Average Calculator", "Everyday", "Average of three values", listOf("Value 1", "Value 2", "Value 3"), listOf("10", "20", "30"), "average"),
    CalcDef("Inflation Calculator", "Everyday", "Future cost after inflation", listOf("Current cost (₹)", "Annual inflation (%)", "Period (years)"), listOf("100000", "6", "10"), "inflation")
)

private val CalculateBackground = Color(0xFFF6F7FB)
private val CalculateBlue = Color(0xFF246BCE)
private val CalculateText = Color(0xFF172033)
private val CalculateMuted = Color(0xFF697386)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculateScreen(isPremium: Boolean, onHomeClick: () -> Unit, onExpensesClick: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<CalcDef?>(null) }
    val categories = listOf("All", "Loans", "Investments", "Savings", "Business", "Everyday")
    val filtered = calcDefs.filter {
        (category == "All" || it.category == category) &&
            (search.isBlank() || it.name.contains(search, true) || it.description.contains(search, true))
    }

    // This handler is deliberately inside CalculateScreen so Back/edge-swipe first closes
    // the calculator detail and leaves the user on the calculator list.
    BackHandler(enabled = selected != null) { selected = null }

    Scaffold(
        containerColor = CalculateBackground,
        topBar = {
            if (selected != null) {
                TopAppBar(
                    title = {
                        Text(
                            selected!!.name,
                            color = CalculateText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selected = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CalculateText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CalculateBackground)
                )
            }
        },
        bottomBar = {
            LedgerBottomNavigation(
                selectedTab = "calculate",
                isPremium = isPremium,
                onPartiesClick = onHomeClick,
                onExpensesClick = onExpensesClick,
                onCalculateClick = { selected = null }
            )
        }
    ) { inset ->
        if (selected == null) {
            Column(
                Modifier.fillMaxSize().padding(inset).padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search calculators") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = CalculateBlue,
                        unfocusedBorderColor = Color(0xFFE1E5ED)
                    )
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    categories.forEach { item ->
                        FilterChip(
                            modifier = Modifier.height(34.dp),
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item, maxLines = 1) },
                            shape = RoundedCornerShape(13.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = category == item,
                                borderColor = Color(0xFFE0E5EE),
                                selectedBorderColor = CalculateBlue
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE7F0FF),
                                selectedLabelColor = CalculateBlue,
                                containerColor = Color.White,
                                labelColor = CalculateText
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 18.dp)
                ) {
                    items(filtered, key = { it.name }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selected = item },
                            shape = RoundedCornerShape(19.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE9ECF2)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 17.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF0F5FF)
                                ) {
                                    Icon(
                                        Icons.Outlined.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.padding(13.dp).size(25.dp),
                                        tint = CalculateBlue
                                    )
                                }
                                Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                                    Text(item.name, color = CalculateText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(3.dp))
                                    Text(item.description, color = CalculateMuted, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(5.dp))
                                    Text(item.category, color = CalculateBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open ${item.name}",
                                    tint = Color(0xFF8792A5),
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text("No calculators found.", Modifier.padding(24.dp), color = CalculateMuted)
                        }
                    }
                }
            }
        } else {
            CalcDetail(
                def = selected!!,
                modifier = Modifier.fillMaxSize().padding(inset).padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun CalcDetail(def: CalcDef, modifier: Modifier = Modifier) {
    var values by remember(def.name) { mutableStateOf(def.defaults) }
    var gstInclusive by remember(def.name) { mutableStateOf(false) }
    val nums = values.map { it.toDoubleOrNull() ?: 0.0 }
    val a = nums[0]
    val b = nums[1]
    val c = nums[2]
    val result = remember(def.formula, a, b, c, gstInclusive) {
        when (def.formula) {
            "emi" -> {
                val r = b / 1200.0
                if (a <= 0 || c <= 0) 0.0 else if (r == 0.0) a / c else a * r * (1 + r).pow(c) / ((1 + r).pow(c) - 1)
            }
            "eligibility" -> {
                val r = b / 1200.0
                if (a <= 0 || c <= 0) 0.0 else if (r == 0.0) a * c else a * ((1 + r).pow(c) - 1) / (r * (1 + r).pow(c))
            }
            "prepayment" -> (a - b).coerceAtLeast(0.0)
            "sip", "stepup" -> {
                val months = (c * 12).toInt().coerceIn(0, 1200)
                val r = (1 + b / 100.0).pow(1.0 / 12.0) - 1
                if (def.formula == "sip") {
                    if (months == 0) 0.0 else if (r == 0.0) a * months else a * ((1 + r).pow(months) - 1) / r * (1 + r)
                } else {
                    var corpus = 0.0
                    for (m in 1..months) corpus = corpus * (1 + r) + a * (1.10).pow(((m - 1) / 12).toDouble())
                    corpus
                }
            }
            "lumpsum", "compound", "inflation" -> a * (1 + b / 100.0).pow(c)
            "swp" -> {
                var corpus = a
                val r = (1 + c / 100.0).pow(1.0 / 12.0) - 1
                repeat(600) { corpus = (corpus * (1 + r) - b).coerceAtLeast(0.0) }
                corpus
            }
            "cagr" -> if (a > 0 && c > 0 && b >= 0) ((b / a).pow(1.0 / c) - 1) * 100 else 0.0
            "fd" -> if (a > 0 && c > 0) a * (1 + b / 400.0).pow(4 * c) else 0.0
            "rd" -> {
                val n = c.toInt().coerceIn(0, 1200)
                val r = b / 1200.0
                if (n == 0) 0.0 else if (r == 0.0) a * n else a * ((1 + r).pow(n) - 1) / r * (1 + r)
            }
            "ppf" -> {
                var corpus = 0.0
                repeat(c.toInt().coerceIn(0, 100)) { corpus = (corpus + a) * (1 + b / 100.0) }
                corpus
            }
            "simple" -> a + a * b * c / 100.0
            "gst" -> if (gstInclusive) a * 100 / (100 + b) else a * (1 + b / 100)
            "discount" -> a * (1 - b / 100)
            "profit" -> b - a
            "margin" -> if (b > 0) (b - a) / b * 100 else 0.0
            "percentage" -> a * b / 100
            "average" -> (a + b + c) / 3
            else -> 0.0
        }
    }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val invested = when (def.formula) {
        "sip", "stepup" -> a * c * 12
        "rd", "ppf" -> a * c
        else -> a
    }

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(def.description, color = CalculateMuted, style = MaterialTheme.typography.bodyMedium)
        def.labels.forEachIndexed { index, label ->
            if (label != "Unused") {
                OutlinedTextField(
                    value = values[index],
                    onValueChange = { next ->
                        if (next.isEmpty() || next.matches(Regex("^\\d*\\.?\\d*$"))) {
                            values = values.toMutableList().also { it[index] = next }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = CalculateBlue,
                        unfocusedBorderColor = Color(0xFFE1E5ED)
                    )
                )
            }
        }
        if (def.formula == "gst") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = gstInclusive, onCheckedChange = { gstInclusive = it })
                Spacer(Modifier.width(9.dp))
                Text(if (gstInclusive) "Entered amount includes GST" else "Add GST to entered amount", color = CalculateText)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF)),
            border = BorderStroke(1.dp, Color(0xFFD8E6FF))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    when (def.formula) {
                        "emi" -> "Monthly EMI"
                        "eligibility" -> "Estimated eligible loan"
                        "gst" -> if (gstInclusive) "Amount before GST" else "Amount including GST"
                        "discount" -> "Price after discount"
                        "profit" -> if (result >= 0) "Profit" else "Loss"
                        "margin", "cagr" -> "Result"
                        "percentage" -> "Calculated value"
                        "average" -> "Average"
                        "inflation" -> "Estimated future cost"
                        "simple", "compound" -> "Total amount"
                        "swp" -> "Estimated remaining corpus"
                        else -> "Estimated future / maturity value"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = CalculateMuted
                )
                Text(
                    if (def.formula == "margin" || def.formula == "cagr") String.format(Locale.getDefault(), "%.2f%%", result)
                    else currency.format(result),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CalculateText
                )
                if (def.formula in listOf("sip", "stepup", "lumpsum", "fd", "rd", "ppf", "compound")) {
                    Text("Invested: ${currency.format(invested)}", color = CalculateMuted)
                    Text("Estimated gain: ${currency.format((result - invested).coerceAtLeast(0.0))}", color = CalculateMuted)
                }
                if (def.formula == "gst") Text("GST component: ${currency.format(if (gstInclusive) a - result else result - a)}", color = CalculateMuted)
                if (def.formula == "discount") Text("You save: ${currency.format(a - result)}", color = CalculateMuted)
            }
        }
        Text(
            "Illustrative estimate only. Actual bank, lender, tax and investment outcomes may differ. Verify applicable rates before making decisions.",
            style = MaterialTheme.typography.bodySmall,
            color = CalculateMuted
        )
    }
}
