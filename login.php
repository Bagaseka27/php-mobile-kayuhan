<?php
$DB_NAME = "kayuhan";
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

    // Fetch the email, password hash, and id_jabatan by email
    $stmt = mysqli_prepare($conn, "SELECT EMAIL, PASSWORD, ID_JABATAN FROM karyawan WHERE EMAIL = ?");
    mysqli_stmt_bind_param($stmt, "s", $email);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);

    if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $hashed_password = $row['PASSWORD'];

        // Verify password using bcrypt (password_verify) or direct comparison fallback
        $login_ok = false;
        if (!empty($hashed_password) && password_verify($password, $hashed_password)) {
            $login_ok = true;
        } elseif ($password === $hashed_password) {
            $login_ok = true;
        }

        if ($login_ok) {
            $role = "barista";
            if ($row['ID_JABATAN'] == 1 || strpos(strtolower($email), 'admin') !== false) {
                $role = "admin";
            }

            echo json_encode([
                "status" => "true",
                "kode"   => "000",
                "pesan"  => "Login Berhasil",
                "email"  => $row['EMAIL'],
                "role"   => $role
            ]);
        } else {
            echo json_encode([
                "status" => "false",
                "kode"   => "111",
                "pesan"  => "Email atau password salah!"
            ]);
        }
    } else {
        echo json_encode([
            "status" => "false",
            "kode"   => "111",
            "pesan"  => "Email atau password salah!"
        ]);
    }

    mysqli_stmt_close($stmt);
    mysqli_close($conn);
}
?>