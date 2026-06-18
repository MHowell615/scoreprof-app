package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class IOSBillingManager : BillingManager {
    private val _purchaseSuccess = MutableSharedFlow<Boolean>()
    override val purchaseSuccess: SharedFlow<Boolean> = _purchaseSuccess.asSharedFlow()

    override fun queryPurchases() {
        // TODO: Implement iOS In-App Purchases
    }
}
