package com.namilab.gallerycleaner.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.namilab.gallerycleaner.domain.AdGate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프리미엄 상태의 단일 소스는 Google Play 구매 기록이다.
 * [prefs]의 KEY_PREMIUM은 오프라인/연결 전 순간을 위한 로컬 캐시일 뿐,
 * 앱 시작 시 [restorePurchases]가 항상 Play 서버 기준으로 재동기화한다.
 */
@Singleton
class AdGateImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdGate {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val listeners = mutableListOf<(Boolean) -> Unit>()
    private var lifetimeProductDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach(::handlePurchase)
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryLifetimeProductDetails()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing 서비스 연결 끊김 — 다음 launchPremiumPurchase 시 재연결 필요")
            }
        })
    }

    override fun isUnlocked(): Boolean = isPremium() || remainingMs() > 0

    override fun unlock() {
        prefs.edit()
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + UNLOCK_DURATION_MS)
            .apply()
    }

    override fun remainingMs(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    override fun isPremium(): Boolean = prefs.getBoolean(KEY_PREMIUM, false)

    override fun setPremium(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
        listeners.forEach { it(value) }
    }

    override fun launchPremiumPurchase(activity: Activity) {
        val details = lifetimeProductDetails
        if (details == null) {
            Log.w(TAG, "상품 정보 미로딩 — Play Console에 '$PRODUCT_ID_LIFETIME' 상품이 등록/전파됐는지 확인 필요")
            return
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun addOnPremiumChangedListener(listener: (Boolean) -> Unit) {
        listeners += listener
    }

    private fun queryLifetimeProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_LIFETIME)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                lifetimeProductDetails = productDetailsList.firstOrNull()
            }
        }
    }

    /** 재설치·기기 변경 후에도 이미 구매한 프리미엄을 Play 서버 기준으로 복원 */
    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.none { it == PRODUCT_ID_LIFETIME }) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        setPremium(true)

        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) {}
        }
    }

    companion object {
        private const val TAG = "AdGateImpl"
        private const val PREFS_NAME = "ad_gate_prefs"
        private const val KEY_EXPIRES_AT = "ad_free_expires_at"
        private const val KEY_PREMIUM = "is_premium"
        private const val UNLOCK_DURATION_MS = 60 * 60 * 1000L // 1시간

        /** Play Console에 등록할 평생 구매(INAPP) 상품 ID */
        const val PRODUCT_ID_LIFETIME = "gallerycleaner_premium_lifetime"
    }
}
