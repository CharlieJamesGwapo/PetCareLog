package com.petcare.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.petcare.log.databinding.ItemPetBinding

class PetAdapter(private val onClick: (Pet) -> Unit) : RecyclerView.Adapter<PetAdapter.VH>() {

    private var items: List<Pet> = emptyList()

    fun submit(list: List<Pet>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemPetBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pet = items[position]
        holder.b.avatar.setImageResource(petIcon(pet.type))
        holder.b.petName.text = pet.name
        var summary = "${pet.type} · ${agePlural(pet.age)}"
        if (pet.nextVetVisit.isNotEmpty()) summary += " · Vet ${isoToDisplay(pet.nextVetVisit)}"
        holder.b.petSummary.text = summary
        holder.b.root.setOnClickListener { onClick(pet) }
    }
}
