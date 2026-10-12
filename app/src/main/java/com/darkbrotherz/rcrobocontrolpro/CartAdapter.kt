package com.darkbrotherz.rcrobocontrolpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(private val cartList: List<Product>) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(android.R.id.text1)
        val txtPrice: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = cartList[position]
        holder.txtName.text = product.title
        holder.txtPrice.text = product.price
        holder.txtName.setTextColor(0xFFFFFFFF.toInt())
        holder.txtPrice.setTextColor(0x80FFFFFF.toInt())
    }

    override fun getItemCount(): Int = cartList.size
}
