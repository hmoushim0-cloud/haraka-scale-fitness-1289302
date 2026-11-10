package sa.factory.harakascalefitness.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.harakascalefitness.model.HarakaScaleItem

@Entity(tableName = "fitness_items_cache")
data class HarakaScaleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): HarakaScaleItem = HarakaScaleItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
