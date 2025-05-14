package com.prayag.omr_scan_aar.presentation.omrresult

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.omr_scan_aar.domain.omrresult.model.OMRResult
import com.prayag.omr_scan_aar.domain.omrresult.repository.OMRRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResultViewModel(private val repository: OMRRepository) : ViewModel() {

    private val _results = MutableLiveData<List<OMRResult>>()
    val results: LiveData<List<OMRResult>> = _results

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun processOMR(imagePath: String?) {
        if (imagePath == null) {
            _error.value = "Image path not found"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resultList = repository.processOMR(imagePath)
                _results.postValue(resultList)
            } catch (e: Exception) {
                _error.postValue("Failed to process image")
            }
        }
    }
}
