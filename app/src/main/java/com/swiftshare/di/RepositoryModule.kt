package com.swiftshare.di

import com.swiftshare.data.repository.DeviceRepositoryImpl
import com.swiftshare.data.repository.FileRepositoryImpl
import com.swiftshare.data.repository.TransferRepositoryImpl
import com.swiftshare.domain.repository.DeviceRepository
import com.swiftshare.domain.repository.FileRepository
import com.swiftshare.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
}
