<?php
include 'koneksi.php';

try {
    if ($_SERVER['REQUEST_METHOD'] == 'POST') {
        $id_transaksi       = $_POST['id_transaksi'];
        $email              = $_POST['email'];
        $total_bayar        = $_POST['total_bayar'];
        $metode_pembayaran  = $_POST['metode_pembayaran'];
        
        // 1. Masukkan data ke tabel utama 'transaksi'
        // Tambahkan DATETIME dan STATUS yang merupakan field NOT NULL tanpa default
        $query_transaksi = "INSERT INTO transaksi (ID_TRANSAKSI, EMAIL, TOTAL_BAYAR, METODE_PEMBAYARAN, DATETIME, STATUS) 
                            VALUES ('$id_transaksi', '$email', '$total_bayar', '$metode_pembayaran', NOW(), 'SUCCESS')";
        
        if (mysqli_query($kon, $query_transaksi)) {
            // 2. Ambil data array pesanan (JSON String) yang dikirim Android
            $cart_items = json_decode($_POST['cart_items'], true);
            
            if (!empty($cart_items)) {
                foreach ($cart_items as $item) {
                    $id_produk = $item['id_produk'];
                    $jml_item  = $item['jml_item'];
                    
                    // Masukkan tiap item ke tabel 'detailtransaksi'
                    $query_detail = "INSERT INTO detailtransaksi (ID_TRANSAKSI, ID_PRODUK, JML_ITEM) 
                                     VALUES ('$id_transaksi', '$id_produk', '$jml_item')";
                    mysqli_query($kon, $query_detail);
                }
                echo json_encode(array('status' => 'success', 'message' => 'Transaksi kasir berhasil disimpan ke MySQL!'));
            } else {
                echo json_encode(array('status' => 'success', 'message' => 'Transaksi utama disimpan tanpa item detail.'));
            }
        } else {
            echo json_encode(array('status' => 'failed', 'message' => 'Gagal simpan transaksi.'));
        }
    }
} catch (Exception $e) {
    echo json_encode(array('status' => 'failed', 'message' => 'Error database: ' . $e->getMessage()));
} finally {
    mysqli_close($kon);
}
?>
##