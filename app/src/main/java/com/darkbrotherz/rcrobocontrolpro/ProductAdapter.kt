package com.darkbrotherz.rcrobocontrolpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(private val productList: List<Product>) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.productImage)
        val titleView: TextView = itemView.findViewById(R.id.productTitle)
        val priceView: TextView = itemView.findViewById(R.id.productPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        holder.titleView.text = product.title
        holder.priceView.text = "Rs. ${product.price}"

        // සර්වර් එකේ සිටින පින්තූරයේ URL එක Glide හරහා Load කිරීම
        val imageUrl = "http://10.0.2.2/ceylon_shop/" + product.image_url
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = productList.size
}
