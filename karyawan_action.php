<?php
include "koneksi.php";

$mode = $_POST['action'] ?? $_POST['mode'] ?? $_GET['mode'] ?? 'select';

if ($mode == 'select' || $mode == 'list') {
    $sql = "SELECT 
                k.EMAIL AS email, 
                k.NAMA AS nama, 
                k.NO_HP AS no_hp, 
                k.ROLE AS posisi, 
                k.ID_ROMBONG AS id_rombong, 
                k.ID_JABATAN AS id_jabatan, 
                k.ID_CABANG AS id_cabang, 
                c.NAMA_LOKASI AS nama_lokasi 
            FROM karyawan k
            LEFT JOIN rombong r ON k.ID_ROMBONG = r.ID_ROMBONG
            LEFT JOIN cabang c ON r.ID_CABANG = c.ID_CABANG";
    $res = mysqli_query($kon, $sql);
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) {
        $data[] = $row;
    }
    echo json_encode(["status" => "success", "data" => $data]);
}
elseif ($mode == 'list_full') {
    $sql = "SELECT k.email, k.nama, j.UPAH_PER_JAM AS gaji_per_jam, j.BONUS_PENJUALAN_PER_CUP AS bonus_percup 
            FROM karyawan k 
            LEFT JOIN jabatan j ON k.ID_JABATAN = j.ID_JABATAN";
    $res = mysqli_query($kon, $sql);
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) {
        $data[] = $row;
    }
    echo json_encode(["status" => "success", "data" => $data]);
}
elseif ($mode == 'insert' || $mode == 'update') {
    $email = $_POST['email'] ?? '';
    $nama = $_POST['nama'] ?? '';
    $no_hp = $_POST['no_hp'] ?? '';
    $posisi = $_POST['posisi'] ?? '';
    $id_jabatan = !empty($_POST['id_jabatan']) ? (int)$_POST['id_jabatan'] : 'NULL';
    $id_rombong = !empty($_POST['id_rombong']) ? "'" . mysqli_real_escape_string($kon, $_POST['id_rombong']) . "'" : 'NULL';
    $id_cabang = !empty($_POST['id_cabang']) ? "'" . mysqli_real_escape_string($kon, $_POST['id_cabang']) . "'" : 'NULL';

    $email = mysqli_real_escape_string($kon, $email);
    $nama = mysqli_real_escape_string($kon, $nama);
    $no_hp = mysqli_real_escape_string($kon, $no_hp);
    $posisi = mysqli_real_escape_string($kon, $posisi);

    if ($mode == 'insert') {
        $sql = "INSERT INTO karyawan (EMAIL, NAMA, NO_HP, ROLE, ID_JABATAN, ID_ROMBONG, ID_CABANG)
                VALUES ('$email', '$nama', '$no_hp', '$posisi', $id_jabatan, $id_rombong, $id_cabang)";
    } else {
        $sql = "UPDATE karyawan SET 
                    NAMA = '$nama', 
                    NO_HP = '$no_hp', 
                    ROLE = '$posisi', 
                    ID_JABATAN = $id_jabatan, 
                    ID_ROMBONG = $id_rombong, 
                    ID_CABANG = $id_cabang 
                WHERE EMAIL = '$email'";
    }

    if (mysqli_query($kon, $sql)) {
        echo json_encode(["kode" => "000", "pesan" => "Berhasil simpan karyawan"]);
    } else {
        echo json_encode(["kode" => "111", "pesan" => mysqli_error($kon)]);
    }
}
elseif ($mode == 'delete') {
    $email = $_POST['email'] ?? '';
    $email = mysqli_real_escape_string($kon, $email);

    // Delete related records in dependent tables first to avoid foreign key violations
    mysqli_query($kon, "DELETE FROM absensi WHERE EMAIL = '$email'");
    mysqli_query($kon, "DELETE FROM gaji_disimpan WHERE EMAIL = '$email'");
    mysqli_query($kon, "DELETE FROM gaji_harian WHERE EMAIL = '$email'");
    mysqli_query($kon, "DELETE FROM gaji_pengambilan WHERE EMAIL = '$email'");
    mysqli_query($kon, "DELETE FROM jadwal WHERE EMAIL = '$email'");
    mysqli_query($kon, "DELETE FROM tabungan WHERE EMAIL = '$email'");
    mysqli_query($kon, "DELETE FROM detailtransaksi WHERE ID_TRANSAKSI IN (SELECT ID_TRANSAKSI FROM transaksi WHERE EMAIL = '$email')");
    mysqli_query($kon, "DELETE FROM transaksi WHERE EMAIL = '$email'");

    $sql = "DELETE FROM karyawan WHERE EMAIL = '$email'";
    if (mysqli_query($kon, $sql)) {
        echo json_encode(["kode" => "000", "pesan" => "Berhasil hapus karyawan"]);
    } else {
        echo json_encode(["kode" => "111", "pesan" => mysqli_error($kon)]);
    }
}
?>