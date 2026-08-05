package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.StoreKit.*
import platform.Foundation.*
import platform.darwin.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private var productsRequest: SKProductsRequest? = null

class IOSBillingManager : BillingManager, SKPaymentTransactionObserverProtocol {
    private val _purchaseSuccess = MutableSharedFlow<Boolean>()
    override val purchaseSuccess: SharedFlow<Boolean> = _purchaseSuccess.asSharedFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    override val formattedPrice: StateFlow<String?> = _formattedPrice.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    override val premiumProductId: String = "Monthly_Ad_Removal_Subscription"

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(this)
    }

    override fun queryPurchases() {
        // Trigger a restore check to verify if they already have it. 
        // Note: Apple requires a "Restore" button specifically.
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    override fun purchasePremium(productId: String) {
        val identifiers = NSSet.setWithObject(productId)
        productsRequest = SKProductsRequest(productIdentifiers = identifiers)
        productsRequest?.setDelegate(object : NSObject(), SKProductsRequestDelegateProtocol {
            override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                val product = didReceiveResponse.products.firstOrNull() as? SKProduct
                if (product != null) {
                    // Localized price for iOS
                    val formatter = NSNumberFormatter()
                    formatter.numberStyle = NSNumberFormatterCurrencyStyle
                    formatter.locale = product.priceLocale
                    _formattedPrice.value = formatter.stringFromNumber(product.price)

                    val payment = SKPayment.paymentWithProduct(product)
                    SKPaymentQueue.defaultQueue().addPayment(payment)
                } else {
                    println("Product not found: $productId")
                }
            }
        })
        productsRequest?.start()
    }

    override fun restorePurchases() {
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    // SKPaymentTransactionObserverProtocol implementation
    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
        updatedTransactions.forEach { transaction ->
            if (transaction is SKPaymentTransaction) {
                when (transaction.transactionState) {
                    SKPaymentTransactionState.SKPaymentTransactionStatePurchased,
                    SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                        scope.launch {
                            _purchaseSuccess.emit(true)
                        }
                        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                    }
                    SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                        println("Purchase failed: ${transaction.error?.localizedDescription}")
                        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                    }
                    else -> {}
                }
            }
        }
    }
}
