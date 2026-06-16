package com.example.kayuhan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import org.osmdroid.config.Configuration
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class PresensiBaristaActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var ivFoto: ImageView
    private lateinit var btnKamera: Button
    private lateinit var btnKirim: Button
    private lateinit var tvLokasi: TextView
    private lateinit var tvKoordinat: TextView

    private var bitmapFoto: Bitmap? = null
    private val REQ_CAMERA = 101
    private val REQ_LOCATION = 102
    // FIX BUG 3: Tambahkan request code untuk runtime permission kamera
    private val REQ_CAMERA_PERMISSION = 103

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val urlWebService = "http://192.168.0.32/php-mobile-kayuhan/insert_absensi.php"

    private var currentLat = 0.0
    private var currentLng = 0.0
    private var currentAddress = "Mencari alamat lokasi asli..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_presensi_barista)

        etEmail = findViewById(R.id.etEmailBarista)
        ivFoto = findViewById(R.id.ivFotoAbsen)
        btnKamera = findViewById(R.id.btnAmbilFoto)
        btnKirim = findViewById(R.id.btnKirimAbsen)
        tvLokasi = findViewById(R.id.tvLokasi)
        tvKoordinat = findViewById(R.id.tvKoordinat)

        val emailLogin = intent.getStringExtra("EXTRA_EMAIL")
        if (!emailLogin.isNullOrEmpty()) {
            etEmail.setText(emailLogin)
            etEmail.isEnabled = false
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ambilLokasiGPS()

        btnKamera.setOnClickListener {
            // FIX BUG 3: Cek dan minta izin kamera secara runtime sebelum membuka kamera
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQ_CAMERA_PERMISSION
                )
            } else {
                bukaKamera()
            }
        }

        btnKirim.setOnClickListener {
            prosesSimpanAbsensi()
        }
    }

    // FIX BUG 3: Fungsi terpisah untuk membuka kamera
    private fun bukaKamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQ_CAMERA)
        } else {
            Toast.makeText(this, "Aplikasi Kamera Tidak Ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ambilLokasiGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
                tvKoordinat.text = "Koordinat: Lat: $currentLat, Lng: $currentLng"

                thread {
                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(currentLat, currentLng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            currentAddress = addresses[0].getAddressLine(0) ?: "Alamat tidak spesifik"
                        } else {
                            currentAddress = "Koordinat Valid: $currentLat, $currentLng"
                        }
                        runOnUiThread {
                            tvLokasi.text = "Lokasi: $currentAddress"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            tvLokasi.text = "Lokasi: Gagal mengambil nama jalan (Gunakan Data GPS)"
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Gagal mengunci sinyal GPS. Aktifkan lokasi di pengaturan HP anda.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ambilLokasiGPS()
                }
            }
            // FIX BUG 3: Handle hasil permission kamera
            REQ_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bukaKamera()
                } else {
                    Toast.makeText(this, "Izin kamera diperlukan untuk absensi foto.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAMERA && resultCode == Activity.RESULT_OK) {
            bitmapFoto = data?.extras?.get("data") as Bitmap
            ivFoto.setImageBitmap(bitmapFoto)
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun prosesSimpanAbsensi() {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            etEmail.error = "Email wajib diisi untuk identifikasi"
            return
        }
        if (bitmapFoto == null) {
            Toast.makeText(this, "Harap ambil foto wajah terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val stringFotoBase64 = encodeBitmapToBase64(bitmapFoto!!)
        val usernamePakeAbsen = email.split("@")[0]
        val namaFileGambar = "absen_${usernamePakeAbsen}_${System.currentTimeMillis()}.jpg"

        thread {
            try {
                val url = URL(urlWebService)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000

                val postData = "email=" + URLEncoder.encode(email, "UTF-8") +
                        "&foto_datang=" + URLEncoder.encode(stringFotoBase64, "UTF-8") +
                        "&file=" + URLEncoder.encode(namaFileGambar, "UTF-8") +
                        "&lokasi_datang=" + URLEncoder.encode(currentAddress, "UTF-8") +
                        "&lat_datang=" + currentLat.toString() +
                        "&lng_datang=" + currentLng.toString()

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData)
                writer.flush()
                writer.close()

                val responseText = conn.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(responseText)

                val kode = jsonResponse.getString("kode")
                val pesan = jsonResponse.getString("pesan")

                runOnUiThread {
                    if (kode == "000") {
                        Toast.makeText(this, "Sukses: $pesan", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal: $pesan", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Gangguan Koneksi Jaringan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}