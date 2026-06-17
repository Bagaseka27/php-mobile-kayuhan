package com.example.kayuhan

import android.content.Intent
import android.os.Bundle
import android.view.View
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
                R.id.itemBeranda -> {
                    binding.viewPagerMain.currentItem = 0
                    true
                }
                R.id.itemKaryawan -> {
                    binding.viewPagerMain.currentItem = 1
                    true
                }
                R.id.itemAbsensi -> {
                    binding.viewPagerMain.currentItem = 2
                    true
                }
                R.id.itemMenu -> {
                    binding.viewPagerMain.currentItem = 3
                    true
                }
                R.id.itemMore -> {
                    showMoreMenu()
                    false
                }
                else -> false
            }
        }

        // Sinkronisasi swipe / geser halaman ViewPager2 ke item BottomNavigationView
        binding.viewPagerMain.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.bottomNavigationView.menu.findItem(R.id.itemBeranda).isChecked = true
                    1 -> binding.bottomNavigationView.menu.findItem(R.id.itemKaryawan).isChecked = true
                    2 -> binding.bottomNavigationView.menu.findItem(R.id.itemAbsensi).isChecked = true
                    3 -> binding.bottomNavigationView.menu.findItem(R.id.itemMenu).isChecked = true
                    4, 5 -> binding.bottomNavigationView.menu.findItem(R.id.itemMore).isChecked = true
                }
            }
        })

        // Menonaktifkan auto-tint warna default Android agar ikon Bottom Navigation menggunakan warna aslinya
        binding.bottomNavigationView.itemIconTintList = null
    }

    private fun showMoreMenu() {
        val view = binding.bottomNavigationView.findViewById<View>(R.id.itemMore)
        val popup = androidx.appcompat.widget.PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.more_option_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.subLokasi -> {
                    binding.viewPagerMain.currentItem = 4
                    true
                }
                R.id.subTransaksi -> {
                    binding.viewPagerMain.currentItem = 5
                    true
                }
                R.id.subLogout -> {
                    val intentLogout = Intent(this, LoginActivity::class.java)
                    intentLogout.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intentLogout)
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Adapter untuk mengatur Fragment yang tampil di dalam ViewPager2 milik Admin
    inner class MainPagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 6

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FragmentDashboardAdmin()
                1 -> FragmentKaryawan()
                2 -> FragmentMonitoringAbsen()
                3 -> FragmentMenu()
                4 -> FragmentLokasi()
                5 -> FragmentTransaksi()
                else -> FragmentDashboardAdmin()
            }
        }
    }
}