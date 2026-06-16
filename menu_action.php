<?php
include 'koneksi.php';
header("Content-Type: application/json; charset=UTF-8");

$mode = $_POST['mode'] ?? '';
$respon = array("kode" => "000");

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $sql = "SELECT * FROM menu ORDER BY ID_PRODUK ASC";
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

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $id_produk   = $_POST['id_produk'] ?? '';
    $nama_produk = $_POST['nama_produk'] ?? '';
    $kategori    = $_POST['kategori'] ?? 'Coffee';
    $harga_dasar = $_POST['harga_dasar'] ?? '0';
    $harga_jual  = $_POST['harga_jual'] ?? '0';
    $foto_base64 = $_POST['foto'] ?? ''; 

    $nama_file_foto = "default.png"; 

    // Proses konversi Base64 dari Android menjadi file gambar di server
    if (!empty($foto_base64)) {
        $nama_file_foto = "menu_" . $id_produk . "_" . time() . ".jpeg";
        $jalur_simpan = "images/" . $nama_file_foto;
        
        // Decode string base64 lalu tulis jadi file image
        file_put_contents($jalur_simpan, base64_decode($foto_base64));
    }

    if ($mode == "insert") {
        $sql = "INSERT INTO menu (ID_PRODUK, NAMA_PRODUK, KATEGORI, HARGA_DASAR, HARGA_JUAL, FOTO) 
                VALUES ('$id_produk', '$nama_produk', '$kategori', '$harga_dasar', '$harga_jual', '$nama_file_foto')";
        
        if (!mysqli_query($conn, $sql)) {
            $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
        }
    } 
    else if ($mode == "update") {
        // Jika user tidak upload foto baru saat edit, pakai foto yang lama
        if (!empty($foto_base64)) {
            $sql = "UPDATE menu 
                    SET NAMA_PRODUK='$nama_produk', KATEGORI='$kategori', HARGA_DASAR='$harga_dasar', HARGA_JUAL='$harga_jual', FOTO='$nama_file_foto' 
                    WHERE ID_PRODUK='$id_produk'";
        } else {
            $sql = "UPDATE menu 
                    SET NAMA_PRODUK='$nama_produk', KATEGORI='$kategori', HARGA_DASAR='$harga_dasar', HARGA_JUAL='$harga_jual' 
                    WHERE ID_PRODUK='$id_produk'";
        }
                
        if (!mysqli_query($conn, $sql)) {
            $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
        }
    } 
    else if ($mode == "delete") {
        $sql = "DELETE FROM menu WHERE ID_PRODUK='$id_produk'";
        if (!mysqli_query($conn, $sql)) {
            $respon = array("kode" => "111", "pesan" => mysqli_error($conn));
        }
    }

    echo json_encode($respon);
}
?>