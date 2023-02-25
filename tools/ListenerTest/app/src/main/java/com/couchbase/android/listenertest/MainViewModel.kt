package com.couchbase.android.listenertest

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchbase.android.listenertest.service.ListenerService
import com.couchbase.android.listenertest.service.ReplicatorService
import com.couchbase.android.listenertest.service.ServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException


class MainViewModel(
    private val listenerService: ListenerService,
    private val replicatorService: ReplicatorService
) : ViewModel() {
    companion object {
        private const val TAG = "MODEL"
    }

    val listenerState = MutableLiveData<String?>(null)
    val listenerError = MutableLiveData("")

    val replicatorState = MutableLiveData(ServiceState.STOPPED)
    val replicatorError = MutableLiveData("")

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

    fun startReplicator(uri: String) {
        if (replicatorState.value != ServiceState.STOPPED) {
            return
        }
        getTargetUri(uri)?.let {
            viewModelScope.launch(Dispatchers.IO) {
                replicatorService.startReplicator(it).collect { ss -> replicatorState.postValue(ss) }
            }
        }
    }


    fun stopReplicator() {
        if (replicatorState.value != ServiceState.STOPPED) {
            replicatorService.stopReplicator()
        }
    }

    private fun getTargetUri(uriStr: String): URI? {
        try {
            val uri = URI(uriStr).normalize()
            return when (uri.scheme) {
                "wss", "ws" -> uri
                else -> throw URISyntaxException(uriStr, "URI must have scheme ws: or wss:")
            }
        } catch (e: URISyntaxException) {
            Log.d(TAG, "Bad URI: ${uriStr}", e)
            replicatorError.value = e.message
        }
        return null
    }
}

