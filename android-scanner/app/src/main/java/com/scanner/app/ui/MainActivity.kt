package com.scanner.app.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.scanner.app.R
import com.scanner.app.databinding.ActivityMainBinding
import com.scanner.app.util.AudioHelper
import com.scanner.app.util.ScanReceiver
import com.scanner.app.util.SoundHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    lateinit var audioHelper: AudioHelper
        private set
    lateinit var soundHelper: SoundHelper
        private set

    private var scanReceiver: ScanReceiver? = null

    // Callback for hardware scan results
    var onHardwareScan: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize helpers
        audioHelper = AudioHelper(this)
        soundHelper = SoundHelper(this)

        setupNavigation()
        observeViewModel()
        registerScanReceiver()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun observeViewModel() {
        viewModel.isConnected.observe(this) { connected ->
            updateConnectionUI(connected)
        }

        viewModel.pendingUploads.observe(this) { count ->
            if (count > 0) {
                binding.pendingBadge.visibility = View.VISIBLE
                binding.pendingBadge.text = "${count}${getString(R.string.items_unit)}${getString(R.string.pending_uploads)}"
            } else {
                binding.pendingBadge.visibility = View.GONE
            }
        }
    }

    private fun updateConnectionUI(connected: Boolean) {
        if (connected) {
            binding.connectionDot.background = ContextCompat.getDrawable(this, R.drawable.dot_online)
            binding.connectionText.text = getString(R.string.status_online)
            binding.connectionText.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            binding.connectionDot.background = ContextCompat.getDrawable(this, R.drawable.dot_offline)
            binding.connectionText.text = getString(R.string.status_offline)
            binding.connectionText.setTextColor(ContextCompat.getColor(this, R.color.danger))
        }
    }

    private fun registerScanReceiver() {
        scanReceiver = ScanReceiver { barcode ->
            onHardwareScan?.invoke(barcode)
        }
        val filter = ScanReceiver.createIntentFilter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(scanReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanReceiver?.let { unregisterReceiver(it) }
        audioHelper.release()
        soundHelper.release()
    }
}
