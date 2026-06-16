<?php
$host = "localhost";
$user = "root";
$pass = ""; // Kosongkan jika pakai default Laragon
$db   = "kayuhanmobile";

$kon = mysqli_connect($host, $user, $pass, $db);

if (!$kon) {
    die("Koneksi database gagal: " . mysqli_connect_error());
}
?>
##