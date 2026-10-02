package com.github.premtechworks.synqvia.ime

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.data.ClipEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipImeAdapter(
    private val onItemClick: (ClipEntity) -> Unit,
    private val onItemLongClick: (ClipEntity, View) -> Unit,
    private val onPinClick: (ClipEntity) -> Unit,
    private val onDeleteClick: (ClipEntity) -> Unit
) : RecyclerView.Adapter<ClipImeAdapter.ClipViewHolder>() {

    private var clips: List<ClipEntity> = emptyList()

    fun submitList(newClips: List<ClipEntity>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = clips.size
            override fun getNewListSize(): Int = newClips.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return clips[oldItemPosition].id == newClips[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return clips[oldItemPosition] == newClips[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.clips = newClips
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ime_clip, parent, false)
        return ClipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        holder.bind(clips[position])
    }

    override fun getItemCount(): Int = clips.size

    inner class ClipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSource: TextView = itemView.findViewById(R.id.tv_clip_source)
        val tvTime: TextView = itemView.findViewById(R.id.tv_clip_time)
        val btnPin: ImageView = itemView.findViewById(R.id.btn_pin_clip)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_clip)
        val tvText: TextView = itemView.findViewById(R.id.tv_clip_text)

        fun bind(clip: ClipEntity) {
            val context = itemView.context

            // Text preview (mask if sensitive)
            if (clip.sensitive) {
                tvText.text = context.getString(R.string.ime_sensitive_masked)
                tvText.setTextColor(Color.parseColor("#94A3B8"))
            } else {
                tvText.text = clip.text
                tvText.setTextColor(Color.WHITE)
            }

            // Direction badge
            val isRemote = clip.direction == "remote"
            if (isRemote) {
                tvSource.text = "PC"
                tvSource.setTextColor(ContextCompat.getColor(context, R.color.ime_source_pc_text))
                tvSource.setBackgroundResource(R.drawable.bg_ime_pill_pc)
            } else {
                tvSource.text = "Local"
                tvSource.setTextColor(ContextCompat.getColor(context, R.color.ime_source_local_text))
                tvSource.setBackgroundResource(R.drawable.bg_ime_pill_local)
            }

            // Relative or absolute timestamp
            tvTime.text = formatTimestamp(clip.ts)

            // Pin state (gold when pinned)
            if (clip.pinned) {
                btnPin.setImageResource(R.drawable.ic_ime_pin_filled)
                btnPin.setColorFilter(ContextCompat.getColor(context, R.color.ime_pin_gold))
                btnPin.contentDescription = context.getString(R.string.ime_pinned)
            } else {
                btnPin.setImageResource(R.drawable.ic_ime_pin)
                btnPin.setColorFilter(Color.parseColor("#64748B"))
                btnPin.contentDescription = context.getString(R.string.ime_unpinned)
            }

            btnDelete.setColorFilter(Color.parseColor("#64748B"))

            // Clicks
            itemView.setOnClickListener {
                onItemClick(clip)
            }
            itemView.setOnLongClickListener { v ->
                onItemLongClick(clip, v)
                true
            }
            btnPin.setOnClickListener {
                onPinClick(clip)
            }
            btnDelete.setOnClickListener {
                onDeleteClick(clip)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diffMs = now - timestamp
            return when {
                diffMs < 60_000L -> "Just now"
                diffMs < 3600_000L -> "${diffMs / 60_000L}m ago"
                diffMs < 86400_000L -> "${diffMs / 3600_000L}h ago"
                else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
