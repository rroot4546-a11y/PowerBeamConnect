package com.roox.powerbeam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val frequency: Int,
    val level: Int,
    val capabilities: String,
    val isPowerBeam: Boolean
)

class WifiAdapter(
    private val networks: List<WifiScanResult>,
    private val onItemClick: (WifiScanResult) -> Unit
) : RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {

    inner class WifiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardNetwork)
        val tvSsid: TextView = view.findViewById(R.id.tvSsid)
        val tvFreq: TextView = view.findViewById(R.id.tvFreq)
        val tvSignal: TextView = view.findViewById(R.id.tvSignal)
        val tvLock: TextView = view.findViewById(R.id.tvLock)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_network, parent, false)
        return WifiViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        val network = networks[position]
        
        holder.tvSsid.text = network.ssid
        holder.tvFreq.text = "${network.frequency / 1000} GHz"
        holder.tvSignal.text = getSignalEmoji(network.level)
        
        // Lock icon
        holder.tvLock.visibility = if (network.capabilities.contains("WPA") || network.capabilities.contains("WEP")) 
            View.VISIBLE else View.GONE
        
        // PowerBeam highlight
        if (network.isPowerBeam) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.powerbeam_bg))
            holder.ivIcon.setImageResource(R.drawable.ic_satellite)
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.card_bg))
            holder.ivIcon.setImageResource(R.drawable.ic_wifi)
        }
        
        holder.itemView.setOnClickListener { onItemClick(network) }
    }

    override fun getItemCount() = networks.size

    private fun getSignalEmoji(level: Int): String {
        return when {
            level >= -50 -> "📶"  // Excellent
            level >= -60 -> "📶"  // Good
            level >= -70 -> "📵"  // Fair
            level >= -80 -> "📴"  // Weak
            else -> "❌"          // Very weak
        }
    }
}
