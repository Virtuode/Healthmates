package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.data.ProfileData
import com.corps.healthmate.databinding.ItemMedicationInputBinding

class MedicationAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.ViewHolder>() {

    private val items = mutableListOf<ProfileData.CurrentHealthInfo.Medication>()

    class ViewHolder(private val binding: ItemMedicationInputBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            medication: ProfileData.CurrentHealthInfo.Medication,
            onRemoveClick: (Int) -> Unit
        ) {
            binding.apply {
                nameInput.setText(medication.name)
                dosageInput.setText(medication.dosage)
                frequencyInput.setText(medication.frequency)
                
                // Setup frequency dropdown
                val frequencies = arrayOf("Once daily", "Twice daily", "Three times daily", "As needed")
                val adapter = ArrayAdapter(root.context, android.R.layout.simple_dropdown_item_1line, frequencies)
                frequencyInput.setAdapter(adapter)
                
                removeButton.setOnClickListener { onRemoveClick(adapterPosition) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onRemoveClick)
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ProfileData.CurrentHealthInfo.Medication>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getMedications(): List<ProfileData.CurrentHealthInfo.Medication> {
        return items.toList()
    }
} 