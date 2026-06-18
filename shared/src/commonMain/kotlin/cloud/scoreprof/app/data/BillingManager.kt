package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.SharedFlow

interface BillingManager {
    val purchaseSuccess: SharedFlow<Boolean>
    fun queryPurchases()
    // iOS and Android will have different launch mechanisms,
    // so we might need a platform-specific way to trigger it from UI.
}
