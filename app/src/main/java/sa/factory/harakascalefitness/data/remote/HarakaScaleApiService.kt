package sa.factory.harakascalefitness.data.remote

import sa.factory.harakascalefitness.model.HarakaScaleItem
import sa.factory.harakascalefitness.model.HarakaScaleOrder

interface HarakaScaleApiService {
    suspend fun fetchCatalog(): List<HarakaScaleItem>
    suspend fun submitOrder(order: HarakaScaleOrder): Boolean
}
