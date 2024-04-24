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

import com.couchbase.lite.BasicAuthenticator
import com.couchbase.lite.ListenerPasswordAuthenticator
import com.couchbase.lite.TLSIdentity
import com.couchbase.lite.internal.utils.PlatformUtils
import java.security.KeyStore
import java.security.cert.X509Certificate


const val EXTERNAL_KEY_STORE = "teststore.p12"
const val EXTERNAL_KEY_STORE_TYPE = "PKCS12"
const val EXTERNAL_KEY_PASSWORD = "couchbase"
const val EXTERNAL_KEY_ALIAS = "test"
const val SERVER_KEY_ALIAS = "test-server"

class SecurityService {
    val clientAuthenticator = BasicAuthenticator("daniel", "123".toCharArray())
    val listenerAuthenticator = ListenerPasswordAuthenticator { user, pwd ->
        (user == "daniel") && (String(pwd) == "123")
    }

    val serverIdentity
        get() = TLSIdentity.getIdentity(SERVER_KEY_ALIAS) ?: loadIdentity(SERVER_KEY_ALIAS)

    val serverCert
        get() = serverIdentity.certs[0] as X509Certificate

    private fun loadIdentity(alias: String): TLSIdentity {
        loadExternalKey(alias)
        return TLSIdentity.getIdentity(alias) ?: throw IllegalStateException("No such keystore entry: ${alias}")
    }

    private fun loadExternalKey(alias: String) {
        loadPlatformKeyStore().setEntry(
            alias,
            loadExternalKeyStore().getEntry(
                EXTERNAL_KEY_ALIAS,
                KeyStore.PasswordProtection(EXTERNAL_KEY_PASSWORD.toCharArray())
            ),
            null
        )
    }

    private fun loadExternalKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(EXTERNAL_KEY_STORE_TYPE)
        PlatformUtils.getAsset(EXTERNAL_KEY_STORE).use { keyStream ->
            keyStore.load(keyStream, EXTERNAL_KEY_PASSWORD.toCharArray())
        }

        return keyStore
    }

    private fun loadPlatformKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore
    }
}