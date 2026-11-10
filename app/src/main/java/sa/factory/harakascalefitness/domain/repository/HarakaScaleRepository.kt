package sa.factory.harakascalefitness.domain.repository

import sa.factory.harakascalefitness.model.HarakaScaleItem
import sa.factory.harakascalefitness.model.HarakaScaleOrder
import sa.factory.harakascalefitness.model.HarakaScaleTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HarakaScaleRepository {
    val catalogFlow: Flow<List<HarakaScaleItem>>
    val activeCart: StateFlow<List<HarakaScaleItem>>
    val telemetryFlow: Flow<HarakaScaleTelemetry>
    
    fun addToCart(item: HarakaScaleItem)
    fun removeFromCart(item: HarakaScaleItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): HarakaScaleOrder
    suspend fun refreshCatalog()
}
