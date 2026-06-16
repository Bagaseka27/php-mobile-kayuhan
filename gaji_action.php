<?php
include "koneksi.php";
date_default_timezone_set('Asia/Jakarta');

$action = $_POST['action'] ?? '';

if ($action == 'insert' || $action == 'update') {
    $id_gaji = $_POST['id_gaji'] ?? '';
    $email = $_POST['email'] ?? '';
    $periode = $_POST['periode'] ?? '';
    $total_gaji_pokok = $_POST['total_gaji_pokok'] ?? 0;
    $total_bonus = $_POST['total_bonus'] ?? 0;
    $total_kompensasi = $_POST['total_kompensasi'] ?? 0;
    $total_gaji_akhir = $_POST['total_gaji_akhir'] ?? 0;

    if ($action == 'insert') {
        $sql = "INSERT INTO gaji (id_gaji, email, periode, total_gaji_pokok, total_bonus, total_kompensasi, total_gaji_akhir)
                VALUES (?, ?, ?, ?, ?, ?, ?)";
        $stmt = mysqli_prepare($kon, $sql);
        mysqli_stmt_bind_param($stmt, "sssiddd", $id_gaji, $email, $periode, $total_gaji_pokok, $total_bonus, $total_kompensasi, $total_gaji_akhir);
    } else {
        $sql = "UPDATE gaji SET email=?, periode=?, total_gaji_pokok=?, total_bonus=?, total_kompensasi=?, total_gaji_akhir=?
                WHERE id_gaji=?";
        $stmt = mysqli_prepare($kon, $sql);
        mysqli_stmt_bind_param($stmt, "ssiddds", $email, $periode, $total_gaji_pokok, $total_bonus, $total_kompensasi, $total_gaji_akhir, $id_gaji);
    }

    if (mysqli_stmt_execute($stmt)) {
        echo json_encode(["status" => "success", "message" => "Data gaji berhasil disimpan"]);
    } else {
        echo json_encode(["status" => "error", "message" => mysqli_error($kon)]);
    }
} elseif ($action == 'delete') {
    $id_gaji = $_POST['id_gaji'] ?? '';
    $sql = "DELETE FROM gaji WHERE id_gaji = ?";
    $stmt = mysqli_prepare($kon, $sql);
    mysqli_stmt_bind_param($stmt, "s", $id_gaji);
    if (mysqli_stmt_execute($stmt)) {
        echo json_encode(["status" => "success", "message" => "Data gaji berhasil dihapus"]);
    } else {
        echo json_encode(["status" => "error", "message" => mysqli_error($kon)]);
    }
} elseif ($action == 'list') {
    $sql = "SELECT 
                gh.EMAIL AS email, 
                DATE_FORMAT(gh.TANGGAL, '%Y-%m') AS periode, 
                SUM(gh.GAJI_POKOK_HARIAN) AS total_gaji_pokok, 
                SUM(gh.BONUS_HARIAN) AS total_bonus, 
                SUM(gh.POTONGAN_TERLAMBAT) AS total_kompensasi, 
                SUM(gh.TOTAL_GAJI_HARIAN) AS total_gaji_akhir, 
                IFNULL(ANY_VALUE(k.NAMA), 'Tanpa Nama') AS nama_karyawan
            FROM gaji_harian gh
            LEFT JOIN karyawan k ON gh.EMAIL = k.EMAIL
            GROUP BY gh.EMAIL, DATE_FORMAT(gh.TANGGAL, '%Y-%m')
            ORDER BY periode DESC, gh.EMAIL ASC";
    $result = mysqli_query($kon, $sql);
    $data = [];
    if ($result) {
        while ($row = mysqli_fetch_assoc($result)) {
            $row['id_gaji'] = $row['email'] . '-' . $row['periode'];
            $row['total_gaji_pokok'] = (double)$row['total_gaji_pokok'];
            $row['total_bonus'] = (double)$row['total_bonus'];
            $row['total_kompensasi'] = (double)$row['total_kompensasi'];
            $row['total_gaji_akhir'] = (double)$row['total_gaji_akhir'];
            $data[] = $row;
        }
    }
    echo json_encode(["status" => "success", "data" => $data]);
} elseif ($action == 'barista_info') {
    $email = $_POST['email'] ?? '';
    if (empty($email)) {
        echo json_encode(["status" => "error", "message" => "Email is required"]);
        exit();
    }

    $q1 = mysqli_query($kon, "SELECT SUM(TOTAL_GAJI_HARIAN) AS total FROM gaji_harian WHERE EMAIL = '$email'");
    $r1 = mysqli_fetch_assoc($q1);
    $totalGajiHarianAllTime = (double)($r1['total'] ?? 0);

    $q2 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_pengambilan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $r2 = mysqli_fetch_assoc($q2);
    $pengambilanDisetujui = (double)($r2['total'] ?? 0);

    $q3 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $r3 = mysqli_fetch_assoc($q3);
    $penyimpananDisetujui = (double)($r3['total'] ?? 0);

    $sisaGajiHarian = $totalGajiHarianAllTime - $pengambilanDisetujui - $penyimpananDisetujui;
    
    $q4 = mysqli_query($kon, "SELECT SALDO FROM tabungan WHERE EMAIL = '$email'");
    $r4 = mysqli_fetch_assoc($q4);
    $totalTabungan = (double)($r4['SALDO'] ?? 0);

    $sisaGaji = $sisaGajiHarian + $totalTabungan;

    echo json_encode([
        "status" => "success",
        "sisa_gaji_harian" => $sisaGajiHarian,
        "total_tabungan" => $totalTabungan,
        "sisa_gaji" => $sisaGaji
    ]);
} elseif ($action == 'ambil_gaji') {
    $email = $_POST['email'] ?? '';
    $nominal = (double)($_POST['nominal'] ?? 0);
    if (empty($email)) {
        echo json_encode(["status" => "error", "message" => "Email is required"]);
        exit();
    }
    if ($nominal <= 0) {
        echo json_encode(["status" => "error", "message" => "Nominal harus lebih dari 0"]);
        exit();
    }

    $q1 = mysqli_query($kon, "SELECT SUM(TOTAL_GAJI_HARIAN) AS total FROM gaji_harian WHERE EMAIL = '$email'");
    $r1 = mysqli_fetch_assoc($q1);
    $totalGajiHarianAllTime = (double)($r1['total'] ?? 0);

    $q2 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_pengambilan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $r2 = mysqli_fetch_assoc($q2);
    $pengambilanDisetujui = (double)($r2['total'] ?? 0);

    $q3 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $r3 = mysqli_fetch_assoc($q3);
    $penyimpananDisetujui = (double)($r3['total'] ?? 0);

    $sisaGajiHarian = $totalGajiHarianAllTime - $pengambilanDisetujui - $penyimpananDisetujui;
    
    $q4 = mysqli_query($kon, "SELECT SALDO FROM tabungan WHERE EMAIL = '$email'");
    $r4 = mysqli_fetch_assoc($q4);
    $totalTabungan = (double)($r4['SALDO'] ?? 0);

    $sisaGaji = $sisaGajiHarian + $totalTabungan;

    if ($nominal > $sisaGaji) {
        echo json_encode(["status" => "error", "message" => "Nominal melebihi saldo tersedia"]);
        exit();
    }

    $sql = "INSERT INTO gaji_pengambilan (EMAIL, TANGGAL_PENGAMBILAN, NOMINAL, STATUS, created_at, updated_at) VALUES ('$email', CURDATE(), '$nominal', 'menunggu', NOW(), NOW())";
    if (mysqli_query($kon, $sql)) {
        echo json_encode(["status" => "success", "message" => "Pengajuan pengambilan berhasil terkirim"]);
    } else {
        echo json_encode(["status" => "error", "message" => mysqli_error($kon)]);
    }
} elseif ($action == 'simpan_gaji') {
    $email = $_POST['email'] ?? '';
    if (empty($email)) {
        echo json_encode(["status" => "error", "message" => "Email is required"]);
        exit();
    }

    $q1 = mysqli_query($kon, "SELECT SUM(TOTAL_GAJI_HARIAN) AS total FROM gaji_harian WHERE EMAIL = '$email'");
    $r1 = mysqli_fetch_assoc($q1);
    $totalGajiHarianAllTime = (double)($r1['total'] ?? 0);

    $q2 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_pengambilan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $r2 = mysqli_fetch_assoc($q2);
    $pengambilanDisetujui = (double)($r2['total'] ?? 0);

    $q3 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $r3 = mysqli_fetch_assoc($q3);
    $penyimpananDisetujui = (double)($r3['total'] ?? 0);

    $sisaGajiHarian = $totalGajiHarianAllTime - $pengambilanDisetujui - $penyimpananDisetujui;

    if ($sisaGajiHarian <= 0) {
        echo json_encode(["status" => "error", "message" => "Tidak ada sisa gaji yang bisa disimpan"]);
        exit();
    }

    $sql = "INSERT INTO gaji_disimpan (EMAIL, TANGGAL_PENYIMPANAN, NOMINAL, STATUS, created_at, updated_at) VALUES ('$email', CURDATE(), '$sisaGajiHarian', 'menunggu', NOW(), NOW())";
    if (mysqli_query($kon, $sql)) {
        echo json_encode(["status" => "success", "message" => "Pengajuan penyimpanan sisa gaji berhasil terkirim"]);
    } else {
        echo json_encode(["status" => "error", "message" => mysqli_error($kon)]);
    }
} elseif ($action == 'riwayat_gaji') {
    $email = $_POST['email'] ?? '';
    if (empty($email)) {
        echo json_encode(["status" => "error", "message" => "Email is required"]);
        exit();
    }

    $qP = mysqli_query($kon, "SELECT TANGGAL_PENGAMBILAN AS tanggal, NOMINAL AS nominal, STATUS AS status, CATATAN_ADMIN AS catatan, 'pengambilan' AS tipe FROM gaji_pengambilan WHERE EMAIL = '$email' ORDER BY id DESC LIMIT 15");
    $qS = mysqli_query($kon, "SELECT TANGGAL_PENYIMPANAN AS tanggal, NOMINAL AS nominal, STATUS AS status, CATATAN_ADMIN AS catatan, 'penyimpanan' AS tipe FROM gaji_disimpan WHERE EMAIL = '$email' AND NOMINAL > 0 ORDER BY id DESC LIMIT 15");
    
    $riwayat = [];
    while ($row = mysqli_fetch_assoc($qP)) {
        $row['nominal'] = (double)$row['nominal'];
        $riwayat[] = $row;
    }
    while ($row = mysqli_fetch_assoc($qS)) {
        $row['nominal'] = (double)$row['nominal'];
        $riwayat[] = $row;
    }

    usort($riwayat, function($a, $b) {
        return strcmp($b['tanggal'], $a['tanggal']);
    });

    echo json_encode(["status" => "success", "data" => $riwayat]);
} elseif ($action == 'list_pengajuan') {
    // Admin: list all pending pengambilan + penyimpanan requests
    $status_filter = $_POST['status'] ?? 'menunggu';
    
    $pengambilan = [];
    $qP = mysqli_query($kon, "SELECT gp.id, gp.EMAIL AS email, IFNULL(k.NAMA,'?') AS nama, gp.TANGGAL_PENGAMBILAN AS tanggal, gp.NOMINAL AS nominal, gp.STATUS AS status, gp.CATATAN_ADMIN AS catatan, 'pengambilan' AS tipe FROM gaji_pengambilan gp LEFT JOIN karyawan k ON gp.EMAIL = k.EMAIL WHERE gp.STATUS = '$status_filter' ORDER BY gp.id DESC");
    while ($row = mysqli_fetch_assoc($qP)) {
        $row['nominal'] = (double)$row['nominal'];
        $pengambilan[] = $row;
    }

    $penyimpanan = [];
    $qS = mysqli_query($kon, "SELECT gd.id, gd.EMAIL AS email, IFNULL(k.NAMA,'?') AS nama, gd.TANGGAL_PENYIMPANAN AS tanggal, gd.NOMINAL AS nominal, gd.STATUS AS status, gd.CATATAN_ADMIN AS catatan, 'penyimpanan' AS tipe FROM gaji_disimpan gd LEFT JOIN karyawan k ON gd.EMAIL = k.EMAIL WHERE gd.STATUS = '$status_filter' AND gd.NOMINAL > 0 ORDER BY gd.id DESC");
    while ($row = mysqli_fetch_assoc($qS)) {
        $row['nominal'] = (double)$row['nominal'];
        $penyimpanan[] = $row;
    }

    $all = array_merge($pengambilan, $penyimpanan);
    usort($all, function($a, $b) { return strcmp($b['tanggal'], $a['tanggal']); });

    echo json_encode(["status" => "success", "data" => $all]);

} elseif ($action == 'terima_pengambilan') {
    $id = (int)($_POST['id'] ?? 0);
    $catatan = $_POST['catatan'] ?? '';

    $q = mysqli_query($kon, "SELECT * FROM gaji_pengambilan WHERE id = $id AND STATUS = 'menunggu'");
    $row = mysqli_fetch_assoc($q);
    if (!$row) {
        echo json_encode(["status" => "error", "message" => "Pengajuan tidak ditemukan atau sudah diproses"]);
        exit();
    }

    $email = $row['EMAIL'];
    $nominal = (double)$row['NOMINAL'];

    // Hitung sisa gaji
    $q1 = mysqli_query($kon, "SELECT SUM(TOTAL_GAJI_HARIAN) AS total FROM gaji_harian WHERE EMAIL = '$email'");
    $totalGH = (double)(mysqli_fetch_assoc($q1)['total'] ?? 0);
    $q2 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_pengambilan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $pengDisetujui = (double)(mysqli_fetch_assoc($q2)['total'] ?? 0);
    $q3 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $penDisetujui = (double)(mysqli_fetch_assoc($q3)['total'] ?? 0);
    $sisaGajiHarian = $totalGH - $pengDisetujui - $penDisetujui;

    $q4 = mysqli_query($kon, "SELECT SALDO FROM tabungan WHERE EMAIL = '$email'");
    $r4 = mysqli_fetch_assoc($q4);
    $totalTabungan = (double)($r4['SALDO'] ?? 0);
    $sisaGaji = $sisaGajiHarian + $totalTabungan;

    if ($nominal > $sisaGaji) {
        echo json_encode(["status" => "error", "message" => "Nominal melebihi saldo tersedia (Rp " . number_format($sisaGaji, 0, ',', '.') . ")"]);
        exit();
    }

    // Setujui
    mysqli_query($kon, "UPDATE gaji_pengambilan SET STATUS = 'disetujui', CATATAN_ADMIN = '$catatan', TANGGAL_DIPROSES = NOW() WHERE id = $id");

    // Jika sisa gaji harian masih > nominal, sisanya otomatis disimpan ke tabungan
    if ($nominal <= $sisaGajiHarian) {
        $remainder = $sisaGajiHarian - $nominal;
        if ($remainder > 0) {
            mysqli_query($kon, "INSERT INTO gaji_disimpan (EMAIL, TANGGAL_PENYIMPANAN, NOMINAL, STATUS, CATATAN_ADMIN, TANGGAL_DIPROSES, created_at, updated_at) VALUES ('$email', CURDATE(), $remainder, 'disetujui', 'Sisa pengambilan otomatis disimpan', NOW(), NOW(), NOW())");
        }
    } else {
        // Tarik dari tabungan
        $excess = $nominal - $sisaGajiHarian;
        if ($excess > 0) {
            mysqli_query($kon, "INSERT INTO gaji_disimpan (EMAIL, TANGGAL_PENYIMPANAN, NOMINAL, STATUS, CATATAN_ADMIN, TANGGAL_DIPROSES, created_at, updated_at) VALUES ('$email', CURDATE(), -$excess, 'disetujui', 'Penarikan tabungan untuk pengambilan', NOW(), NOW(), NOW())");
        }
    }

    // Sync tabungan
    $qSync = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $saldo = (double)(mysqli_fetch_assoc($qSync)['total'] ?? 0);
    $exists = mysqli_query($kon, "SELECT EMAIL FROM tabungan WHERE EMAIL = '$email'");
    if (mysqli_num_rows($exists) > 0) {
        mysqli_query($kon, "UPDATE tabungan SET SALDO = $saldo, updated_at = NOW() WHERE EMAIL = '$email'");
    } else {
        mysqli_query($kon, "INSERT INTO tabungan (EMAIL, SALDO, created_at, updated_at) VALUES ('$email', $saldo, NOW(), NOW())");
    }

    echo json_encode(["status" => "success", "message" => "Pengambilan gaji berhasil disetujui"]);

} elseif ($action == 'tolak_pengambilan') {
    $id = (int)($_POST['id'] ?? 0);
    $catatan = $_POST['catatan'] ?? 'Ditolak oleh admin';
    $q = mysqli_query($kon, "SELECT id FROM gaji_pengambilan WHERE id = $id AND STATUS = 'menunggu'");
    if (!mysqli_fetch_assoc($q)) {
        echo json_encode(["status" => "error", "message" => "Pengajuan tidak ditemukan"]);
        exit();
    }
    mysqli_query($kon, "UPDATE gaji_pengambilan SET STATUS = 'ditolak', CATATAN_ADMIN = '$catatan', TANGGAL_DIPROSES = NOW() WHERE id = $id");
    echo json_encode(["status" => "success", "message" => "Pengambilan gaji ditolak"]);

} elseif ($action == 'terima_penyimpanan') {
    $id = (int)($_POST['id'] ?? 0);
    $catatan = $_POST['catatan'] ?? '';
    $q = mysqli_query($kon, "SELECT * FROM gaji_disimpan WHERE id = $id AND STATUS = 'menunggu'");
    $row = mysqli_fetch_assoc($q);
    if (!$row) {
        echo json_encode(["status" => "error", "message" => "Pengajuan tidak ditemukan"]);
        exit();
    }

    $email = $row['EMAIL'];
    $nominal = (double)$row['NOMINAL'];

    // Cek sisa gaji tersedia
    $q1 = mysqli_query($kon, "SELECT SUM(TOTAL_GAJI_HARIAN) AS total FROM gaji_harian WHERE EMAIL = '$email'");
    $totalGH = (double)(mysqli_fetch_assoc($q1)['total'] ?? 0);
    $q2 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_pengambilan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $pengDisetujui = (double)(mysqli_fetch_assoc($q2)['total'] ?? 0);
    $q3 = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui' AND id != $id");
    $penDisetujui = (double)(mysqli_fetch_assoc($q3)['total'] ?? 0);
    $available = $totalGH - $pengDisetujui - $penDisetujui;

    $nominalFinal = min($nominal, $available);
    if ($nominalFinal <= 0) {
        mysqli_query($kon, "UPDATE gaji_disimpan SET STATUS = 'ditolak', CATATAN_ADMIN = 'Sisa gaji tidak mencukupi', TANGGAL_DIPROSES = NOW() WHERE id = $id");
        echo json_encode(["status" => "error", "message" => "Penyimpanan ditolak karena sisa gaji 0"]);
        exit();
    }

    mysqli_query($kon, "UPDATE gaji_disimpan SET NOMINAL = $nominalFinal, STATUS = 'disetujui', CATATAN_ADMIN = '$catatan', TANGGAL_DIPROSES = NOW() WHERE id = $id");

    // Sync tabungan
    $qSync = mysqli_query($kon, "SELECT SUM(NOMINAL) AS total FROM gaji_disimpan WHERE EMAIL = '$email' AND STATUS = 'disetujui'");
    $saldo = (double)(mysqli_fetch_assoc($qSync)['total'] ?? 0);
    $exists = mysqli_query($kon, "SELECT EMAIL FROM tabungan WHERE EMAIL = '$email'");
    if (mysqli_num_rows($exists) > 0) {
        mysqli_query($kon, "UPDATE tabungan SET SALDO = $saldo, updated_at = NOW() WHERE EMAIL = '$email'");
    } else {
        mysqli_query($kon, "INSERT INTO tabungan (EMAIL, SALDO, created_at, updated_at) VALUES ('$email', $saldo, NOW(), NOW())");
    }

    echo json_encode(["status" => "success", "message" => "Penyimpanan gaji berhasil disetujui (Rp " . number_format($nominalFinal, 0, ',', '.') . ")"]);

} elseif ($action == 'tolak_penyimpanan') {
    $id = (int)($_POST['id'] ?? 0);
    $catatan = $_POST['catatan'] ?? 'Ditolak oleh admin';
    $q = mysqli_query($kon, "SELECT id FROM gaji_disimpan WHERE id = $id AND STATUS = 'menunggu'");
    if (!mysqli_fetch_assoc($q)) {
        echo json_encode(["status" => "error", "message" => "Pengajuan tidak ditemukan"]);
        exit();
    }
    mysqli_query($kon, "UPDATE gaji_disimpan SET STATUS = 'ditolak', CATATAN_ADMIN = '$catatan', TANGGAL_DIPROSES = NOW() WHERE id = $id");
    echo json_encode(["status" => "success", "message" => "Penyimpanan gaji ditolak"]);
}
?>