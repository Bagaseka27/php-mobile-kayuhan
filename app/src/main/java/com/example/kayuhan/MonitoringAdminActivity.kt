package com.example.kayuhan

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MonitoringAdminActivity : AppCompatActivity() {

    private lateinit var rvMonitoring: RecyclerView
    private lateinit var adapterAbsen: AbsensiAdapter
    private val dataListAbsen = ArrayList<HashMap<String, String>>()

    // Sesuaikan IP 10.0.2.2 dengan IP server local laptopmu
    private val urlGetAbsen = "http://192.168.0.32/kayuhanmobile/get_absensi.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring_admin)

        rvMonitoring = findViewById(R.id.rvMonitoringAbsen)
        rvMonitoring.layoutManager = LinearLayoutManager(this)
        adapterAbsen = AbsensiAdapter(dataListAbsen)
        rvMonitoring.adapter = adapterAbsen

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
                    map["email"] = obj.getString("email")
                    map["datetime_datang"] = obj.getString("datetime_datang")
                    map["lokasi_datang"] = obj.getString("lokasi_datang")
                    map["url_foto"] = obj.getString("url_foto")
                    dataListAbsen.add(map)
                }

                runOnUiThread {
                    adapterAbsen.notifyDataSetChanged()
                    if (dataListAbsen.isEmpty()) {
                        Toast.makeText(this, "Belum ada riwayat absensi masuk", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Gagal mengambil data dari server: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}