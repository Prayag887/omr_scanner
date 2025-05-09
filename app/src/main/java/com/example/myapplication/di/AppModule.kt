// di/AppModule.kt
package com.example.myapplication.di

import com.example.myapplication.domain.documentscanner.repository.DocumentRepository
import com.example.myapplication.data.documentscanner.repository.DocumentRepositoryImpl
import com.example.myapplication.data.omrresult.repository.OMRRepositoryImpl
import com.example.myapplication.domain.omrresult.repository.OMRRepository
import com.example.myapplication.presentation.main.MainViewModel
import com.example.myapplication.presentation.omrresult.ResultViewModel
import com.example.myapplication.utils.BitmapUtils
import com.example.myapplication.utils.OpenCVUtils
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Utils
    single { BitmapUtils() }
    single { OpenCVUtils() }

    // Repositories
    single<DocumentRepository> { DocumentRepositoryImpl(get()) }
    viewModel { MainViewModel(get(), get()) }

    // Repository binding
    single<OMRRepository> { OMRRepositoryImpl() }
    viewModel { ResultViewModel(get()) }
}