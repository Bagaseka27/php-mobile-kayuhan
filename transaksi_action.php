<?php
include 'koneksi.php';
header("Content-Type: application/json; charset=UTF-8");

$dari = $_POST['dari'] ?? '';
$sampai = $_POST['sampai'] ?? '';

$sql = "SELECT 
            t.ID_TRANSAKSI, 
            t.DATETIME AS WAKTU_TRANSAKSI, 
            t.TOTAL_BAYAR AS TOTAL_HARGA, 
            t.METODE_PEMBAYARAN AS METODE_BAYAR, 
            IFNULL(SUM(d.JML_ITEM), 0) AS JUMLAH_ITEM 
        FROM transaksi t 
        LEFT JOIN detailtransaksi d ON t.ID_TRANSAKSI = d.ID_TRANSAKSI";

$role = $_POST['role'] ?? '';
$email = $_POST['email'] ?? '';

$whereClauses = [];
if (strtolower($role) === 'barista' && !empty($email)) {
    $whereClauses[] = "t.EMAIL = '" . mysqli_real_escape_string($kon, $email) . "'";
}
if (!empty($dari) && !empty($sampai)) {
    $whereClauses[] = "t.DATETIME BETWEEN '" . mysqli_real_escape_string($kon, $dari) . "' AND '" . mysqli_real_escape_string($kon, $sampai) . "'";
}

if (!empty($whereClauses)) {
    $sql .= " WHERE " . implode(" AND ", $whereClauses);
}

$sql .= " GROUP BY t.ID_TRANSAKSI, t.DATETIME, t.TOTAL_BAYAR, t.METODE_PEMBAYARAN";
$sql .= " ORDER BY t.DATETIME DESC";

$result = mysqli_query($kon, $sql);
$data = array();

if ($result) {
    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>