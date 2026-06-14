<?php
$DB_NAME = "kayuhanmobile";
$DB_USER = "root";
$DB_PASS = "";
$DB_SERVER_LOC = "localhost";

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $conn = mysqli_connect($DB_SERVER_LOC, $DB_USER, $DB_PASS, $DB_NAME);

    if (!$conn) {
        echo json_encode(["kode" => "111", "pesan" => "Koneksi database gagal"]);
        exit();
    }

    $email          = $_POST['email'];
    $tanggal        = date('Y-m-d');
    $datetime_datang = date('Y-m-d H:i:s');
    $imstr          = $_POST['foto_datang'];
    $filename       = $_POST['file'];
    $lokasi_datang  = $_POST['lokasi_datang'];
    $lat_datang     = $_POST['lat_datang'];
    $lng_datang     = $_POST['lng_datang'];

    $path = "images/";

    if (file_put_contents($path . $filename, base64_decode($imstr))) {
        // FIX: Gunakan prepared statement
        $stmt = mysqli_prepare($conn,
            "INSERT INTO absensi (EMAIL, TANGGAL, DATETIME_DATANG, FOTO_DATANG, LOKASI_DATANG, LAT_DATANG, LNG_DATANG)
             VALUES (?, ?, ?, ?, ?, ?, ?)"
        );
        mysqli_stmt_bind_param($stmt, "sssssss",
            $email, $tanggal, $datetime_datang, $filename, $lokasi_datang, $lat_datang, $lng_datang
        );

        if (mysqli_stmt_execute($stmt)) {
            echo json_encode(["kode" => "000", "pesan" => "Presensi Berhasil Terkirim"]);
        } else {
            echo json_encode(["kode" => "111", "pesan" => "Gagal menyimpan data ke database"]);
        }

        mysqli_stmt_close($stmt);
    } else {
        echo json_encode(["kode" => "111", "pesan" => "Gagal mengupload file gambar ke folder PC"]);
    }

    mysqli_close($conn);
}
?>