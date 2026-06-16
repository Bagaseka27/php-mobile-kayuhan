<?php
include "koneksi.php";
date_default_timezone_set('Asia/Jakarta');

$response = [];
$role = $_POST['role'] ?? '';
$email = $_POST['email'] ?? '';

if (strtolower($role) === 'barista') {
    $today = date('Y-m-d');
    
    // 1. Penjualan Shift Ini
    $resPenjualan = mysqli_query($kon, "SELECT SUM(TOTAL_BAYAR) as total FROM transaksi WHERE EMAIL = '$email' AND DATE(DATETIME) = '$today'");
    $rowPenjualan = mysqli_fetch_assoc($resPenjualan);
    $response['penjualan_shift_ini'] = (double)($rowPenjualan['total'] ?? 0);
    
    // 2. Item Terjual Hari Ini
    $resItems = mysqli_query($kon, "SELECT SUM(d.JML_ITEM) as total FROM detailtransaksi d JOIN transaksi t ON d.ID_TRANSAKSI = t.ID_TRANSAKSI WHERE t.EMAIL = '$email' AND DATE(t.DATETIME) = '$today'");
    $rowItems = mysqli_fetch_assoc($resItems);
    $response['total_items_terjual'] = (int)($rowItems['total'] ?? 0);
    
    // 3. Menu Terjual Hari Ini (Breakdown)
    $resMenu = mysqli_query($kon, "SELECT m.NAMA_PRODUK, SUM(d.JML_ITEM) as total_terjual FROM detailtransaksi d JOIN transaksi t ON d.ID_TRANSAKSI = t.ID_TRANSAKSI JOIN menu m ON d.ID_PRODUK = m.ID_PRODUK WHERE t.EMAIL = '$email' AND DATE(t.DATETIME) = '$today' GROUP BY m.NAMA_PRODUK ORDER BY total_terjual DESC");
    $menuTerjual = [];
    while ($row = mysqli_fetch_assoc($resMenu)) {
        $menuTerjual[] = $row['NAMA_PRODUK'] . " (" . $row['total_terjual'] . ")";
    }
    $response['menu_terjual_list'] = empty($menuTerjual) ? "Belum ada menu terjual." : implode(", ", $menuTerjual);
    
    // 4. Jadwal Shift Hari Ini
    $resJadwal = mysqli_query($kon, "SELECT j.JAM_MULAI, j.JAM_SELESAI, c.NAMA_LOKASI FROM jadwal j LEFT JOIN cabang c ON j.ID_CABANG = c.ID_CABANG WHERE j.EMAIL = '$email' AND j.TANGGAL = '$today' LIMIT 1");
    if ($resJadwal && mysqli_num_rows($resJadwal) > 0) {
        $rowJadwal = mysqli_fetch_assoc($resJadwal);
        $response['lokasi_kerja'] = $rowJadwal['NAMA_LOKASI'] ?? 'Belum Ditentukan';
        $response['jam_kerja'] = substr($rowJadwal['JAM_MULAI'], 0, 5) . " - " . substr($rowJadwal['JAM_SELESAI'], 0, 5);
    } else {
        $response['lokasi_kerja'] = 'Libur';
        $response['jam_kerja'] = '-';
    }
    
    // Tambahkan nama barista
    $resBarista = mysqli_query($kon, "SELECT NAMA FROM karyawan WHERE EMAIL = '$email' LIMIT 1");
    $rowBarista = mysqli_fetch_assoc($resBarista);
    $response['nama_barista'] = $rowBarista['NAMA'] ?? 'Barista';
} else {
    // 1. Jumlah Karyawan (Gunakan DISTINCT Email untuk memastikan tidak ada duplikasi jika ada anomali data)
    $resKaryawan = mysqli_query($kon, "SELECT COUNT(DISTINCT email) as total FROM karyawan");
    $rowKaryawan = mysqli_fetch_assoc($resKaryawan);
    $response['jumlah_karyawan'] = (int)$rowKaryawan['total'];

    // 2. Estimasi Gaji (Budget per Jam)
    $resGaji = mysqli_query($kon, "SELECT SUM(j.UPAH_PER_JAM) as total FROM karyawan k JOIN jabatan j ON k.ID_JABATAN = j.ID_JABATAN");
    $rowGaji = mysqli_fetch_assoc($resGaji);
    $response['total_gaji_per_jam'] = (double)($rowGaji['total'] ?? 0);

    // 3. Jumlah Menu
    $resMenu = mysqli_query($kon, "SELECT COUNT(*) as total FROM menu");
    $rowMenu = mysqli_fetch_assoc($resMenu);
    $response['jumlah_menu'] = (int)$rowMenu['total'];

    // 4. Total Omset
    $resOmset = mysqli_query($kon, "SELECT SUM(TOTAL_BAYAR) as total FROM transaksi");
    $rowOmset = mysqli_fetch_assoc($resOmset);
    $response['total_omset'] = (double)($rowOmset['total'] ?? 0);
}

header('Content-Type: application/json');
echo json_encode(["status" => "success", "data" => $response]);
?>