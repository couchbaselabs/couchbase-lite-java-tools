package com.couchbase.android.listenertest.service

import android.util.Log
import com.couchbase.lite.URLEndpointListener
import com.couchbase.lite.URLEndpointListenerConfigurationFactory
import com.couchbase.lite.newConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


private typealias ListenerObserver = (String?) -> Unit

private class ObservableListener(val listener: URLEndpointListener) {
    private var observer: ListenerObserver? = null

    fun registerObserver(observer: ListenerObserver) {
        this.observer = observer
    }

    fun start() {
        listener.start()
        observer?.invoke(getListenerUrl(listener))
    }

    fun stop() {
        listener.stop()
        observer?.invoke(null)
    }

    private fun getListenerUrl(listener: URLEndpointListener): String? {
        var uris = listener.urls
        if (uris.isEmpty()) uris = listener.constructUrls()
        uris.forEach { Log.d(ListenerService.TAG, "Listener URI: ${it}") }
        return uris.firstOrNull()?.toString()
    }
}

class ListenerService(private val dbSvc: DatabaseService, private val secureSvc: SecurityService) {
    companion object {
        internal const val TAG = "TEST/LISTEN_SVC"
    }

    private var observableListener: ObservableListener? = null

    fun startListener(port: Int, useTls: Boolean): Flow<String?> {
        val collections = dbSvc.getCollections(this)
        val listener = ObservableListener(
            URLEndpointListener(
                URLEndpointListenerConfigurationFactory.newConfig(
                    collections = collections,
                    port = port,
                    disableTls = !useTls,
                    authenticator = if (useTls) null else secureSvc.listenerAuthenticator,
                    identity = if (!useTls) null else secureSvc.serverIdentity
                )
            )
        )

        this.observableListener = listener
        return callbackFlow {
            listener.registerObserver { trySend(it) }
            Log.d(TAG, "Starting listener @${port} ${if (!useTls) "" else "tls"} ")
            listener.start()
            collections.forEach { it.close() }
            awaitClose { }
        }
    }

    fun stopListener() {
        Log.d(TAG, "Stopping listener")
        observableListener?.stop()
        dbSvc.closeDb(this)
    }
}
