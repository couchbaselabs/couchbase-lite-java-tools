package com.couchbase.android.listenertest

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchbase.android.listenertest.service.ListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ServerViewModel(private val listenerService: ListenerService) : ViewModel() {
    companion object {
        private const val TAG = "TEST/SERVE_MODEL"
    }

    val listenerState = MutableLiveData<String?>(null)
    val listenerError = MutableLiveData("")

    fun startListener(port: Int, useTls: Boolean) {
        if (listenerState.value != null) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            listenerService.startListener(port, useTls).collect { uri ->
                listenerState.postValue(uri)
            }
        }
    }

    fun stopListener() {
        if (listenerState.value != null) {
            listenerService.stopListener()
        }
    }
}
