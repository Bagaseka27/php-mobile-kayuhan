package com.example.kayuhan

import com.bumptech.glide.Glide
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

class AbsensiAdapter(private val listAbsen: ArrayList<HashMap<String, String>>) :
    RecyclerView.Adapter<AbsensiAdapter.AbsenViewHolder>() {

    class AbsenViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmail: TextView = view.findViewById(R.id.txtItemEmail)
        val tvWaktu: TextView = view.findViewById(R.id.txtItemWaktu)
        val tvLokasi: TextView = view.findViewById(R.id.txtItemLokasi)
        val imgFoto: ImageView = view.findViewById(R.id.imgItemFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_absensi, parent, false)
        return AbsenViewHolder(view)
    }

    override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
        val item = listAbsen[position]
        holder.tvEmail.text = item["email"]
        holder.tvWaktu.text = "Waktu: ${item["datetime_datang"]}"
        holder.tvLokasi.text = "Lokasi: ${item["lokasi_datang"]}"


        val urlFotoLengkap = item["url_foto"]

        if (!urlFotoLengkap.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(urlFotoLengkap)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.imgFoto)
        } else {
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount(): Int = listAbsen.size
}