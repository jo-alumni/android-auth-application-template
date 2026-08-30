package com.example.authappliation.data.di

import com.example.authappliation.data.auth.AuthRepositoryImpl
import com.example.authappliation.data.item.ItemRepositoryImpl
import com.example.authappliation.domain.auth.AuthRepository
import com.example.authappliation.domain.item.ItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository
}
