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

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.couchbase.android.listenertest.ListenerTestApp
import com.couchbase.lite.Collection
import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.LogDomain
import com.couchbase.lite.LogLevel
import com.couchbase.lite.MutableDocument


class DatabaseService(app: ListenerTestApp) {
    private var handler: Handler
    private var databases: MutableMap<Class<*>, Database> = mutableMapOf()

    init {
        CouchbaseLite.init(app)

        val consoleLogger = Database.log.console
        consoleLogger.level = LogLevel.DEBUG
        consoleLogger.domains = LogDomain.ALL_DOMAINS

        handler = Handler(Looper.getMainLooper())
    }

    companion object {
        private const val TAG = "DB"

        val collectionNames = setOf("apples", "oranges")
    }

    fun getCollections(requester: Any): Set<Collection> {
        val db = getDb(requester)
        return collections(db)
    }

    fun closeDb(requester: Any) {
        synchronized(databases) {
            databases.remove(requester::class.java)
        }
    }

    private fun getDb(requester: Any): Database {
        val klass = requester::class.java
        val n: Int
        val db = synchronized(databases) {
            var d = databases[klass]
            if (d == null) {
                d = Database(klass.simpleName.lowercase())
                databases[klass] = d
            }
            n = databases.size
            d
        }

        if (n == 1) {
            handler.postDelayed(this::addDocuments, 60 * 1000)
        }

        return db
    }

    private fun addDocuments() {
        val dbs = synchronized(databases) { databases.values.toSet() }
        if (dbs.isEmpty()) {
            return
        }

        dbs.forEach { db ->
            collections(db).forEach { coll ->
                val doc = MutableDocument()
                Log.d(TAG, "Adding document ${doc.id} to ${coll}")
                coll.save(doc)
            }
        }

        handler.postDelayed(this::addDocuments, 60 * 1000)
    }

    private fun collections(db: Database): Set<Collection> {
        return collectionNames.map { db.createCollection(it) }.toSet()
    }
}
