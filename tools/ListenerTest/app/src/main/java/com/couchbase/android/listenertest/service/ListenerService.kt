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

class ListenerService(private val db: DatabaseService, private val security: SecurityService) {
    companion object {
        internal const val TAG = "LISTEN"
    }

    private var observableListener: ObservableListener? = null

    fun startListener(port: Int, useTls: Boolean): Flow<String?> {
        val listener = ObservableListener(
            URLEndpointListener(
                URLEndpointListenerConfigurationFactory.newConfig(
                    collections = db.getCollections(this),
                    port = port,
                    disableTls = !useTls,
                    authenticator = if (useTls) null else security.listenerAuthenticator,
                    identity = if (!useTls) null else security.serverIdentity
                )
            )
        )

        this.observableListener = listener

        Log.d(TAG, "Starting listener @${port} ${if (!useTls) "" else "tls"} ")
        return callbackFlow {
            listener.registerObserver { trySend(it) }
            listener.start()
            awaitClose { observableListener?.stop() }
        }
    }

    fun stopListener() {
        Log.d(TAG, "Stopping listener")
        observableListener?.stop()
        db.closeDb(this)
    }
}
