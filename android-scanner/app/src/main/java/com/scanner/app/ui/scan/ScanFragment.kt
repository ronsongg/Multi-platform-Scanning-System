package com.scanner.app.ui.scan

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.scanner.app.R
import com.scanner.app.databinding.FragmentScanBinding
import com.scanner.app.ui.MainActivity
import com.scanner.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val scanViewModel: ScanViewModel by viewModels()

    private lateinit var adapter: ScanRecordAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateHeader()
        setupRecyclerView()
        setupScanInput()
        setupFilters()
        observeMainViewModel()
        observeScanViewModel()
        registerHardwareScan()
    }

    private fun setupDateHeader() {
        binding.textDate.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        adapter = ScanRecordAdapter()
        binding.recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecords.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Ensure input has focus when returning to screen
        binding.inputScan.post {
            binding.inputScan.requestFocus()
        }
    }

    private fun setupScanInput() {
        binding.inputScan.requestFocus()
        // Prevent soft keyboard from popping up if possible (optional, depending on device)
        binding.inputScan.showSoftInputOnFocus = false 

        binding.inputScan.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performScan()
                true
            } else false
        }

        binding.btnConfirm.setOnClickListener {
            performScan()
        }
    }

    private fun setupFilters() {
        binding.filterAll.setOnClickListener { setActiveFilter(FilterMode.ALL) }
        binding.filterSuccess.setOnClickListener { setActiveFilter(FilterMode.SUCCESS) }
        binding.filterError.setOnClickListener { setActiveFilter(FilterMode.ERROR) }
    }

    private fun setActiveFilter(mode: FilterMode) {
        scanViewModel.setFilter(mode)

        val active = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_active)
        val inactive = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_inactive)

        binding.filterAll.background = if (mode == FilterMode.ALL) active else inactive
        binding.filterSuccess.background = if (mode == FilterMode.SUCCESS) active else inactive
        binding.filterError.background = if (mode == FilterMode.ERROR) active else inactive

        val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_hint)

        binding.filterAll.setTextColor(if (mode == FilterMode.ALL) activeColor else inactiveColor)
        binding.filterSuccess.setTextColor(if (mode == FilterMode.SUCCESS) activeColor else inactiveColor)
        binding.filterError.setTextColor(if (mode == FilterMode.ERROR) activeColor else inactiveColor)
    }

    private fun performScan() {
        val input = binding.inputScan.text.toString().trim()
        if (input.isNotEmpty()) {
            scanViewModel.submitScan(input)
            binding.inputScan.text?.clear()
            
            // Re-request focus to ensure continuous scanning
            binding.inputScan.post {
                binding.inputScan.requestFocus()
            }
        }
    }

    private fun observeMainViewModel() {
        mainViewModel.serverUrl.observe(viewLifecycleOwner) { url ->
            val datasetId = mainViewModel.activeDataset.value?.id ?: return@observe
            scanViewModel.setup(url, datasetId)
        }

        mainViewModel.activeDataset.observe(viewLifecycleOwner) { dataset ->
            val url = mainViewModel.serverUrl.value ?: return@observe
            if (dataset != null) {
                scanViewModel.setup(url, dataset.id)
                binding.textDatasetName.text = dataset.name
            } else {
                binding.textDatasetName.text = ""
            }
        }

        mainViewModel.progress.observe(viewLifecycleOwner) { progress ->
            if (progress != null) {
                binding.textScanned.text = progress.scanned.toString()
                binding.textTotal.text = progress.total.toString()
                binding.textPercentage.text = "${progress.percentage}%"
                binding.progressBar.progress = progress.percentage
            }
        }
    }

    private fun observeScanViewModel() {
        scanViewModel.scanState.observe(viewLifecycleOwner) { state ->
            updateScanUI(state)
        }

        scanViewModel.lastResult.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                binding.textZone.text = response.zone ?: ""
                binding.textStoreAddress.text = response.storeAddress ?: ""
                binding.textDuplicateTag.visibility =
                    if (response.isDuplicate) View.VISIBLE else View.GONE
                response.progress?.let { mainViewModel.updateProgress(it) }
            }
        }

        scanViewModel.records.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
            binding.emptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerRecords.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateScanUI(state: ScanState) {
        val activity = requireActivity() as MainActivity

        binding.stateWaiting.visibility = View.GONE
        binding.stateFound.visibility = View.GONE
        binding.stateNotFound.visibility = View.GONE
        binding.scanViewport.setBackgroundResource(R.drawable.bg_scan_viewport)

        when (state) {
            ScanState.WAITING -> {
                binding.stateWaiting.visibility = View.VISIBLE
            }
            ScanState.FOUND -> {
                binding.stateFound.visibility = View.VISIBLE
                binding.textZone.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.scan_found)
                )
                binding.textDuplicateTag.visibility = View.GONE
                val zone = scanViewModel.lastResult.value?.zone
                if (zone != null) activity.audioHelper.playZone(zone)
            }

            ScanState.DUPLICATE -> {
                binding.stateFound.visibility = View.VISIBLE
                binding.textZone.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.scan_duplicate)
                )
                binding.textDuplicateTag.visibility = View.VISIBLE
                // Play specific duplicate sound instead of zone sound
                activity.audioHelper.playDuplicate()
            }
            ScanState.NOT_FOUND -> {
                binding.stateNotFound.visibility = View.VISIBLE
                binding.scanViewport.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.scan_not_found_bg)
                )
                // Play specific error sound instead of warning beep
                activity.audioHelper.playError()
            }
            ScanState.ERROR -> {
                binding.stateNotFound.visibility = View.VISIBLE
                binding.scanViewport.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.scan_not_found_bg)
                )
                // Play specific error sound instead of warning beep
                activity.audioHelper.playError()
            }
        }
    }

    private fun registerHardwareScan() {
        (requireActivity() as MainActivity).onHardwareScan = { barcode ->
            requireActivity().runOnUiThread {
                binding.inputScan.setText(barcode)
                performScan()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as? MainActivity)?.onHardwareScan = null
        _binding = null
    }
}
