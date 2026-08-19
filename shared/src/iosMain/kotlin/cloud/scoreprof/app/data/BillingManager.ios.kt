package cloud.scoreprof.app.data

import kotlinx.coroutines.flow.*
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

    private val _billingResults = MutableSharedFlow<BillingResult>()
    override val billingResults: SharedFlow<BillingResult> = _billingResults.asSharedFlow()

    override val premiumProductId: String = "cloud.scoreprof.premium.monthly"

    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val observer = TransactionObserver(
        onSuccess = { 
            _isPremium.value = true 
            scope.launch { _billingResults.emit(BillingResult.SUCCESS) }
        },
        onFailure = { 
            _isPremium.value = false 
            scope.launch { _billingResults.emit(BillingResult.FAILURE) }
        }
    )

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(observer)
    }

    override fun queryPurchases() {
        // Fetch product details to get localized price and warm up the store
        fetchProductDetails(premiumProductId)
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    private fun fetchProductDetails(productId: String) {
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
                }
            }
            override fun request(request: SKRequest, didFailWithError: NSError) {
                println("ScoreProf IAP Error: ${didFailWithError.localizedDescription}")
            }
        })
        productsRequest?.start()
    }

    override fun purchasePremium(productId: String) {
        val identifiers = NSSet.setWithObject(productId)
        productsRequest = SKProductsRequest(productIdentifiers = identifiers)
        productsRequest?.setDelegate(object : NSObject(), SKProductsRequestDelegateProtocol {
            override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                val product = didReceiveResponse.products.firstOrNull() as? SKProduct
                if (product != null) {
                    val payment = SKPayment.paymentWithProduct(product)
                    SKPaymentQueue.defaultQueue().addPayment(payment)
                } else {
                    _isPremium.value = false
                    scope.launch { _billingResults.emit(BillingResult.FAILURE) }
                }
            }
            override fun request(request: SKRequest, didFailWithError: NSError) {
                _isPremium.value = false
                scope.launch { _billingResults.emit(BillingResult.FAILURE) }
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
                        onFailure()
                        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                    }
                    else -> SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                }
            }
        }
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        // If the queue finishes and no transactions were processed, we can assume nothing to restore
        println("Restore finished")
    }
}
