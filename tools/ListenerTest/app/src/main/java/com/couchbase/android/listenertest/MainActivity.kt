package com.couchbase.android.listenertest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.couchbase.android.listenertest.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootBinding = ActivityMainBinding.inflate(layoutInflater)
        val contentBinding = rootBinding.mainContent

        setContentView(rootBinding.root)
        setSupportActionBar(rootBinding.toolbar)

        val viewPager = contentBinding.viewpager
        viewPager.adapter = MainPagerAdapter(this, 2)

        val mediator = TabLayoutMediator(contentBinding.slidingTabs, viewPager) { tab, pos ->
            tab.setText(
                when (pos) {
                    0 -> R.string.client
                    1 -> R.string.server
                    else -> {
                        Log.w("MEDIATOR", "Page index out of bounds: $pos")
                        throw IllegalStateException("Page index out of bounds: $pos")
                    }
                }
            )
        }

        mediator.attach()
    }
}

class MainPagerAdapter(activity: FragmentActivity?, private val pageCount: Int) :
    FragmentStateAdapter(activity!!) {
    override fun getItemCount() = pageCount

    override fun createFragment(pos: Int): Fragment {
        return when (pos) {
            0 -> ClientFragment()
            1 -> ServerFragment()
            else -> {
                Log.w("ADAPTER", "Page index out of bounds: $pos")
                throw IllegalStateException("Page index out of bounds: $pos")
            }
        }
    }
}
