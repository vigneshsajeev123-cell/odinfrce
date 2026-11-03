package com.memoryuniverse.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.memoryuniverse.core.database.converters.Converters
import com.memoryuniverse.core.database.dao.ClusterDao
import com.memoryuniverse.core.database.dao.MemoryDao
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryPositionEntity
import com.memoryuniverse.core.database.entity.PhotoMetadataEntity

import com.memoryuniverse.core.database.dao.EmbeddingDao
import com.memoryuniverse.core.database.dao.RelationshipDao
import com.memoryuniverse.core.database.entity.MemoryEmbeddingEntity
import com.memoryuniverse.core.database.entity.MemoryRelationshipEntity

@Database(
    entities = [
        MemoryEntity::class,
        PhotoMetadataEntity::class,
        ClusterEntity::class,
        MemoryPositionEntity::class,
        MemoryEmbeddingEntity::class,
        MemoryRelationshipEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemoryDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun clusterDao(): ClusterDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun relationshipDao(): RelationshipDao

    companion object {
        private const val DATABASE_NAME = "memory_universe.db"

        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
