package com.couchbase.android.listenertest

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchbase.android.listenertest.service.ServiceState
import com.couchbase.android.listenertest.service.WorkerService
import com.couchbase.android.listenertest.service.asUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException


class WorkerViewModel(private val workerService: WorkerService) : ViewModel() {
    companion object {
        private const val TAG = "TEST/WORK_MODEL"
    }

    val isRunning
        get() = workerService.isRunning

    fun attach() = workerService.trackWork()

    fun startWorker(uriStr: String): LiveData<ServiceState?>? {
        val uri: URI
        try {
            uri = uriStr.asUri()
        } catch (e: URISyntaxException) {
            Log.d(TAG, "Bad URI: ${this}", e)
            return null
        }

        viewModelScope.launch(Dispatchers.IO) {
            workerService.startWorker(uri)
        }

        return workerService.trackWork()
    }

    fun stopWorker(): LiveData<ServiceState?> {
        viewModelScope.launch(Dispatchers.IO) {
            workerService.stopWorker()
        }

        return workerService.trackWork()
    }
}


