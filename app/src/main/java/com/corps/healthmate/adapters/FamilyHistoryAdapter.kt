package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.databinding.ItemFamilyHistoryBinding

class FamilyHistoryAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<FamilyHistoryAdapter.ViewHolder>() {

    private val items = mutableListOf<Map<String, String>>()

    class ViewHolder(private val binding: ItemFamilyHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Map<String, String>, onRemoveClick: (Int) -> Unit) {
            binding.relationText.text = item["relation"]
            binding.conditionText.text = item["condition"]
            binding.detailsText.text = item["details"]
            binding.removeButton.setOnClickListener { onRemoveClick(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFamilyHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onRemoveClick)
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Map<String, String>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
} 