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

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.couchbase.android.listenertest.databinding.FragmentServerBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServerFragment : Fragment() {
    companion object {
        private const val TAG = "TEST/SERVE_UI"
    }

    private val model by viewModel<ServerViewModel>()
    private var viewBinding: FragmentServerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val binding = FragmentServerBinding.inflate(layoutInflater, container, false)

        binding.port.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                enableButtons()
            }
        })

        binding.startServer.setOnClickListener { startServer() }
        binding.stopServer.setOnClickListener { stopServer() }

        viewBinding = binding

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        model.listenerState.observe(this, this::onStateChanged)
        model.listenerError.observe(this, this::onError)
    }

    private fun startServer() {
        val port = viewBinding?.port?.text?.toString()?.toIntOrNull() ?: return
        model.startListener(port, viewBinding?.tls?.isChecked ?: false)
        enableButtons()
    }

    private fun stopServer() {
        model.stopListener()
        enableButtons()
    }

    private fun enableButtons() {
        val running = model.listenerState.value != null
        viewBinding?.stopServer?.isEnabled = running
        viewBinding?.startServer?.isEnabled = (!running) && ((viewBinding?.port?.text?.length ?: -1) > 0)

    }

    private fun onStateChanged(state: String?) {
        Log.d(TAG, "state change: ${state}")
        viewBinding?.serverState?.text = state
        enableButtons()
    }

    private fun onError(errMessage: String) = Toast.makeText(activity, errMessage, Toast.LENGTH_SHORT).show()
}
