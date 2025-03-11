package com.corps.healthmate.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R

class SymptomAdapter(
    private val symptoms: List<String>,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<SymptomAdapter.SymptomViewHolder>() {

    private val selectedSymptoms = mutableListOf<String>()

    inner class SymptomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val symptomText: TextView = itemView.findViewById(R.id.symptom_text)
        val symptomIcon: ImageView = itemView.findViewById(R.id.symptom_icon)

        init {
            itemView.setOnClickListener {
                val symptom = symptoms[adapterPosition]
                if (selectedSymptoms.contains(symptom)) {
                    selectedSymptoms.remove(symptom)
                    itemView.isSelected = false
                } else {
                    selectedSymptoms.add(symptom)
                    itemView.isSelected = true
                }
                onSelectionChanged(selectedSymptoms)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_symptom, parent, false)
        return SymptomViewHolder(view)
    }

    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        val symptom = symptoms[position]
        holder.symptomText.text = symptom
        holder.itemView.isSelected = selectedSymptoms.contains(symptom)
        holder.symptomIcon.setImageResource(getIconForSymptom(symptom))
    }

    override fun getItemCount(): Int = symptoms.size

    fun getSelectedSymptoms(): List<String> = selectedSymptoms.toList()

    private fun getIconForSymptom(symptom: String): Int {
        return when (symptom.lowercase()) {
            "coughing" -> R.drawable.ic_cough
            "runny nose" -> R.drawable.ic_nose
            "headache" -> R.drawable.ic_headache
            "sore throat" -> R.drawable.ic_throat
            "chills" -> R.drawable.ic_chills
            "fatigue" -> R.drawable.ic_fatigue
            "vomiting" -> R.drawable.ic_vomit
            "diarrhea" -> R.drawable.ic_diarrhea
            "chest pain" -> R.drawable.ic_chest
            "dizziness" -> R.drawable.ic_dizzy
            "nausea" -> R.drawable.ic_nausea
            "fever" -> R.drawable.ic_fever
            else -> R.drawable.ic_default_symptom
        }
    }
}