package com.ledger.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.round

data class LedgerCurrency(
    val code: String,
    val label: String,
    val symbol: String,
    val locale: Locale
)

object CurrencySettings {
    val options = listOf(
        LedgerCurrency("INR", "INR — Indian Rupee", "₹", Locale("en", "IN")),
        LedgerCurrency("USD", "USD — US Dollar", "$", Locale.US),
        LedgerCurrency("EUR", "EUR — Euro", "€", Locale.GERMANY),
        LedgerCurrency("GBP", "GBP — British Pound", "£", Locale.UK),
        LedgerCurrency("AED", "AED — UAE Dirham", "د.إ", Locale("en", "AE")),
        LedgerCurrency("CAD", "CAD — Canadian Dollar", "CA$", Locale.CANADA),
        LedgerCurrency("AUD", "AUD — Australian Dollar", "A$", Locale("en", "AU")),
        LedgerCurrency("SGD", "SGD — Singapore Dollar", "S$", Locale("en", "SG")),
        LedgerCurrency("JPY", "JPY — Japanese Yen", "¥", Locale.JAPAN)
    )

    private const val LEGACY_PREFS = "ledger_currency_preferences"
    private const val LEGACY_KEY = "currency_code"

    val currentCode = mutableStateOf("INR")

    val selectedOption: LedgerCurrency
        get() = options.firstOrNull { it.code == currentCode.value } ?: options.first()

    val symbol: String
        get() = selectedOption.symbol

    private val databaseDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val scope = CoroutineScope(SupervisorJob() + databaseDispatcher)

    @Volatile
    private var selectionRevision = 0

    fun load(context: Context) {
        val appContext = context.applicationContext
        val revisionAtStart = selectionRevision

        scope.launch {
            val dao = LedgerDatabaseProvider.get(appContext).appSettingsDao()
            val preferences = appContext.getSharedPreferences(
                LEGACY_PREFS,
                Context.MODE_PRIVATE
            )
            val legacyCode = preferences.getString(LEGACY_KEY, null)
            val validLegacyCode = options.firstOrNull { it.code == legacyCode }?.code

            val existing = dao.getSettings()
            val selectedCode = when {
                existing == null -> validLegacyCode ?: "INR"
                existing.currencyCode == "INR" &&
                    validLegacyCode != null &&
                    validLegacyCode != "INR" -> validLegacyCode
                else -> existing.currencyCode
            }.let { code ->
                options.firstOrNull { it.code == code }?.code ?: "INR"
            }

            // Persist the resolved setting in Room, including the first-run default.
            dao.saveSettings(AppSettingsEntity(currencyCode = selectedCode))

            // Remove the old preference only after Room has stored the setting.
            preferences.edit().remove(LEGACY_KEY).apply()

            withContext(Dispatchers.Main) {
                if (selectionRevision == revisionAtStart) {
                    currentCode.value = selectedCode
                }
            }
        }
    }

    fun setCurrency(
        context: Context,
        code: String,
        onSaved: (() -> Unit)? = null
    ) {
        if (options.none { it.code == code }) return

        selectionRevision++
        currentCode.value = code
        val appContext = context.applicationContext

        scope.launch {
            LedgerDatabaseProvider.get(appContext)
                .appSettingsDao()
                .saveSettings(AppSettingsEntity(currencyCode = code))

            appContext.getSharedPreferences(
                LEGACY_PREFS,
                Context.MODE_PRIVATE
            ).edit().remove(LEGACY_KEY).apply()

            withContext(Dispatchers.Main) {
                onSaved?.invoke()
            }
        }
    }

    fun formatNumber(amount: Double): String {
        val formatter = NumberFormat.getIntegerInstance(selectedOption.locale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return formatter.format(round(amount).toLong())
    }

    fun format(amount: Double): String {
        val sign = if (amount < 0.0) "-" else ""
        return sign + symbol + formatNumber(abs(amount))
    }
}
