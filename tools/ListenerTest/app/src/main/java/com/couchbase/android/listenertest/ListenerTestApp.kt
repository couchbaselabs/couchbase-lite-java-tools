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
package com.couchbase.android.listenertest

import android.app.Application
import android.util.Log
import com.couchbase.android.listenertest.service.DatabaseService
import com.couchbase.android.listenertest.service.ListenerService
import com.couchbase.android.listenertest.service.ReplicatorService
import com.couchbase.android.listenertest.service.SecurityService
import com.couchbase.android.listenertest.service.WorkerService
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import java.net.NetworkInterface

class ListenerTestApp : Application() {
    companion object {
        private const val TAG = "TEST/APP"
    }

    @Suppress("USELESS_CAST")
    override fun onCreate() {
        super.onCreate()

        // Enable Koin dependency injection framework
        GlobalContext.startKoin {
            // inject Android context
            androidContext(this@ListenerTestApp)

            // dependency register modules
            modules(
                module {
                    single { DatabaseService(this@ListenerTestApp) as DatabaseService }
                    single { SecurityService() as SecurityService }
                    single { ReplicatorService(get(), get()) as ReplicatorService }
                    single { ListenerService(get(), get()) as ListenerService }
                    single { WorkerService(get()) as WorkerService }

                    viewModel { ServerViewModel(get()) }
                    viewModel { ClientViewModel(get()) }
                    viewModel { WorkerViewModel(get()) }
                })
        }

        // List the network interfaces on this device
        logNetInterfaces()
    }

    private fun logNetInterfaces() {
        NetworkInterface.getNetworkInterfaces().toList().forEach { iface ->
            iface.inetAddresses.toList().forEach { addr ->
                Log.d(
                    TAG,
                    "Device address @ ${iface.name}(${iface.isUp}, ${iface.isLoopback}): ${addr.hostAddress}"
                            + "(${addr::class.java.simpleName}, ${addr.isLoopbackAddress}, ${addr.isLinkLocalAddress}, ${addr.isSiteLocalAddress}, ${addr.isAnyLocalAddress})"
                )
            }
        }
    }
}
