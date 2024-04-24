//
// Copyright (c) 2023 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.android.listenertest.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.ReplicatorActivityLevel
import com.couchbase.lite.URLEndpoint
import com.couchbase.lite.WorkManagerReplicatorConfiguration
import com.couchbase.lite.WorkManagerReplicatorFactory
import com.couchbase.lite.toReplicatorStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class WorkerFactory() : WorkManagerReplicatorFactory, KoinComponent {
    companion object {
        private const val TAG = "TEST/WORK_FACT"
        private const val FIELD_URI = "URI"
    }

    private val dbSvc: DatabaseService by inject()
    private val secureSvc: SecurityService by inject()

    constructor(uri: URI) : this() {
        dbSvc.getDb(this).createCollection(tag).save(MutableDocument(tag).setString(FIELD_URI, uri.toString()))
        Log.d(TAG, "URL: ${uri} stored in db for worker ${tag}")
    }

    override val tag = "${this::class.java.canonicalName!!}.WORKER".replace('.', '_')

    override fun getConfig(): WorkManagerReplicatorConfiguration {
        val uri = dbSvc.getDb(this).getCollection(tag)?.getDocument(tag)?.getString(FIELD_URI)?.asUri()
            ?: throw IllegalStateException("No URI for worker")
        Log.d(TAG, "URL: ${uri} recovered from db for worker ${tag}")

        val config = WorkManagerReplicatorConfiguration.from(URLEndpoint(uri))

        val useTls = uri.isTls()
        config.addCollections(dbSvc.getCollections(this))
        config.authenticator = if (useTls) null else secureSvc.clientAuthenticator
        config.pinnedServerCertificate = if (!useTls) null else secureSvc.serverCert

        return config
    }
}

class WorkerService(private val app: Context) {
    companion object {
        private const val TAG = "TEST/WORK_SVC"
    }

    val isRunning
        get() = running.get()

    private var running = AtomicBoolean(false)

    fun startWorker(uri: URI) {
        if (!running.compareAndSet(false, true)) return

        val workFactory = WorkerFactory(uri)

        val workBuilder = workFactory.periodicWorkRequestBuilder(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS
        )

        val tag = workFactory.tag
        WorkManager.getInstance(app).enqueue(
            workBuilder.addTag(tag)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .build()
        )

        Log.d(TAG, "Started worker ${tag}")
    }

    fun trackWork(): LiveData<ServiceState?> {
        val tag = WorkerFactory().tag
        return WorkManager.getInstance(app)
            .getWorkInfosByTagLiveData(tag)
            .map {
                it.mapNotNull { info ->
                    when (info.progress.toReplicatorStatus(tag)?.activityLevel) {
                        ReplicatorActivityLevel.STOPPED,
                        ReplicatorActivityLevel.OFFLINE,
                        ReplicatorActivityLevel.IDLE ->
                            ServiceState.RUNNING
                        ReplicatorActivityLevel.BUSY,
                        ReplicatorActivityLevel.CONNECTING ->
                            ServiceState.CONNECTED
                        else -> null
                    }
                }
                    .lastOrNull()
                    ?: if (isRunning) ServiceState.RUNNING else ServiceState.STOPPED
            }
    }

    fun stopWorker() {
        val tag = WorkerFactory().tag
        Log.d(TAG, "Stopping worker ${tag} ${running.get()}")
        if (!running.compareAndSet(true, false)) return
        WorkManager.getInstance(app).cancelAllWorkByTag(tag)

    }
}