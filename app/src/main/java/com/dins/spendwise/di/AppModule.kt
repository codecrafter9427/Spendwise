package com.dins.spendwise.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.dao.OutboxDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.local.database.Migrations
import com.dins.spendwise.data.local.database.SpendWiseDatabase
import com.dins.spendwise.data.repository.BudgetRepositoryImpl
import com.dins.spendwise.data.repository.CategoryRepositoryImpl
import com.dins.spendwise.data.repository.SettingsRepositoryImpl
import com.dins.spendwise.data.repository.TransactionRepositoryImpl
import com.dins.spendwise.domain.repository.BudgetRepository
import com.dins.spendwise.domain.repository.CategoryRepository
import com.dins.spendwise.domain.repository.SettingsRepository
import com.dins.spendwise.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.settingsDataStore by preferencesDataStore(name = "spendwise_settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): SpendWiseDatabase {
        lateinit var database: SpendWiseDatabase
        database = Room.databaseBuilder(
            context,
            SpendWiseDatabase::class.java,
            SpendWiseDatabase.NAME
        )
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(SpendWiseDatabase.createCallback(scope) { database })
            .build()
        return database
    }

    @Provides
    fun provideCategoryDao(db: SpendWiseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: SpendWiseDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideMonthlyBudgetDao(db: SpendWiseDatabase): MonthlyBudgetDao = db.monthlyBudgetDao()

    @Provides
    fun provideOutboxDao(db: SpendWiseDatabase): OutboxDao = db.outboxDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.settingsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
