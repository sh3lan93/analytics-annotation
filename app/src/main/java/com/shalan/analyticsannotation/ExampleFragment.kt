package com.shalan.analyticsannotation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.shalan.analytics.annotation.Track
import com.shalan.analytics.annotation.TrackScreen

@TrackScreen(screenName = "Example Fragment", screenClass = "ExampleScreen")
class ExampleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_example, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setup()
    }

    /**
     * Example of method tracking with no parameters.
     * This demonstrates tracking a simple user action without any additional data.
     */
    @Track(eventName = "fragment_init", includeGlobalParams = true)
    private fun init() {
        Toast.makeText(requireContext(), "Some text", Toast.LENGTH_SHORT).show()
    }

    /**
     * Example of method tracking with no parameters and include global params is off.
     * This demonstrates tracking a simple user action without any additional data.
     */
    @Track(eventName = "fragment_setup", includeGlobalParams = false)
    private fun setup() {
        Toast.makeText(requireContext(), "Some Setup", Toast.LENGTH_SHORT).show()
    }
}
