package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.SharedFlow

interface BillingManager {
    val purchaseSuccess: SharedFlow<Boolean>
    val premiumProductId: String
    fun queryPurchases()
    fun purchasePremium(productId: String)
    fun restorePurchases()
}
