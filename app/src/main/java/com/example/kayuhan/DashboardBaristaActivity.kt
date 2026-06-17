package com.example.kayuhan

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView

class DashboardBaristaActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_barista)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Mengatur tombol hamburger (garis tiga) di toolbar untuk buka-tutup sidebar
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        // Menangkap email lemparan dari LoginActivity untuk dipasang di Header Sidebar
        val emailLogin = intent.getStringExtra("EXTRA_EMAIL")
        val headerView = navView.getHeaderView(0)
        val tvEmailHeader = headerView.findViewById<TextView>(R.id.tvHeaderEmail)
        if (!emailLogin.isNullOrEmpty()) {
            tvEmailHeader.text = emailLogin
        }

        // Set default fragment to Dashboard
        if (savedInstanceState == null) {
            loadFragment(FragmentDashboardAdmin())
            navView.setCheckedItem(R.id.nav_dashboard)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    // Mengatur Logika Klik pada Item Menu Sidebar
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                loadFragment(FragmentDashboardAdmin())
            }
            R.id.nav_kasir -> {
                val emailLogin = intent.getStringExtra("EXTRA_EMAIL")
                val intentKasir = Intent(this, KasirActivity::class.java)
                intentKasir.putExtra("EXTRA_EMAIL", emailLogin)
                startActivity(intentKasir)
            }
            R.id.nav_menu -> {
                loadFragment(FragmentMenu())
            }
            R.id.nav_presensi -> {
                // Berpindah ke form Presensi yang kita buat kemarin
                val emailLogin = intent.getStringExtra("EXTRA_EMAIL")
                val intentPresensi = Intent(this, PresensiBaristaActivity::class.java)
                intentPresensi.putExtra("EXTRA_EMAIL", emailLogin)
                startActivity(intentPresensi)
            }
            R.id.nav_gaji -> {
                val emailLogin = intent.getStringExtra("EXTRA_EMAIL")
                val intentGaji = Intent(this, GajiBaristaActivity::class.java)
                intentGaji.putExtra("EXTRA_EMAIL", emailLogin)
                startActivity(intentGaji)
            }
            R.id.nav_riwayat -> {
                loadFragment(FragmentTransaksi())
            }
            R.id.nav_logout -> {
                // Kembali ke halaman login dan hapus tumpukan activity sebelumnya
                val intentLogout = Intent(this, LoginActivity::class.java)
                intentLogout.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intentLogout)
                finish()
            }
        }

        // Tutup kembali sidebar secara otomatis setelah menu diklik
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Logika agar saat tombol BACK di HP ditekan, sidebar menutup dulu (tidak langsung keluar aplikasi)
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}