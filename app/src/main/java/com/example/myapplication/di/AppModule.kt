// di/AppModule.kt
package com.example.myapplication.di

import com.example.myapplication.domain.repository.DocumentRepository
import com.example.myapplication.domain.repository.DocumentRepositoryImpl
import com.example.myapplication.presentation.camera.CameraViewModel
import com.example.myapplication.presentation.main.MainViewModel
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

    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { CameraViewModel(get(), get()) }
}