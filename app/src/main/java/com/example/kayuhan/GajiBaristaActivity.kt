package com.example.kayuhan

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class GajiBaristaActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvSisaGajiHarian: TextView
    private lateinit var tvTotalTabungan: TextView
    private lateinit var tvSisaGajiTotal: TextView
    private lateinit var btnSimpanGaji: View
    private lateinit var btnAmbilGaji: View
    private lateinit var rvRiwayat: RecyclerView

    private var emailLogin: String = ""
    private var sisaGajiHarian: Double = 0.0
    private var totalTabungan: Double = 0.0
    private var sisaGajiTotal: Double = 0.0

    private val apiUrl = "http://192.168.0.109/php-mobile-kayuhan/gaji_action.php"
    private val riwayatList = ArrayList<RiwayatGajiItem>()
    private lateinit var adapter: RiwayatGajiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaji_barista)

        toolbar = findViewById(R.id.toolbarGaji)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        tvSisaGajiHarian = findViewById(R.id.tvSisaGajiHarian)
        tvTotalTabungan = findViewById(R.id.tvTotalTabungan)
        tvSisaGajiTotal = findViewById(R.id.tvSisaGajiTotal)
        btnSimpanGaji = findViewById(R.id.btnSimpanGaji)
        btnAmbilGaji = findViewById(R.id.btnAmbilGaji)
        rvRiwayat = findViewById(R.id.rvRiwayatGaji)

        emailLogin = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        if (emailLogin.isEmpty()) {
            Toast.makeText(this, "Sesi tidak valid, silakan login kembali.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        rvRiwayat.layoutManager = LinearLayoutManager(this)
        adapter = RiwayatGajiAdapter(riwayatList)
        rvRiwayat.adapter = adapter

        btnSimpanGaji.setOnClickListener {
            prosesSimpanGaji()
        }

        btnAmbilGaji.setOnClickListener {
            prosesAmbilGaji()
        }

        muatDataGaji()
    }

    private fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return "Rp " + formatter.format(amount)
    }

    private fun muatDataGaji() {
        val params = mapOf(
            "action" to "barista_info",
            "email" to emailLogin
        )

        postKeServer(this, apiUrl, params) { response ->
            try {
                Log.d("GAJI_DEBUG", "barista_info response: $response")
                val jsonStart = response.indexOf('{')
                val jsonEnd = response.lastIndexOf('}')
                if (jsonStart != -1 && jsonEnd != -1) {
                    val cleanJson = response.substring(jsonStart, jsonEnd + 1)
                    val json = JSONObject(cleanJson)
                    if (json.optString("status") == "success") {
                        sisaGajiHarian = json.optDouble("sisa_gaji_harian", 0.0)
                        totalTabungan = json.optDouble("total_tabungan", 0.0)
                        sisaGajiTotal = json.optDouble("sisa_gaji", 0.0)

                        tvSisaGajiHarian.text = formatRupiah(sisaGajiHarian)
                        tvTotalTabungan.text = formatRupiah(totalTabungan)
                        tvSisaGajiTotal.text = formatRupiah(sisaGajiTotal)

                        btnSimpanGaji.isEnabled = sisaGajiHarian > 0
                        btnAmbilGaji.isEnabled = sisaGajiTotal > 0
                    } else {
                        Toast.makeText(this, "Gagal memuat info gaji", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        muatRiwayatGaji()
    }

    private fun muatRiwayatGaji() {
        val params = mapOf(
            "action" to "riwayat_gaji",
            "email" to emailLogin
        )

        postKeServer(this, apiUrl, params) { response ->
            try {
                Log.d("GAJI_DEBUG", "riwayat response: $response")
                val jsonStart = response.indexOf('{')
                val jsonEnd = response.lastIndexOf('}')
                if (jsonStart != -1 && jsonEnd != -1) {
                    val cleanJson = response.substring(jsonStart, jsonEnd + 1)
                    val json = JSONObject(cleanJson)
                    if (json.optString("status") == "success") {
                        riwayatList.clear()
                        val dataArray = json.optJSONArray("data") ?: JSONArray()
                        for (i in 0 until dataArray.length()) {
                            val itemObj = dataArray.getJSONObject(i)
                            val item = RiwayatGajiItem(
                                itemObj.optString("tanggal"),
                                itemObj.optDouble("nominal", 0.0),
                                itemObj.optString("status"),
                                itemObj.optString("tipe"),
                                itemObj.optString("catatan", "")
                            )
                            riwayatList.add(item)
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prosesSimpanGaji() {
        if (sisaGajiHarian <= 0) {
            Toast.makeText(this, "Tidak ada sisa gaji harian yang dapat disimpan", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Simpan Gaji")
            .setMessage("Apakah Anda yakin ingin menyimpan seluruh sisa gaji hari ini sebesar ${formatRupiah(sisaGajiHarian)} ke Tabungan?")
            .setPositiveButton("Simpan") { _, _ ->
                val params = mapOf(
                    "action" to "simpan_gaji",
                    "email" to emailLogin
                )

                postKeServer(this, apiUrl, params) { response ->
                    try {
                        Log.d("GAJI_DEBUG", "simpan response: $response")
                        val jsonStart = response.indexOf('{')
                        val jsonEnd = response.lastIndexOf('}')
                        if (jsonStart != -1 && jsonEnd != -1) {
                            val cleanJson = response.substring(jsonStart, jsonEnd + 1)
                            val json = JSONObject(cleanJson)
                            val status = json.optString("status")
                            val message = json.optString("message", "Terjadi kesalahan")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            if (status == "success") {
                                muatDataGaji()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun prosesAmbilGaji() {
        if (sisaGajiTotal <= 0) {
            Toast.makeText(this, "Anda tidak memiliki saldo gaji yang bisa diambil", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ambil Gaji")
        builder.setMessage("Masukkan nominal pengambilan (Maksimal: ${formatRupiah(sisaGajiTotal)})")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Contoh: 50000"

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val margin16 = (16 * resources.displayMetrics.density).toInt()
        lp.setMargins(margin16, 8, margin16, 8)
        input.layoutParams = lp
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Kirim") { _, _ ->
            val nominalStr = input.text.toString().trim()
            if (nominalStr.isNotEmpty()) {
                val nominal = nominalStr.toDoubleOrNull() ?: 0.0
                if (nominal <= 0) {
                    Toast.makeText(this, "Nominal tidak valid!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (nominal > sisaGajiTotal) {
                    Toast.makeText(this, "Nominal melebihi saldo tersedia!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val params = mapOf(
                    "action" to "ambil_gaji",
                    "email" to emailLogin,
                    "nominal" to nominalStr
                )

                postKeServer(this, apiUrl, params) { response ->
                    try {
                        Log.d("GAJI_DEBUG", "ambil response: $response")
                        val jsonStart = response.indexOf('{')
                        val jsonEnd = response.lastIndexOf('}')
                        if (jsonStart != -1 && jsonEnd != -1) {
                            val cleanJson = response.substring(jsonStart, jsonEnd + 1)
                            val json = JSONObject(cleanJson)
                            val status = json.optString("status")
                            val message = json.optString("message", "Terjadi kesalahan")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            if (status == "success") {
                                muatDataGaji()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Nominal tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    data class RiwayatGajiItem(
        val tanggal: String,
        val nominal: Double,
        val status: String,
        val tipe: String,
        val catatan: String
    )

    class RiwayatGajiAdapter(private val list: List<RiwayatGajiItem>) :
        RecyclerView.Adapter<RiwayatGajiAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivTipeIcon: ImageView = view.findViewById(R.id.ivTipeIcon)
            val tvRiwayatTipe: TextView = view.findViewById(R.id.tvRiwayatTipe)
            val tvRiwayatTanggal: TextView = view.findViewById(R.id.tvRiwayatTanggal)
            val tvRiwayatNominal: TextView = view.findViewById(R.id.tvRiwayatNominal)
            val tvRiwayatStatus: TextView = view.findViewById(R.id.tvRiwayatStatus)
            val tvRiwayatCatatan: TextView = view.findViewById(R.id.tvRiwayatCatatan)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_riwayat_gaji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]

            // Format nominal rupiah
            val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
            holder.tvRiwayatNominal.text = "Rp " + formatter.format(item.nominal)

            holder.tvRiwayatTanggal.text = item.tanggal

            // Custom UI based on Type
            if (item.tipe.lowercase() == "penyimpanan") {
                holder.tvRiwayatTipe.text = "Penyimpanan Gaji"
                holder.ivTipeIcon.setImageResource(R.drawable.ic_gaji)
                holder.ivTipeIcon.setColorFilter(Color.parseColor("#E67E22"))
            } else {
                holder.tvRiwayatTipe.text = "Pengambilan Gaji"
                holder.ivTipeIcon.setImageResource(R.drawable.ic_history)
                holder.ivTipeIcon.setColorFilter(Color.parseColor("#2980B9"))
            }

            // Custom status badge
            val colorString = when (item.status.lowercase()) {
                "menunggu" -> "#F39C12"
                "disetujui" -> "#27AE60"
                "ditolak" -> "#C0392B"
                else -> "#7F8C8D"
            }
            val color = Color.parseColor(colorString)
            val gd = GradientDrawable()
            gd.setColor(color)
            gd.cornerRadius = 12f
            holder.tvRiwayatStatus.background = gd
            holder.tvRiwayatStatus.text = item.status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }

            if (item.catatan.isNotEmpty() && item.catatan != "null" && item.catatan != "-") {
                holder.tvRiwayatCatatan.text = "Catatan: ${item.catatan}"
                holder.tvRiwayatCatatan.visibility = View.VISIBLE
            } else {
                holder.tvRiwayatCatatan.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
