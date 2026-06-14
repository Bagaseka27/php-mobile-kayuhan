<?php
include 'koneksi.php';

$kategori = isset($_GET['kategori']) ? $_GET['kategori'] : '';

// Jika kategori dipilih selain 'All Menu'
if ($kategori != '' && $kategori != 'All') {
    $query = "SELECT * FROM menu WHERE KATEGORI LIKE '%$kategori%'";
} else {
    $query = "SELECT * FROM menu";
}

$result = mysqli_query($kon, $query);
$response = array();

while ($row = mysqli_fetch_assoc($result)) {
    $response[] = array(
        'id_produk'    => $row['ID_PRODUK'],
        'nama_produk'  => $row['NAMA_PRODUK'],
        'kategori'     => $row['KATEGORI'],
        'harga_jual'   => (int)$row['HARGA_JUAL'],
        'foto'         => $row['FOTO']
    );
}

echo json_encode($response);
mysqli_close($kon);
?>