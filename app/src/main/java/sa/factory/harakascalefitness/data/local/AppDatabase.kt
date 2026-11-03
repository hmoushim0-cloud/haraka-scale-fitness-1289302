package sa.factory.harakascalefitness.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HarakaScaleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fitnessDao(): HarakaScaleDao
}
