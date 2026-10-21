package com.ledger.app.billing

import android.util.Log
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.ledger.app.backup.DriveAccountBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PremiumBillingState(
    val isPremium: Boolean = false,
    val productPrice: String = "",
    val isReady: Boolean = false,
    val message: String = ""
)

/**
 * Supports yearly subscriptions for new customers and preserves access
 * for existing lifetime_premium purchasers.
 */
class PremiumBillingManager(context: Context) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext

    companion object {
        const val LIFETIME_PRODUCT_ID = "lifetime_premium"
        const val SUBSCRIPTION_PRODUCT_ID = "ledger_premium_yearly"
        const val SUBSCRIPTION_BASE_PLAN_ID = "yearly"
    }

    private val _state = MutableStateFlow(PremiumBillingState())
    val state: StateFlow<PremiumBillingState> = _state.asStateFlow()

    private var subscriptionDetails: ProductDetails? = null
    private var isConnecting = false

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connect()
    }

    private fun connect() {
        if (billingClient.isReady) {
            loadSubscription()
            refreshOwnedPurchases()
            return
        }

        if (isConnecting) {
            Log.d("LedgerBilling", "Connection already in progress; skipping duplicate request")
            return
        }

        isConnecting = true
        Log.d("LedgerBilling", "Starting BillingClient connection")

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                isConnecting = false
                Log.e(
                    "LedgerBilling",
                    "onBillingSetupFinished responseCode=${result.responseCode}, " +
                        "debugMessage=${result.debugMessage}"
                )

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(
                        isReady = true,
                        message = ""
                    )
                    loadSubscription()
                    refreshOwnedPurchases()
                } else {
                    _state.value = _state.value.copy(
                        isReady = false,
                        message = "Google Play Billing is unavailable (${result.responseCode})."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                Log.w("LedgerBilling", "Billing service disconnected")
                _state.value = _state.value.copy(isReady = false)
            }
        })
    }

    private fun loadSubscription() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(SUBSCRIPTION_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                subscriptionDetails =
                    queryResult.productDetailsList.firstOrNull()

                val offer = subscriptionDetails
                    ?.subscriptionOfferDetails
                    ?.firstOrNull { it.basePlanId == SUBSCRIPTION_BASE_PLAN_ID }

                val price = offer?.pricingPhases
                    ?.pricingPhaseList
                    ?.lastOrNull()
                    ?.formattedPrice
                    .orEmpty()

                _state.value = _state.value.copy(
                    productPrice = price,
                    message = if (subscriptionDetails == null || offer == null) {
                        "Yearly subscription unavailable. Check ledger_premium_yearly and its active base plan in Play Console."
                    } else {
                        ""
                    }
                )
            } else {
                _state.value = _state.value.copy(
                    message = "Unable to load the yearly Premium subscription from Google Play."
                )
            }
        }
    }

    fun launchPurchase(activity: Activity, driveAccountId: String) {
        if (driveAccountId.isBlank()) {
            _state.value = _state.value.copy(
                message = "Connect Google Drive before subscribing to Premium."
            )
            return
        }

        if (!billingClient.isReady) {
            _state.value = _state.value.copy(
                message = "Connecting to Google Play. Please try again shortly."
            )
            connect()
            return
        }

        val details = subscriptionDetails
        val offer = details?.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == SUBSCRIPTION_BASE_PLAN_ID }

        if (details == null || offer == null) {
            _state.value = _state.value.copy(
                message = "Yearly Premium is unavailable. Verify ledger_premium_yearly and its active base plan in Play Console."
            )
            loadSubscription()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()

        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                message = "Unable to start the Premium subscription (${result.responseCode})."
            )
        }
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach(::processPurchase)
                refreshOwnedPurchases()
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refreshOwnedPurchases()

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(message = "Purchase cancelled.")
            }

            else -> {
                _state.value = _state.value.copy(
                    message = "Purchase did not complete (${result.responseCode})."
                )
            }
        }
    }

    private fun isPremiumProduct(purchase: Purchase): Boolean =
        LIFETIME_PRODUCT_ID in purchase.products ||
            SUBSCRIPTION_PRODUCT_ID in purchase.products

    private fun processPurchase(purchase: Purchase) {
        if (!isPremiumProduct(purchase)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Premium entitlement depends on Google Play ownership, not Drive Gmail.
        _state.value = _state.value.copy(isPremium = true, message = "")

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(
                        message = "Purchase completed; Google Play acknowledgement is pending."
                    )
                }
                refreshOwnedPurchases()
            }
        }
    }

    private fun refreshOwnedPurchases() {
        if (!billingClient.isReady) return


        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
            if (inAppResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

            val lifetimeOwned = inAppPurchases.any { purchase ->
                LIFETIME_PRODUCT_ID in purchase.products &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            inAppPurchases.forEach(::processPurchase)

            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(subsParams) { subsResult, subscriptions ->
                if (subsResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

                val subscriptionOwned = subscriptions.any { purchase ->
                    SUBSCRIPTION_PRODUCT_ID in purchase.products &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                subscriptions.forEach(::processPurchase)

                val ownerPremium = DriveAccountBinding.isOwnerPremiumAccount(appContext)
                _state.value = _state.value.copy(
                    isPremium = lifetimeOwned || subscriptionOwned || ownerPremium,
                    isReady = true,
                    message = if (ownerPremium || lifetimeOwned || subscriptionOwned) {
                        ""
                    } else {
                        _state.value.message
                    }
                )
            }
        }
    }

    fun refreshOwnership() {
        if (billingClient.isReady) {
            refreshOwnedPurchases()
        } else {
            connect()
        }
    }

    fun close() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}
