package org.vitrivr.vitrivrapp.features.results

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import org.vitrivr.vitrivrapp.R

/**
 * This weight adjustment adapter is used in the query refinement drawer to adjust the weights of different categories
 * categoryWeight stores the category and its set weight by the user.
 */
class WeightAdjustmentAdapter(val categoryWeight: HashMap<String, Double>) : RecyclerView.Adapter<WeightAdjustmentAdapter.WeightAdjustmentViewHolder>() {

    /**
     * Corresponding view holder
     */
    class WeightAdjustmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.category)
        val weight: TextView = view.findViewById(R.id.weight)
        val weightAdjustment: SeekBar = view.findViewById(R.id.weightAdjustment)
    }

    /**
     * list of all the categories
     */
    private val categoryList = categoryWeight.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightAdjustmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.weight_adjustment_item, parent, false)
        return WeightAdjustmentViewHolder(view)
    }

    override fun getItemCount() = categoryList.size

    override fun onBindViewHolder(holder: WeightAdjustmentViewHolder, position: Int) {
        val progress = (categoryWeight[categoryList[position]]!! * 100).toInt()

        holder.category.text = categoryList[position]
        holder.weight.text = "$progress%"
        holder.weightAdjustment.progress = progress
        holder.weightAdjustment.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                categoryWeight[categoryList[position]] = progress / 100.0
                holder.weight.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


}