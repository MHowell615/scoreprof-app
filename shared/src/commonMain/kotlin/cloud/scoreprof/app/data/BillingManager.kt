package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.SharedFlow

interface BillingManager {
    val purchaseSuccess: SharedFlow<Boolean>
    fun queryPurchases()
    fun purchasePremium(productId: String)
}
