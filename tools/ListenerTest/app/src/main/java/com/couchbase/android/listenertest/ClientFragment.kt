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
import com.couchbase.android.listenertest.databinding.FragmentClientBinding
import com.couchbase.android.listenertest.service.ServiceState
import org.koin.androidx.viewmodel.ext.android.viewModel

class ClientFragment : Fragment() {
    companion object {
        private const val TAG = "TEST/CLIENT_UI"
    }

    private val model by viewModel<ClientViewModel>()
    private var viewBinding: FragmentClientBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val binding = FragmentClientBinding.inflate(layoutInflater, container, false)

        binding.target.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                enableButtons()
            }
        })

        binding.startClient.setOnClickListener { startClient() }
        binding.stopClient.setOnClickListener { stopClient() }

        viewBinding = binding

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        model.replicatorState.observe(this, this::onStateChanged)
        model.replicatorError.observe(this, this::onError)
    }

    private fun startClient() {
        val url = viewBinding?.target?.text?.toString() ?: return
        model.startReplicator(url)
        enableButtons()
    }

    private fun stopClient() {
        model.stopReplicator()
        enableButtons()
    }

    private fun enableButtons() {
        val running = model.replicatorState.value != ServiceState.STOPPED
        viewBinding?.stopClient?.isEnabled = running
        viewBinding?.startClient?.isEnabled = (!running) && ((viewBinding?.target?.text?.length ?: -1) > 5)

    }

    private fun onStateChanged(state: ServiceState) {
        Log.d(TAG, "state change: ${state}")
        viewBinding?.clientState?.text = state.toString()
        enableButtons()
    }

    private fun onError(errMessage: String) = Toast.makeText(activity, errMessage, Toast.LENGTH_SHORT).show()
}