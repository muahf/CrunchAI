package com.example.crunchai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Здесь будут @Provides и @Binds для репозиториев и источников данных
}