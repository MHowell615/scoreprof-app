package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BillingManager {
    val isPremium: StateFlow<Boolean?>
    val premiumProductId: String
    val formattedPrice: StateFlow<String?>
    fun queryPurchases()
    fun purchasePremium(productId: String)
    fun restorePurchases()
}
