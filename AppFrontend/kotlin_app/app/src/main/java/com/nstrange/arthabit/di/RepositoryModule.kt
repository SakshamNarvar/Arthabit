package com.nstrange.arthabit.di

import com.nstrange.arthabit.data.repository.AuthRepositoryImpl
import com.nstrange.arthabit.data.repository.ExpenseRepositoryImpl
import com.nstrange.arthabit.data.repository.UserRepositoryImpl
import com.nstrange.arthabit.domain.repository.AuthRepository
import com.nstrange.arthabit.domain.repository.ExpenseRepository
import com.nstrange.arthabit.domain.repository.UserRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
}

