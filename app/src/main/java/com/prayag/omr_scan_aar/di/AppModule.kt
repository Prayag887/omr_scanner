// di/AppModule.kt
package com.prayag.omr_scan_aar.di

import com.prayag.omr_scan_aar.domain.documentscanner.repository.DocumentRepository
import com.prayag.omr_scan_aar.data.documentscanner.repository.DocumentRepositoryImpl
import com.prayag.omr_scan_aar.data.omrresult.repository.OMRRepositoryImpl
import com.prayag.omr_scan_aar.domain.omrresult.repository.OMRRepository
import com.prayag.omr_scan_aar.presentation.main.MainViewModel
import com.prayag.omr_scan_aar.presentation.omrresult.ResultViewModel
import com.prayag.omr_scan_aar.utils.BitmapUtils
import com.prayag.omr_scan_aar.utils.OpenCVUtils
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