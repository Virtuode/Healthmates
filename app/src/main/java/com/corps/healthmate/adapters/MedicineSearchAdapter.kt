package com.corps.healthmate.adapters

import com.corps.healthmate.models.Medicine
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.databinding.ItemMedicineSearchBinding
import java.util.*


class MedicineSearchAdapter(
    private val onMedicineClick: (Medicine) -> Unit,
    private val onLoadMore: () -> Unit
) : ListAdapter<Medicine, MedicineSearchAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    private var isLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = ItemMedicineSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Check if we need to load more items
        if (position == itemCount - 5 && !isLoading) {
            isLoading = true
            onLoadMore()
        }
    }

    fun setLoading(loading: Boolean) {
        isLoading = loading
    }

    inner class MedicineViewHolder(
        private val binding: ItemMedicineSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMedicineClick(getItem(position))
                }
            }
        }

        fun bind(medicine: Medicine) {
            // Only bind the medicine name
            binding.medicineName.text = medicine.name.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
        }
    }

    private class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine) =
            oldItem == newItem
    }
} 