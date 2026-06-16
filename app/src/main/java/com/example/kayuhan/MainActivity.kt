package com.example.kayuhan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.kayuhan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. AMBIL DATA SESI LOGIN DARI INTENT
        // Pastikan key "EXTRA_ROLE" dan "EXTRA_EMAIL" sesuai dengan yang dikirim dari LoginActivity
        val roleLogin = intent.getStringExtra("EXTRA_ROLE")
        val emailLogin = intent.getStringExtra("EXTRA_EMAIL")

        // 2. LOGIKA ROUTING HALAMAN BERDASARKAN ROLE USER
        // Catatan: Pembandingan string ini bersifat Case-Sensitive (memperhatikan huruf besar/kecil)
        if (roleLogin.equals("barista", ignoreCase = true)) {
            // Jika user adalah barista, alihkan ke DashboardBaristaActivity (yang menggunakan Navigation Drawer)
            val intentBarista = Intent(this, DashboardBaristaActivity::class.java).apply {
                putExtra("EXTRA_EMAIL", emailLogin)
                putExtra("EXTRA_ROLE", roleLogin)
            }
            startActivity(intentBarista)
            finish() // Tutup MainActivity agar tidak bisa di-back ke halaman ini
            return
        } else if (roleLogin.equals("admin", ignoreCase = true)) {
            // Jika user adalah admin, tampilkan pesan selamat datang dan lanjutkan memuat halaman MainActivity
            Toast.makeText(this, "Selamat datang, Admin!", Toast.LENGTH_SHORT).show()
        } else {
            // Jika role null atau tidak dikenali (keamanan tambahan), kembalikan user ke halaman Login
            Toast.makeText(this, "Sesi tidak valid, silakan login kembali.", Toast.LENGTH_LONG).show()
            val intentLogin = Intent(this, LoginActivity::class.java)
            startActivity(intentLogin)
            finish()
            return
        }

        // 3. JIKA USER ADALAH ADMIN, LANJUTKAN PROSES INISIALISASI DASHBOARD UTAMA ADMIN
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi ViewPager2 dengan Adapter internal
        val adapter = MainPagerAdapter(this)
        binding.viewPagerMain.adapter = adapter

        // Sinkronisasi klik item BottomNavigationView ke ViewPager2
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itemBeranda -> binding.viewPagerMain.currentItem = 0
                R.id.itemTransaksi -> binding.viewPagerMain.currentItem = 1
                R.id.itemKaryawan -> binding.viewPagerMain.currentItem = 2
                R.id.itemMenu -> binding.viewPagerMain.currentItem = 3
                R.id.itemLokasi -> binding.viewPagerMain.currentItem = 4
            }
            true
        }

        // Sinkronisasi swipe / geser halaman ViewPager2 ke item BottomNavigationView
        binding.viewPagerMain.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })

        // Menonaktifkan auto-tint warna default Android agar ikon Bottom Navigation menggunakan warna aslinya
        binding.bottomNavigationView.itemIconTintList = null
    }

    // Adapter untuk mengatur Fragment yang tampil di dalam ViewPager2 milik Admin
    inner class MainPagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FragmentDashboardAdmin()
                1 -> FragmentTransaksi()
                2 -> FragmentKaryawan() // Memuat fragment manajemen karyawan admin
                3 -> FragmentMenu()
                4 -> FragmentLokasi()
                else -> FragmentDashboardAdmin()
            }
        }
    }
}