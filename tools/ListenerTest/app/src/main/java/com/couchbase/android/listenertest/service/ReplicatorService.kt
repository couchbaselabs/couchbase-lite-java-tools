package com.couchbase.android.listenertest.service

import android.util.Log
import com.couchbase.lite.Replicator
import com.couchbase.lite.ReplicatorActivityLevel
import com.couchbase.lite.ReplicatorChange
import com.couchbase.lite.ReplicatorConfigurationFactory
import com.couchbase.lite.URLEndpoint
import com.couchbase.lite.newConfig
import com.couchbase.lite.replicatorChangesFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URI


enum class ServiceState { STOPPED, RUNNING, CONNECTED }

class ReplicatorService(private val db: DatabaseService, private val security: SecurityService) {
    companion object {
        private const val TAG = "REPL"
    }

    private var replicator: Replicator? = null

    fun startReplicator(uri: URI): Flow<ServiceState> {
        Log.d(TAG, "Starting replicator @${uri}")

        val useTls = uri.scheme == "wss"
        val repl = Replicator(
            ReplicatorConfigurationFactory.newConfig(
                target = URLEndpoint(uri),
                collections = mapOf(db.getCollections(this) to null),
                continuous = true,
                authenticator = if (useTls) null else security.clientAuthenticator,
                pinnedServerCertificate = if (!useTls) null else security.serverCert
            )
        )

        replicator = repl
        repl.start()

        return repl.replicatorChangesFlow().map { state: ReplicatorChange ->
            when (state.status.activityLevel) {
                ReplicatorActivityLevel.STOPPED, ReplicatorActivityLevel.OFFLINE -> ServiceState.STOPPED
                ReplicatorActivityLevel.IDLE -> ServiceState.RUNNING
                ReplicatorActivityLevel.BUSY, ReplicatorActivityLevel.CONNECTING -> ServiceState.CONNECTED
            }
        }
    }

    fun stopReplicator() {
        Log.d(TAG, "Stopping replicator")
        replicator?.stop()
    }
}
