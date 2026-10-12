package com.darkbrotherz.rcrobocontrolpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StoreAdapter(
    private var productList: List<Product>,
    private val onItemClick: (Product) -> Unit,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    fun updateData(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }

    class StoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val txtProductName: TextView = view.findViewById(R.id.txtProductName)
        val txtProductDescription: TextView = view.findViewById(R.id.txtProductDescription)
        val txtProductPrice: TextView = view.findViewById(R.id.txtProductPrice)
        val btnAddToCart: Button = view.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store_product, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val product = productList[position]
        holder.txtProductName.text = product.title
        holder.txtProductDescription.text = product.description
        holder.txtProductPrice.text = product.price

        // Construct full URL for images from server
        val imageUrl = if (product.image_url.startsWith("http")) {
            product.image_url
        } else {
            "https://discount-purposely-rejoin.ngrok-free.dev/ceylon_shop_db/" + product.image_url
        }

        android.util.Log.d("STORE_IMAGE", "Loading image: $imageUrl")

        Glide.with(holder.imgProduct.context)
            .load(imageUrl)
            .centerCrop()
            .into(holder.imgProduct)

        holder.itemView.setOnClickListener { onItemClick(product) }
        holder.btnAddToCart.setOnClickListener { onAddToCart(product) }
    }

    override fun getItemCount(): Int = productList.size
}
