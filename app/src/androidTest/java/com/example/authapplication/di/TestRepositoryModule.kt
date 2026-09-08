package com.example.authapplication.di

import com.example.authapplication.data.di.RepositoryModule
import com.example.authapplication.domain.auth.AuthRepository
import com.example.authapplication.domain.auth.FakeAuthRepository
import com.example.authapplication.domain.debug.ErrorInjectionRepository
import com.example.authapplication.domain.debug.FakeErrorInjectionRepository
import com.example.authapplication.domain.favorite.FakeFavoriteRepository
import com.example.authapplication.domain.favorite.FavoriteRepository
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.ItemRepository
import com.example.authapplication.domain.notification.FakeNotificationRepository
import com.example.authapplication.domain.notification.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * 計装テスト中だけ [RepositoryModule] を置き換え、:data のリポジトリ実装ではなく
 * :domain の testFixtures にあるFakeを注入するモジュール。
 *
 * これにより計装テストは実DataStore（端末上の実ファイル）に触れなくなるため、
 * 前回のテストが書き込んだ認証状態が残らず、テストごとに初期状態から始められる。
 * Hiltのコンポーネントは `HiltAndroidRule` によってテストごとに作り直されるので、
 * `@Singleton` なFakeもテストごとに新しいインスタンスになる。
 *
 * `DataStoreModule` は差し替えていない。DataStoreを要求するのはここで置き換えた
 * リポジトリ実装だけで、実装が注入されなくなればDataStoreも生成されないため。
 *
 * テストから状態を仕込めるよう、インターフェース型に加えてFakeの具象型でも注入できるようにしている。
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideFakeAuthRepository(): FakeAuthRepository = FakeAuthRepository()

    @Provides
    fun provideAuthRepository(fake: FakeAuthRepository): AuthRepository = fake

    @Provides
    @Singleton
    fun provideFakeItemRepository(): FakeItemRepository = FakeItemRepository()

    @Provides
    fun provideItemRepository(fake: FakeItemRepository): ItemRepository = fake

    @Provides
    @Singleton
    fun provideFakeFavoriteRepository(): FakeFavoriteRepository = FakeFavoriteRepository()

    @Provides
    fun provideFavoriteRepository(fake: FakeFavoriteRepository): FavoriteRepository = fake

    @Provides
    @Singleton
    fun provideFakeNotificationRepository(): FakeNotificationRepository = FakeNotificationRepository()

    @Provides
    fun provideNotificationRepository(fake: FakeNotificationRepository): NotificationRepository = fake

    @Provides
    @Singleton
    fun provideFakeErrorInjectionRepository(): FakeErrorInjectionRepository = FakeErrorInjectionRepository()

    @Provides
    fun provideErrorInjectionRepository(fake: FakeErrorInjectionRepository): ErrorInjectionRepository = fake
}
