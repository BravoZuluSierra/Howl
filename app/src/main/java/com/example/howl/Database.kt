package com.example.howl

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

const val DB_NAME = "Howl"

@Entity(tableName = "settings")
data class SavedSettings(
    @PrimaryKey
    val id: Long = 0,
    //All the Coyote parameters
    val channelALimit: Int = 70,
    val channelBLimit: Int = 70,
    val channelAFrequencyBalance: Int = 200,
    val channelBFrequencyBalance: Int = 200,
    val channelAIntensityBalance: Int = 0,
    val channelBIntensityBalance: Int = 0,
    //Player advanced controls
    val playbackSpeed: Float = 1.0f,
    val frequencyInversionA: Boolean = false,
    val frequencyInversionB: Boolean = false,
    var funscriptVolume: Float = 0.5f,
    val funscriptPositionalEffectStrength: Float = 1.0f,
    var funscriptFeel: Float = 1.0f,
    var funscriptFrequencyTimeOffset: Float = 0.1f,
    //Generator controls
    val autoChange: Boolean = true,
    val speedChangeProbability: Double = 0.2,
    val amplitudeChangeProbability: Double = 0.2,
    val frequencyChangeProbability: Double = 0.2,
    val waveChangeProbability: Double = 0.2,
    //Activity settings
    val activityChangeProbability: Float = 0.0f,
    //Misc options
    val showPowerMeter: Boolean = true,
    val smootherCharts: Boolean = true,
    val showDebugLog: Boolean = false,
    val powerStepSizeA: Int = 1,
    val powerStepSizeB: Int = 1,
    val powerAutoIncrementDelayA: Int = 120,
    val powerAutoIncrementDelayB: Int = 120,
)

@Dao
interface SavedSettingsDao {
    @Upsert
    suspend fun updateSettings(settings: SavedSettings)

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun getSettings(): SavedSettings?
}


@Database(
    entities = [SavedSettings::class],
    version = 4,
    exportSchema = false
)
abstract class HowlDatabase : RoomDatabase() {

    abstract fun savedSettingsDao(): SavedSettingsDao

    companion object {
        @Volatile
        private var Instance: HowlDatabase? = null

        fun getDatabase(context: Context): HowlDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, HowlDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
