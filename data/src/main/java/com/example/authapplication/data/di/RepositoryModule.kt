package com.example.authapplication.data.di

import com.example.authapplication.data.auth.AuthRepositoryImpl
import com.example.authapplication.data.favorite.FavoriteRepositoryImpl
import com.example.authapplication.data.item.ItemRepositoryImpl
import com.example.authapplication.data.notification.NotificationRepositoryImpl
import com.example.authapplication.domain.auth.AuthRepository
import com.example.authapplication.domain.favorite.FavoriteRepository
import com.example.authapplication.domain.item.ItemRepository
import com.example.authapplication.domain.notification.NotificationRepository
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

    @Binds
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
