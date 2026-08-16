package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class BillingResult {
    SUCCESS,
    NOTHING_TO_RESTORE,
    FAILURE
}

interface BillingManager {
    val isPremium: StateFlow<Boolean?>
    val premiumProductId: String
    val formattedPrice: StateFlow<String?>
    val billingResults: SharedFlow<BillingResult>
    fun queryPurchases()
    fun purchasePremium(productId: String)
    fun restorePurchases()
}
