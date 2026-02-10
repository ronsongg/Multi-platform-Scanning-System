package com.scanner.app.ui.scan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scanner.app.R
import com.scanner.app.data.local.CachedScanRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanRecordAdapter : ListAdapter<CachedScanRecord, ScanRecordAdapter.ViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textBoxNumber: TextView = itemView.findViewById(R.id.text_box_number)
        private val textZone: TextView = itemView.findViewById(R.id.text_record_zone)
        private val textAddress: TextView = itemView.findViewById(R.id.text_record_address)
        private val textStatus: TextView = itemView.findViewById(R.id.text_record_status)
        private val textTime: TextView = itemView.findViewById(R.id.text_record_time)

        fun bind(record: CachedScanRecord) {
            textBoxNumber.text = record.boxNumber

            if (record.found && record.zone != null) {
                textZone.text = "→ ${record.zone}"
                textZone.visibility = View.VISIBLE
            } else {
                textZone.visibility = View.GONE
            }

            textAddress.text = record.storeAddress ?: ""
            textAddress.visibility = if (record.storeAddress.isNullOrBlank()) View.GONE else View.VISIBLE

            val context = itemView.context
            when {
                !record.found -> {
                    textStatus.text = "未找到"
                    textStatus.setTextColor(ContextCompat.getColor(context, R.color.danger))
                }
                !record.firstScan -> {
                    textStatus.text = "重复"
                    textStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                }
                else -> {
                    textStatus.text = "成功"
                    textStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                }
            }

            textTime.text = timeFormat.format(Date(record.scannedAt))
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<CachedScanRecord>() {
        override fun areItemsTheSame(old: CachedScanRecord, new: CachedScanRecord) = old.id == new.id
        override fun areContentsTheSame(old: CachedScanRecord, new: CachedScanRecord) = old == new
    }
}
