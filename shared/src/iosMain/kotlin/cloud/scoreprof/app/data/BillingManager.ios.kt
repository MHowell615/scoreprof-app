package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class IOSBillingManager : BillingManager {
    
    private val _isPremium = MutableStateFlow<Boolean?>(null)
    override val isPremium: StateFlow<Boolean?> = _isPremium.asStateFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    override val formattedPrice: StateFlow<String?> = _formattedPrice.asStateFlow()

    override val premiumProductId: String = "Monthly_Ad_Removal_Subscription"

    private val scope = CoroutineScope(Dispatchers.Main)
    
    // Use a separate observer to avoid mixing Kotlin and Obj-C supertypes
    private val observer = TransactionObserver(
        onSuccess = { _isPremium.value = true },
        onFailure = { _isPremium.value = false }
    )

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(observer)
    }

    override fun queryPurchases() {
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    override fun purchasePremium(productId: String) {
        val identifiers = NSSet.setWithObject(productId)
        productsRequest = SKProductsRequest(productIdentifiers = identifiers)
        productsRequest?.setDelegate(object : NSObject(), SKProductsRequestDelegateProtocol {
            override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                val product = didReceiveResponse.products.firstOrNull() as? SKProduct
                if (product != null) {
                    val formatter = NSNumberFormatter()
                    formatter.numberStyle = NSNumberFormatterCurrencyStyle
                    formatter.locale = product.priceLocale
                    _formattedPrice.value = formatter.stringFromNumber(product.price)

                    val payment = SKPayment.paymentWithProduct(product)
                    SKPaymentQueue.defaultQueue().addPayment(payment)
                } else {
                    println("Product not found: $productId")
                    _isPremium.value = false
                }
            }

            override fun request(request: SKRequest, didFailWithError: NSError) {
                println("Product request failed: ${didFailWithError.localizedDescription}")
                _isPremium.value = false
            }
        })
        productsRequest?.start()
    }

    override fun restorePurchases() {
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }
}

private class TransactionObserver(
    private val onSuccess: () -> Unit,
    private val onFailure: () -> Unit
) : NSObject(), SKPaymentTransactionObserverProtocol {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
        updatedTransactions.forEach { transaction ->
            if (transaction is SKPaymentTransaction) {
                when (transaction.transactionState) {
                    SKPaymentTransactionState.SKPaymentTransactionStatePurchased,
                    SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                        onSuccess()
                        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                    }
                    SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                        println("Purchase failed: ${transaction.error?.localizedDescription}")
                        onFailure()
                        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                    }
                    SKPaymentTransactionState.SKPaymentTransactionStateDeferred,
                    SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> {
                        // In progress
                    }
                    else -> {
                        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                    }
                }
            }
        }
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        println("Restore finished")
    }

    override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
        println("Restore failed: ${restoreCompletedTransactionsFailedWithError.localizedDescription}")
        onFailure()
    }
}
