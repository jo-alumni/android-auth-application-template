package com.example.authapplication.data.di

import com.example.authapplication.data.auth.AuthRepositoryImpl
import com.example.authapplication.data.item.ItemRepositoryImpl
import com.example.authapplication.domain.auth.AuthRepository
import com.example.authapplication.domain.item.ItemRepository
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
