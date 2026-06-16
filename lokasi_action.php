<?php
include 'koneksi.php';
header("Content-Type: application/json; charset=UTF-8");

$mode = $_POST['mode'] ?? '';
$respon = array("kode" => "000");

// 1. AMBIL DATA (GET)
if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    // Memakai SELECT * agar aman dari error kolom saat menampilkan data
    $sql = "SELECT * FROM cabang";
    $result = mysqli_query($conn, $sql);

    $data = array();
    if ($result) {
        while ($row = mysqli_fetch_assoc($result)) {
            $data[] = $row;
        }
    }
    echo json_encode($data);
    exit();
}

// 2. MANIPULASI DATA / SIMPAN (POST)
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $id_cabang   = $_POST['id_cabang'] ?? '';
    $nama_cabang = $_POST['nama_cabang'] ?? ''; // Menerima kiriman data dari Android
    $id_rombong  = $_POST['id_rombong'] ?? '';

    if ($mode == "insert") {
        if (empty($id_rombong)) {
            // FIX SAKTI: Menyebutkan nama kolom database asli kamu secara spesifik (ID_CABANG, NAMA_LOKASI)
            $sql = "INSERT INTO cabang (ID_CABANG, NAMA_LOKASI) VALUES ('$id_cabang', '$nama_cabang')";
            if (!mysqli_query($conn, $sql)) {
                $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
            }
        } else {
            $sql = "INSERT INTO rombong (ID_ROMBONG, ID_CABANG) VALUES ('$id_rombong', '$id_cabang')";
            if (!mysqli_query($conn, $sql)) {
                $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
            }
        }
    } 
    else if ($mode == "update") {
        if (empty($id_rombong)) {
            // FIX SAKTI: Update langsung ke kolom NAMA_LOKASI
            $sql = "UPDATE cabang SET NAMA_LOKASI='$nama_cabang' WHERE ID_CABANG='$id_cabang'";
            if (!mysqli_query($conn, $sql)) {
                $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
            }
        } else {
            $sql = "UPDATE rombong SET ID_ROMBONG='$id_rombong' WHERE ID_CABANG='$id_cabang'";
            if (!mysqli_query($conn, $sql)) {
                $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
            }
        }
    } 
    else if ($mode == "delete") {
        if (empty($id_rombong)) {
            $sql = "DELETE FROM cabang WHERE ID_CABANG='$id_cabang'";
            mysqli_query($conn, $sql);
        } else {
            $sql = "DELETE FROM rombong WHERE ID_ROMBONG='$id_rombong'";
            mysqli_query($conn, $sql);
        }
    }

    // Mengembalikan respon JSON murni yang valid {"kode":"000"} untuk Android
    echo json_encode($respon);
}
?>