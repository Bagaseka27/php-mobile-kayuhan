package com.example.kayuhan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.*

class CartAdapter(
    private val items: MutableList<KasirActivity.CartItem>,
    private val onQtyChanged: (KasirActivity.CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ID di bawah ini disesuaikan 100% dengan file item_cart.xml kamu!
        val tvNama: TextView = view.findViewById(R.id.tvCartItemName)
        val tvSubtotal: TextView = view.findViewById(R.id.tvCartItemSubtotal)
        val tvHargaTotalItem: TextView = view.findViewById(R.id.tvCartItemPrice)
        val tvQty: TextView = view.findViewById(R.id.tvCartItemQty)
        val btnIncrease: ImageButton = view.findViewById(R.id.btnCartIncrease)
        val btnDecrease: ImageButton = view.findViewById(R.id.btnCartDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        holder.tvNama.text = item.namaProduk

        // Menampilkan teks formatting: "1 x Rp 15.000" sesuai desain awal XML kamu
        val hargaFormatted = formatRupiah.format(item.hargaJual).replace("Rp", "Rp ")
        holder.tvSubtotal.text = "${item.qty} x $hargaFormatted"

        // Menampilkan total harga akumulasi item
        holder.tvHargaTotalItem.text = formatRupiah.format(item.hargaJual * item.qty).replace("Rp", "Rp ")

        holder.tvQty.text = item.qty.toString()

        holder.btnIncrease.setOnClickListener {
            item.qty++
            holder.tvQty.text = item.qty.toString()
            holder.tvSubtotal.text = "${item.qty} x $hargaFormatted"
            holder.tvHargaTotalItem.text = formatRupiah.format(item.hargaJual * item.qty).replace("Rp", "Rp ")
            onQtyChanged(item, item.qty)
        }

        holder.btnDecrease.setOnClickListener {
            item.qty--
            if (item.qty <= 0) {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    items.removeAt(currentPosition)
                    notifyItemRemoved(currentPosition)
                    notifyItemRangeChanged(currentPosition, items.size)
                    onQtyChanged(item, 0)
                }
            } else {
                holder.tvQty.text = item.qty.toString()
                holder.tvSubtotal.text = "${item.qty} x $hargaFormatted"
                holder.tvHargaTotalItem.text = formatRupiah.format(item.hargaJual * item.qty).replace("Rp", "Rp ")
                onQtyChanged(item, item.qty)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}