<?php
$DB_NAME = "kayuhanmobile";
$DB_USER = "root";
$DB_PASS = "";
$DB_SERVER_LOC = "localhost";

header("Access-Control-Allow-Origin: *");
header("Content-type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] == 'POST' || $_SERVER['REQUEST_METHOD'] == 'GET') {
    $conn = mysqli_connect($DB_SERVER_LOC, $DB_USER, $DB_PASS, $DB_NAME);

    if (!$conn) {
        // FIX: Header sudah di atas, die() tidak akan merusak response
        echo json_encode(["error" => "Koneksi database gagal"]);
        exit();
    }

    $ip_laptop = "192.168.0.32";

    $sql = "SELECT id, EMAIL, TANGGAL, DATETIME_DATANG, LOKASI_DATANG, LAT_DATANG, LNG_DATANG,
            CONCAT('http://$ip_laptop/kayuhanmobile/images/', FOTO_DATANG) AS url_foto
            FROM absensi
            ORDER BY DATETIME_DATANG DESC";

    $result = mysqli_query($conn, $sql);

    $data_absensi = [];
    if ($result && mysqli_num_rows($result) > 0) {
        while ($row = mysqli_fetch_assoc($result)) {
            array_push($data_absensi, $row);
        }
    }

    echo json_encode($data_absensi);
    mysqli_close($conn);
}
?>