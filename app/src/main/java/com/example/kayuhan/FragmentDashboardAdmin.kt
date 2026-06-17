package com.example.kayuhan

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kayuhan.databinding.ActivityFragmentDashboardAdminBinding
import java.text.NumberFormat
import java.util.Locale

class FragmentDashboardAdmin : Fragment() {

    private var vb: ActivityFragmentDashboardAdminBinding? = null
    private val binding get() = vb!!
    private val apiUrl = ApiConfig.DASHBOARD_STATS

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        vb = ActivityFragmentDashboardAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        loadDashboardStats()
    }

    private fun loadDashboardStats() {
        val email = when (val act = activity) {
            is DashboardBaristaActivity -> act.intent.getStringExtra("EXTRA_EMAIL") ?: ""
            is MainActivity -> act.intent.getStringExtra("EXTRA_EMAIL") ?: ""
            else -> ""
        }
        val role = when (activity) {
            is DashboardBaristaActivity -> "barista"
            is MainActivity -> "admin"
            else -> "admin"
        }

        val params = mapOf(
            "role" to role,
            "email" to email
        )

        postKeServer(requireContext(), apiUrl, params) { response ->
            try {
                val json = org.json.JSONObject(response)
                if (json.getString("status") == "success") {
                    val data = json.getJSONObject("data")
                    if (role == "barista") {
                        binding.tvDashboardGreeting.text = "Selamat datang kembali,"
                        binding.tvDashboardTitle.text = data.optString("nama_barista", "Barista")
                        
                        binding.tvLabelKaryawan.text = "Item Terjual Hari Ini"
                        binding.tvJumlahKaryawan.text = "${data.optInt("total_items_terjual", 0)} Cup"
                        
                        binding.tvLabelGaji.text = "Lokasi Rombong"
                        binding.tvTotalGaji.text = data.optString("lokasi_kerja", "-")
                        
                        binding.tvLabelMenu.text = "Jam Kerja Shift"
                        binding.tvJumlahMenu.text = data.optString("jam_kerja", "-")
                        
                        binding.tvLabelOmset.text = "Penjualan Shift Ini"
                        binding.tvTotalOmset.text = formatKeRupiah(data.optDouble("penjualan_shift_ini", 0.0))
                        
                        binding.cardMenuTerjual.visibility = View.VISIBLE
                        binding.tvMenuTerjualHariIni.text = data.optString("menu_terjual_list", "Belum ada menu terjual.")
                    } else {
                        binding.tvDashboardGreeting.text = "Selamat Datang di"
                        binding.tvDashboardTitle.text = "Dashboard Admin"
                        
                        binding.tvLabelKaryawan.text = "Banyak Karyawan"
                        binding.tvJumlahKaryawan.text = data.getInt("jumlah_karyawan").toString()
                        
                        binding.tvLabelGaji.text = "Gaji Karyawan"
                        binding.tvTotalGaji.text = formatKeRupiah(data.getDouble("total_gaji_per_jam")) + " /jam"
                        
                        binding.tvLabelMenu.text = "Banyak Menu"
                        binding.tvJumlahMenu.text = data.getInt("jumlah_menu").toString()
                        
                        binding.tvLabelOmset.text = "Omset Bulan Ini"
                        binding.tvTotalOmset.text = formatKeRupiah(data.getDouble("total_omset"))
                        
                        binding.cardMenuTerjual.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatKeRupiah(angka: Double): String {
        val localeID = java.util.Locale("in", "ID")
        val format = java.text.NumberFormat.getCurrencyInstance(localeID)
        return format.format(angka).replace(",00", "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vb = null
    }
}