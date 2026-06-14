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
        echo json_encode(["status" => "false", "pesan" => "Koneksi database gagal"]);
        exit();
    }

    $email    = $_POST['email'];
    $password = $_POST['password'];

    // FIX: Gunakan prepared statement agar aman dari SQL Injection
    $stmt = mysqli_prepare($conn, "SELECT EMAIL, ID_JABATAN FROM karyawan WHERE EMAIL = ? AND PASSWORD = ?");
    mysqli_stmt_bind_param($stmt, "ss", $email, $password);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);

    if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);

        $role = "barista";
        if ($row['ID_JABATAN'] == 1 || strpos(strtolower($email), 'admin') !== false) {
            $role = "admin";
        }

        // FIX: Tambahkan key "status" yang dicek oleh Android
        echo json_encode([
            "status" => "true",   // <-- INI YANG HILANG, penyebab bug login
            "kode"   => "000",
            "pesan"  => "Login Berhasil",
            "email"  => $row['EMAIL'],
            "role"   => $role
        ]);
    } else {
        echo json_encode([
            "status" => "false",  // <-- konsisten
            "kode"   => "111",
            "pesan"  => "Email atau password salah!"
        ]);
    }

    mysqli_stmt_close($stmt);
    mysqli_close($conn);
}
?>