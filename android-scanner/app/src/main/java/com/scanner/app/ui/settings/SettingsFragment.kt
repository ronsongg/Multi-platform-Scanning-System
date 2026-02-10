package com.scanner.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.scanner.app.R
import com.scanner.app.databinding.FragmentSettingsBinding
import com.scanner.app.ui.MainViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            val url = result.contents
            binding.inputServerUrl.setText(url)
            connectToServer(url)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        observeViewModel()
    }

    private fun setupButtons() {
        // QR Scan
        binding.btnScanQr.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt("扫描PC端显示的二维码")
                setBeepEnabled(true)
                setOrientationLocked(true)
                setBarcodeImageEnabled(false)
            }
            barcodeLauncher.launch(options)
        }

        // Test Connection
        binding.btnTestConnection.setOnClickListener {
            val url = binding.inputServerUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                settingsViewModel.testConnection(url)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
            }
        }

        // Connect
        binding.btnConnect.setOnClickListener {
            val url = binding.inputServerUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                connectToServer(url)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
            }
        }

        // Audio Speed
        binding.sliderSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                updateSpeedUI(value)
                settingsViewModel.setPlaybackSpeed(value)
                (requireActivity() as? com.scanner.app.ui.MainActivity)?.audioHelper?.setSpeed(value)
            }
        }
    }

    private fun updateSpeedUI(value: Float) {
        binding.textSpeedLabel.text = String.format("%.1fx", value)
    }

    private fun connectToServer(url: String) {
        settingsViewModel.connect(url)
    }

    private fun observeViewModel() {
        settingsViewModel.savedUrl.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                binding.inputServerUrl.setText(url)
                binding.textCurrentServer.text = url
            }
        }

        settingsViewModel.playbackSpeed.observe(viewLifecycleOwner) { speed ->
            binding.sliderSpeed.value = speed
            updateSpeedUI(speed)
            (requireActivity() as? com.scanner.app.ui.MainActivity)?.audioHelper?.setSpeed(speed)
        }

        settingsViewModel.connectionState.observe(viewLifecycleOwner) { state ->
            binding.textConnectionStatus.visibility = View.VISIBLE
            when (state) {
                ConnectionState.TESTING -> {
                    binding.textConnectionStatus.text = getString(R.string.status_connecting)
                    binding.textConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_hint)
                    )
                }
                ConnectionState.SUCCESS -> {
                    binding.textConnectionStatus.text = getString(R.string.connection_success)
                    binding.textConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.success)
                    )
                    val url = binding.inputServerUrl.text.toString().trim()
                    mainViewModel.tryConnect(url)
                    binding.textCurrentServer.text = url
                }
                ConnectionState.FAILED -> {
                    binding.textConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.danger)
                    )
                }
                ConnectionState.IDLE -> {
                    binding.textConnectionStatus.visibility = View.GONE
                }
            }
        }

        settingsViewModel.connectionMessage.observe(viewLifecycleOwner) { msg ->
            if (settingsViewModel.connectionState.value == ConnectionState.FAILED) {
                binding.textConnectionStatus.text = msg
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
