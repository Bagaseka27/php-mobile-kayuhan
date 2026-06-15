<?php
include 'koneksi.php';
header("Content-Type: application/json; charset=UTF-8");

$mode = $_POST['mode'] ?? '';
$respon = array("kode" => "000");

// 1. READ DATA ROMBONG (GET)
if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    // Mengambil semua data dari tabel rombong dan digabungkan dengan nama_lokasi dari cabang
    $sql = "SELECT r.*, c.NAMA_LOKASI 
            FROM rombong r 
            LEFT JOIN cabang c ON r.ID_CABANG = c.ID_CABANG 
            ORDER BY r.ID_ROMBONG ASC";
            
    $result = mysqli_query($conn, $sql);
    
    // Fallback jika query JOIN bermasalah, ambil data basic dari tabel rombong saja
    if (!$result) {
        $sql = "SELECT * FROM rombong";
        $result = mysqli_query($conn, $sql);
    }

    $data = array();
    if ($result) {
        while ($row = mysqli_fetch_assoc($result)) {
            $data[] = $row;
        }
    }
    echo json_encode($data);
    exit();
}

// 2. MANIPULASI DATA ROMBONG (POST)
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $id_rombong = $_POST['id_rombong'] ?? '';
    $id_cabang  = $_POST['id_cabang'] ?? '';

    if ($mode == "insert") {
        // Memasukkan data langsung ke kolom tabel rombong (ID_ROMBONG, ID_CABANG)
        $sql = "INSERT INTO rombong (ID_ROMBONG, ID_CABANG) VALUES ('$id_rombong', '$id_cabang')";
        if (!mysqli_query($conn, $sql)) {
            $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
        }
    } 
    else if ($mode == "update") {
        // Mengubah lokasi mangkal rombong berdasarkan ID_ROMBONG-nya
        $sql = "UPDATE rombong SET ID_CABANG='$id_cabang' WHERE ID_ROMBONG='$id_rombong'";
        if (!mysqli_query($conn, $sql)) {
            $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
        }
    } 
    else if ($mode == "delete") {
        $sql = "DELETE FROM rombong WHERE ID_ROMBONG='$id_rombong'";
        if (!mysqli_query($conn, $sql)) {
            $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
        }
    }

    // Mengembalikan respon JSON murni {"kode":"000"} agar Android mendeteksi sukses
    echo json_encode($respon);
}
?>