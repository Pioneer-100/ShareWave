package com.swiftshare.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.swiftshare.data.db.dao.TransferDao
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.data.db.entity.TransferStatus
import com.swiftshare.domain.model.TransferDirection
import com.swiftshare.domain.model.TransportChannel

@Database(
    entities = [TransferRecord::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SwiftShareConverters::class)
abstract class SwiftShareDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
}

class SwiftShareConverters {
    @TypeConverter fun directionToString(v: TransferDirection): String = v.name
    @TypeConverter fun stringToDirection(v: String): TransferDirection = TransferDirection.valueOf(v)

    @TypeConverter fun channelToString(v: TransportChannel): String = v.name
    @TypeConverter fun stringToChannel(v: String): TransportChannel = TransportChannel.valueOf(v)

    @TypeConverter fun statusToString(v: TransferStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): TransferStatus = TransferStatus.valueOf(v)
}
