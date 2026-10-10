package com.ledger.app.billing

import com.ledger.app.backup.DriveAccountBinding

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.BillingFlowParams
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
 * Client-side Google Play purchase ownership manager for the non-consumable
 * lifetime_premium product. Product must also exist in Play Console.
 */
class PremiumBillingManager(context: Context) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    companion object {
        const val PRODUCT_ID = "lifetime_premium"
    }

    private val _state = MutableStateFlow(PremiumBillingState())
    val state: StateFlow<PremiumBillingState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
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
            loadProduct()
            refreshOwnedPurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(
                        isReady = true,
                        message = ""
                    )
                    loadProduct()
                    refreshOwnedPurchases()
                } else {
                    _state.value = _state.value.copy(
                        isReady = false,
                        message = "Google Play Billing is unavailable (${result.responseCode})."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(isReady = false)
            }
        })
    }

    private fun loadProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = queryResult.productDetailsList.firstOrNull()
                val offer = productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                _state.value = _state.value.copy(
                    productPrice = offer?.formattedPrice.orEmpty(),
                    message = if (productDetails == null) {
                        "Premium product unavailable. Check its Play Console setup."
                    } else {
                        ""
                    }
                )
            } else {
                _state.value = _state.value.copy(
                    message = "Unable to load the Premium product from Google Play."
                )
            }
        }
    }

    fun launchPurchase(activity: Activity, driveAccountId: String) {
        if (driveAccountId.isBlank()) {
            _state.value = _state.value.copy(
                message = "Connect Google Drive before purchasing Premium."
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

        val details = productDetails
        val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
        if (details == null || offer == null) {
            _state.value = _state.value.copy(
                message = "Premium is unavailable. Verify lifetime_premium in Play Console."
            )
            loadProduct()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken.orEmpty())
            .build()

        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(driveAccountId)
            .build()

        val result = billingClient.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                message = "Unable to start Google Play purchase (${result.responseCode})."
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

    private fun processPurchase(purchase: Purchase) {
        if (PRODUCT_ID !in purchase.products) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val currentAccountId = DriveAccountBinding.currentAccountId(appContext)
        val purchaseAccountId =
            purchase.accountIdentifiers?.obfuscatedAccountId
        val accountMatches =
            !currentAccountId.isNullOrBlank() &&
            purchaseAccountId == currentAccountId

        _state.value = _state.value.copy(
            isPremium = accountMatches,
            message = if (accountMatches) "" else
                "Premium is linked to a different Google Drive account."
        )

        // Acknowledge a completed Play purchase even if this app account does not match.
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(
                        message = "Premium purchased; Google Play acknowledgement is pending."
                    )
                }
                refreshOwnedPurchases()
            }
        }
    }

    private fun refreshOwnedPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

            val currentAccountId = DriveAccountBinding.currentAccountId(appContext)
            val owned = purchases.firstOrNull {
                PRODUCT_ID in it.products &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    !currentAccountId.isNullOrBlank() &&
                    it.accountIdentifiers?.obfuscatedAccountId == currentAccountId
            }

            _state.value = _state.value.copy(isPremium = owned != null)
            owned?.let(::processPurchase)
        }
    }

    fun refreshOwnership() {
        if (billingClient.isReady) refreshOwnedPurchases()
    }

    fun close() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}
