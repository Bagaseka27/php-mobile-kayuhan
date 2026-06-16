<?php
include "koneksi.php";

$mode = $_POST['action'] ?? $_POST['mode'] ?? $_GET['mode'] ?? 'select';

if ($mode == 'select' || $mode == 'list') {
    $sql = "SELECT id_jabatan, nama_jabatan, UPAH_PER_JAM as gaji_per_jam, BONUS_PENJUALAN_PER_CUP as bonus_percup FROM jabatan ORDER BY id_jabatan ASC";
    $res = mysqli_query($kon, $sql);
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) {
        $data[] = $row;
    }
    echo json_encode(["status" => "success", "data" => $data]);
}
elseif ($mode == 'insert' || $mode == 'update') {
    $id = (int)($_POST['id_jabatan'] ?? 0);
    $nama = $_POST['nama_jabatan'] ?? '';
    $gaji = (double)($_POST['gaji_per_jam'] ?? 0);
    $bonus = (double)($_POST['bonus_percup'] ?? 0);

    $nama = mysqli_real_escape_string($kon, $nama);

    if ($mode == 'insert') {
        $sql = "INSERT INTO jabatan (NAMA_JABATAN, UPAH_PER_JAM, BONUS_PENJUALAN_PER_CUP)
                VALUES ('$nama', '$gaji', '$bonus')";
    } else {
        $sql = "UPDATE jabatan SET 
                    NAMA_JABATAN = '$nama', 
                    UPAH_PER_JAM = '$gaji', 
                    BONUS_PENJUALAN_PER_CUP = '$bonus' 
                WHERE ID_JABATAN = '$id'";
    }

    if (mysqli_query($kon, $sql)) {
        echo json_encode(["kode" => "000", "pesan" => "Berhasil simpan jabatan"]);
    } else {
        echo json_encode(["kode" => "111", "pesan" => mysqli_error($kon)]);
    }
}
elseif ($mode == 'delete') {
    $id = (int)($_POST['id_jabatan'] ?? 0);

    // Set ID_JABATAN to NULL for all employees in this position to avoid foreign key failure
    mysqli_query($kon, "UPDATE karyawan SET ID_JABATAN = NULL WHERE ID_JABATAN = '$id'");

    $sql = "DELETE FROM jabatan WHERE ID_JABATAN = '$id'";
    if (mysqli_query($kon, $sql)) {
        echo json_encode(["kode" => "000", "pesan" => "Berhasil hapus jabatan"]);
    } else {
        echo json_encode(["kode" => "111", "pesan" => mysqli_error($kon)]);
    }
}
?>