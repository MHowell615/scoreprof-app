package cloud.scoreprof.app.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillingManagerImpl(
    private val context: Context
) : BillingManager, PurchasesUpdatedListener {

    var currentActivity: Activity? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPremium = MutableStateFlow<Boolean?>(null)
    override val isPremium = _isPremium.asStateFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    override val formattedPrice = _formattedPrice.asStateFlow()

    private val _billingResults = MutableSharedFlow<BillingResult>()
    override val billingResults = _billingResults.asSharedFlow()

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
            override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                } else {
                    if (_isPremium.value == null) {
                        _isPremium.value = false
                    }
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun isEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val product = android.os.Build.PRODUCT
        
        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || model.contains("sdk_gphone")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == product
                || product.contains("sdk_gphone")
    }

    private fun isDebuggable(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun purchasePremium(productId: String) {
        if (isEmulator() && isDebuggable()) {
            scope.launch {
                delay(1000)
                _isPremium.value = true
                _billingResults.emit(BillingResult.SUCCESS)
            }
            return
        }
        currentActivity?.let { activity -> launchPurchaseFlow(activity, productId) }
    }

    override fun restorePurchases() {
        if (isEmulator() && isDebuggable()) {
            scope.launch {
                delay(1000)
                // For mock, let's say we don't have it initially but can "purchase" it
                if (_isPremium.value == true) {
                    _billingResults.emit(BillingResult.SUCCESS)
                } else {
                    _billingResults.emit(BillingResult.NOTHING_TO_RESTORE)
                }
            }
            return
        }
        queryPurchases(manual = true)
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productList = listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(BillingClient.ProductType.SUBS).build())
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = queryProductDetailsResult.productDetailsList
                if (detailsList.isNotEmpty()) {
                    val productDetails = detailsList[0]
                    val flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).setOfferToken(productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: "").build())).build()
                    billingClient.launchBillingFlow(activity, flowParams)
                } else {
                    _isPremium.value = false
                    scope.launch { _billingResults.emit(BillingResult.FAILURE) }
                }
            } else {
                _isPremium.value = false
                scope.launch { _billingResults.emit(BillingResult.FAILURE) }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: com.android.billingclient.api.BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            scope.launch { _billingResults.emit(BillingResult.FAILURE) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPremium.value = true
                    scope.launch { _billingResults.emit(BillingResult.SUCCESS) }
                }
            }
        }
    }

    override fun queryPurchases() { queryPurchases(false) }

    fun queryPurchases(manual: Boolean) {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { it.products.contains(premiumProductId) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                _isPremium.value = hasPremium
                if (manual) {
                    scope.launch { _billingResults.emit(if (hasPremium) BillingResult.SUCCESS else BillingResult.NOTHING_TO_RESTORE) }
                }
            } else {
                _isPremium.value = false
                if (manual) scope.launch { _billingResults.emit(BillingResult.FAILURE) }
            }
        }
    }
}
