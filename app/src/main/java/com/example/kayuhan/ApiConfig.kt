package com.example.kayuhan

object ApiConfig {

    // ============================================================
    // GANTI IP DI SINI SAJA JIKA PINDAH JARINGAN WIFI
    // ============================================================
    private const val IP_ADDRESS = "192.168.255.177"

    // Base URL untuk API PHP
    private const val BASE_URL = "http://$IP_ADDRESS/php-mobile-kayuhan/"

    // Base URL untuk Web POS (Laravel)
    private const val BASE_URL_WEB = "http://$IP_ADDRESS/KayuhanUAS/public/"

    // ─── Endpoint API ──────────────────────────────────────────
    val LOGIN           = "${BASE_URL}login.php"
    val DASHBOARD_STATS = "${BASE_URL}dashboard_stats.php"
    val KARYAWAN        = "${BASE_URL}karyawan_action.php"
    val JABATAN         = "${BASE_URL}jabatan_action.php"
    val LOKASI          = "${BASE_URL}lokasi_action.php"
    val ROMBONG         = "${BASE_URL}rombong_action.php"
    val MENU            = "${BASE_URL}menu_action.php"
    val TRANSAKSI       = "${BASE_URL}transaksi_action.php"
    val GAJI            = "${BASE_URL}gaji_action.php"
    val JADWAL          = "${BASE_URL}query_jadwal.php"
    val ABSENSI_INSERT  = "${BASE_URL}insert_absensi.php"
    val ABSENSI_GET     = "${BASE_URL}get_absensi.php"
    val GET_MENU        = "${BASE_URL}get_menu.php"
    val INSERT_TRANSAKSI = "${BASE_URL}insert_transaksi.php"

    // ─── Web POS ───────────────────────────────────────────────
    val POS_WEB         = "${BASE_URL_WEB}barista/pos"
}
