package com.couchbase.android.listenertest

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchbase.android.listenertest.service.ReplicatorService
import com.couchbase.android.listenertest.service.ServiceState
import com.couchbase.android.listenertest.service.asUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URISyntaxException


class ClientViewModel(private val replicatorService: ReplicatorService) : ViewModel() {
    companion object {
        private const val TAG = "TEST/CLIENT_MODEL"
    }

    val replicatorState = MutableLiveData(ServiceState.STOPPED)
    val replicatorError = MutableLiveData("")

    fun startReplicator(uri: String) {
        if (replicatorState.value != ServiceState.STOPPED) {
            return
        }
        try {
            uri.asUri().let {
                viewModelScope.launch(Dispatchers.IO) {
                    replicatorService.startReplicator(it).collect { ss -> replicatorState.postValue(ss) }
                }
            }
        } catch (e: URISyntaxException) {
            Log.d(TAG, "Bad URI: ${this}", e)
            replicatorError.value = e.message
        }
    }

    fun stopReplicator() {
        if (replicatorState.value != ServiceState.STOPPED) {
            replicatorService.stopReplicator()
        }
    }
}
