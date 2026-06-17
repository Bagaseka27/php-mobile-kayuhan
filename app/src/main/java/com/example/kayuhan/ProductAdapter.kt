package com.example.kayuhan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val products: List<Product>,
    private val onQtyChanged: (Product, Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    data class Product(
        val id: String,
        val nama: String,
        val kategori: String,
        val harga: Int,
        var qty: Int = 0
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama    : TextView    = view.findViewById(R.id.tvProductName)
        val tvKategori: TextView    = view.findViewById(R.id.tvProductCategory)
        val tvHarga   : TextView    = view.findViewById(R.id.tvProductPrice)
        val tvQty     : TextView    = view.findViewById(R.id.tvProductQty)
        val btnPlus   : ImageButton = view.findViewById(R.id.btnProductIncrease)
        val btnMinus  : ImageButton = view.findViewById(R.id.btnProductDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_product, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = products.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.tvNama.text     = product.nama
        holder.tvKategori.text = product.kategori
        holder.tvHarga.text    = "Rp " + product.harga
        holder.tvQty.text      = product.qty.toString()

        holder.btnPlus.setOnClickListener {
            product.qty++
            holder.tvQty.text = product.qty.toString()
            onQtyChanged(product, product.qty)
        }

        holder.btnMinus.setOnClickListener {
            if (product.qty > 0) {
                product.qty--
                holder.tvQty.text = product.qty.toString()
                onQtyChanged(product, product.qty)
            }
        }
    }
}