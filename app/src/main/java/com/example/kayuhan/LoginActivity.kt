package com.example.kayuhan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    private val urlLogin = "http://192.168.0.32/kayuhanmobile/login.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            prosesLogin()
        }
    }

    private fun prosesLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email tidak boleh kosong"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password tidak boleh kosong"
            return
        }

        thread {
            try {
                val url = URL(urlLogin)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val postData = "email=" + URLEncoder.encode(email, "UTF-8") +
                        "&password=" + URLEncoder.encode(password, "UTF-8")

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData)
                writer.flush()
                writer.close()

                // FIX BUG 2: Baca raw response dulu untuk debugging
                val responseCode = conn.responseCode
                val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Error: HTTP $responseCode"
                }

                Log.d("LOGIN_DEBUG", "Response Code: $responseCode")
                Log.d("LOGIN_DEBUG", "Raw Response: $responseText")

                // FIX BUG 2: Cari JSON murni di dalam response (skip PHP warning/notice jika ada)
                val jsonStart = responseText.indexOf('{')
                val jsonEnd = responseText.lastIndexOf('}')

                if (jsonStart == -1 || jsonEnd == -1) {
                    runOnUiThread {
                        Toast.makeText(this, "Response tidak valid dari server. Cek Logcat tag LOGIN_DEBUG.", Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }

                val cleanJson = responseText.substring(jsonStart, jsonEnd + 1)
                val jsonResponse = JSONObject(cleanJson)

                // FIX BUG 2: Gunakan optString agar tidak crash jika key tidak ada
                val status = jsonResponse.optString("status", "false")
                val pesan = jsonResponse.optString("pesan", "Tidak ada pesan dari server")

                runOnUiThread {
                    if (status == "true") {
                        Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()

                        val role = jsonResponse.optString("role", "barista")
                        val userEmail = jsonResponse.optString("email", email)

                        // AMBIL ALIH LOGIKA ROUTING KE DASHBOARD MASING-MASING ROLE
                        if (role.equals("admin", ignoreCase = true)) {
                            // Admin diarahkan ke MainActivity (Dashboard Utama Admin)
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("EXTRA_EMAIL", userEmail)
                            intent.putExtra("EXTRA_ROLE", "admin")
                            startActivity(intent)
                        } else {
                            // Barista diarahkan ke DashboardBaristaActivity (Dashboard Utama Barista)
                            val intent = Intent(this, DashboardBaristaActivity::class.java)
                            intent.putExtra("EXTRA_EMAIL", userEmail)
                            intent.putExtra("EXTRA_ROLE", "barista")
                            startActivity(intent)
                        }
                        finish() // Tutup LoginActivity agar tidak bisa di-back
                    } else {
                        Toast.makeText(this, "Gagal: $pesan", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("LOGIN_DEBUG", "Exception: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Terjadi gangguan jaringan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}