package com.maez.qubitexplorer.services

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Propinas opcionales con Google Play Billing. Son productos consumibles que no
 * desbloquean nada: la app es igual de completa sin ellos.
 *
 * Los IDs deben existir en Play Console → Monetizar → Productos integrados en la app.
 */
class TipJar(context: Context) : PurchasesUpdatedListener {

    enum class Tier(val productId: String, val fallbackPrice: String) {
        SMALL("tip_small", "US$ 1"),
        MEDIUM("tip_medium", "US$ 3"),
        LARGE("tip_large", "US$ 5")
    }

    sealed interface Event {
        data object Thanks : Event
        data object Unavailable : Event
    }

    /** Precio localizado de cada propina; null mientras Play no responde. */
    private val _prices = MutableStateFlow<Map<Tier, String>>(emptyMap())
    val prices: StateFlow<Map<Tier, String>> = _prices.asStateFlow()

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    private val details = mutableMapOf<Tier, ProductDetails>()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    consumePending()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun disconnect() {
        if (client.isReady) client.endConnection()
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun launch(activity: Activity, tier: Tier) {
        val product = details[tier]
        if (!client.isReady || product == null) {
            _events.value = Event.Unavailable
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach(::consume)
        }
    }

    private fun loadProducts() {
        val products = Tier.entries.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val found = mutableMapOf<Tier, String>()
            queryResult.productDetailsList.forEach { pd ->
                val tier = Tier.entries.firstOrNull { it.productId == pd.productId } ?: return@forEach
                details[tier] = pd
                pd.oneTimePurchaseOfferDetails?.formattedPrice?.let { found[tier] = it }
            }
            _prices.value = found
        }
    }

    /** Consume compras que quedaron sin consumir (p. ej. pagos pendientes ya aprobados). */
    private fun consumePending() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach(::consume)
        }
    }

    private fun consume(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _events.value = Event.Thanks
        }
    }
}
