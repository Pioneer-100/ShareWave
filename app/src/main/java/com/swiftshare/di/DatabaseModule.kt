package com.swiftshare.di

import android.content.Context
import androidx.room.Room
import com.swiftshare.data.db.SwiftShareDatabase
import com.swiftshare.data.db.dao.TransferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SwiftShareDatabase =
        Room.databaseBuilder(
            context,
            SwiftShareDatabase::class.java,
            "swiftshare.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideTransferDao(db: SwiftShareDatabase): TransferDao = db.transferDao()
}
