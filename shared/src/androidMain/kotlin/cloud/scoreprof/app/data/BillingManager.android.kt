package cloud.scoreprof.app.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManagerImpl(
    private val context: Context
) : BillingManager, PurchasesUpdatedListener {

    var currentActivity: Activity? = null

    private val _isPremium = MutableStateFlow<Boolean?>(null)
    override val isPremium = _isPremium.asStateFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    override val formattedPrice = _formattedPrice.asStateFlow()

    override val premiumProductId: String = "remove_ads_premium"

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // With enableAutoServiceReconnection(), we might not need to manually restart here, 
                // but it's safe to keep a listener for logging or UI updates.
            }
        })
    }

    override fun purchasePremium(productId: String) {
        println("Billing: purchasePremium called for $productId")
        if (currentActivity == null) {
            println("Billing: Error - currentActivity is NULL")
        }
        currentActivity?.let { activity ->
            launchPurchaseFlow(activity, productId)
        }
    }

    override fun restorePurchases() {
        println("Billing: restorePurchases called")
        queryPurchases()
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        println("Billing: launchPurchaseFlow for $productId")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            println("Billing: queryProductDetailsAsync result: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = queryProductDetailsResult.productDetailsList
                println("Billing: Found ${detailsList.size} products")
                if (detailsList.isNotEmpty()) {
                    val productDetails = detailsList[0]
                    
                    // Capture the localized price string
                    val price = productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    _formattedPrice.value = price

                    val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        )
                        .build()
                    println("Billing: Launching billing flow")
                    billingClient.launchBillingFlow(activity, flowParams)
                } else {
                    println("Billing: No product details found for $productId. Check if the ID matches Play Console and if the user is a licensed tester.")
                    _isPremium.value = false
                }
            } else {
                _isPremium.value = false
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPremium.value = true
                }
            }
        }
    }

    override fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { purchase ->
                    purchase.products.contains(premiumProductId) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremium.value = hasPremium
            } else {
                println("Billing: queryPurchasesAsync error: ${billingResult.responseCode}")
                _isPremium.value = false
            }
        }
    }
}
