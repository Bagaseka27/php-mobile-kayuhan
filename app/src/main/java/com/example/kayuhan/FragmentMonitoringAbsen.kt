package com.example.kayuhan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FragmentMonitoringAbsen : Fragment() {

    private lateinit var rvMonitoring: RecyclerView
    private lateinit var adapterAbsen: AbsensiAdapter
    private val dataListAbsen = ArrayList<HashMap<String, String>>()
    private val urlGetAbsen = ApiConfig.ABSENSI_GET

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_monitoring_admin, container, false)
        
        rvMonitoring = view.findViewById(R.id.rvMonitoringAbsen)
        rvMonitoring.layoutManager = LinearLayoutManager(requireContext())
        adapterAbsen = AbsensiAdapter(dataListAbsen)
        rvMonitoring.adapter = adapterAbsen

        return view
    }

    override fun onStart() {
        super.onStart()
        muatDataAbsensiDariServer()
    }

    private fun muatDataAbsensiDariServer() {
        thread {
            try {
                val url = URL(urlGetAbsen)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000

                val responseText = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(responseText)

                dataListAbsen.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val map = HashMap<String, String>()
                    // Ambil dengan toleransi huruf besar/kecil (case-insensitive)
                    map["email"] = obj.optString("EMAIL", obj.optString("email", ""))
                    map["datetime_datang"] = obj.optString("DATETIME_DATANG", obj.optString("datetime_datang", ""))
                    map["lokasi_datang"] = obj.optString("LOKASI_DATANG", obj.optString("lokasi_datang", ""))
                    map["url_foto"] = obj.optString("url_foto", "")
                    dataListAbsen.add(map)
                }

                activity?.runOnUiThread {
                    adapterAbsen.notifyDataSetChanged()
                    if (dataListAbsen.isEmpty()) {
                        Toast.makeText(requireContext(), "Belum ada riwayat absensi masuk", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Gagal mengambil data dari server: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
