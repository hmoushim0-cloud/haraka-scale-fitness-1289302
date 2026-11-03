package sa.factory.harakascalefitness.domain.usecase

import sa.factory.harakascalefitness.domain.repository.HarakaScaleRepository
import sa.factory.harakascalefitness.model.HarakaScaleItem
import sa.factory.harakascalefitness.model.HarakaScaleOrder
import sa.factory.harakascalefitness.model.HarakaScaleTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: HarakaScaleRepository) {
    operator fun invoke(): Flow<List<HarakaScaleItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: HarakaScaleRepository) {
    val cart = repository.activeCart
    fun add(item: HarakaScaleItem) = repository.addToCart(item)
    fun remove(item: HarakaScaleItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: HarakaScaleRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): HarakaScaleOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: HarakaScaleRepository) {
    operator fun invoke(): Flow<HarakaScaleTelemetry> = repository.telemetryFlow
}
