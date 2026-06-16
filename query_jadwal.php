<?php
$DB_NAME = "kayuhan";
$DB_USER = "root";
$DB_PASS = "";
$DB_SERVER_LOC = "localhost";

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] == 'POST' || $_SERVER['REQUEST_METHOD'] == 'GET') {
    $conn = mysqli_connect($DB_SERVER_LOC, $DB_USER, $DB_PASS, $DB_NAME);

    if (!$conn) {
        echo json_encode(["kode" => "111", "pesan" => "Koneksi gagal"]);
        exit();
    }

    $mode = isset($_POST['mode']) ? $_POST['mode'] : (isset($_GET['mode']) ? $_GET['mode'] : 'select');
    $respon = ["kode" => "000", "pesan" => "Sukses"];

    switch ($mode) {
        case "select":
            // FIX: JOIN ke tabel karyawan dan cabang agar nama tampil di Android
            $sql = "SELECT j.ID_JADWAL as id_jadwal, j.EMAIL, k.nama as nama_karyawan,
                           j.TANGGAL, j.JAM_MULAI, j.JAM_SELESAI,
                           j.ID_CABANG, c.NAMA_LOKASI
                    FROM jadwal j
                    LEFT JOIN karyawan k ON j.EMAIL = k.EMAIL
                    LEFT JOIN cabang c ON j.ID_CABANG = c.ID_CABANG
                    ORDER BY j.ID_JADWAL DESC";
            $result = mysqli_query($conn, $sql);
            $data = [];
            if ($result) {
                while ($row = mysqli_fetch_assoc($result)) {
                    array_push($data, $row);
                }
            }
            echo json_encode($data);
            break;

        case "insert":
            // FIX: Terima id_cabang sesuai yang dikirim Android
            $email      = $_POST['email'];
            $id_cabang  = $_POST['id_cabang'];
            $tanggal    = $_POST['tanggal'];
            $jam_mulai  = $_POST['jam_mulai'];
            $jam_selesai = $_POST['jam_selesai'];

            // Generate unique ID_JADWAL like JDYYYYMMDDxxxxx
            $tanggal_clean = str_replace('-', '', $tanggal);
            $random_suffix = substr(md5(uniqid(rand(), true)), 0, 5);
            $id_jadwal = "JD" . $tanggal_clean . $random_suffix;

            $stmt = mysqli_prepare($conn,
                "INSERT INTO jadwal (ID_JADWAL, EMAIL, ID_CABANG, TANGGAL, JAM_MULAI, JAM_SELESAI)
                 VALUES (?, ?, ?, ?, ?, ?)"
            );
            mysqli_stmt_bind_param($stmt, "ssssss", $id_jadwal, $email, $id_cabang, $tanggal, $jam_mulai, $jam_selesai);

            if (mysqli_stmt_execute($stmt)) {
                echo json_encode($respon);
            } else {
                echo json_encode(["kode" => "111", "pesan" => mysqli_error($conn)]);
            }
            mysqli_stmt_close($stmt);
            break;

        case "update":
            $id_jadwal   = $_POST['id_jadwal'];
            $email       = $_POST['email'];
            $id_cabang   = $_POST['id_cabang'];
            $tanggal     = $_POST['tanggal'];
            $jam_mulai   = $_POST['jam_mulai'];
            $jam_selesai = $_POST['jam_selesai'];

            $stmt = mysqli_prepare($conn,
                "UPDATE jadwal SET EMAIL=?, ID_CABANG=?, TANGGAL=?, JAM_MULAI=?, JAM_SELESAI=? WHERE ID_JADWAL=?"
            );
            mysqli_stmt_bind_param($stmt, "ssssss", $email, $id_cabang, $tanggal, $jam_mulai, $jam_selesai, $id_jadwal);

            if (mysqli_stmt_execute($stmt)) {
                echo json_encode($respon);
            } else {
                echo json_encode(["kode" => "111", "pesan" => mysqli_error($conn)]);
            }
            mysqli_stmt_close($stmt);
            break;

        case "delete":
            $id_jadwal = $_POST['id_jadwal'];

            $stmt = mysqli_prepare($conn, "DELETE FROM jadwal WHERE ID_JADWAL=?");
            mysqli_stmt_bind_param($stmt, "s", $id_jadwal);

            if (mysqli_stmt_execute($stmt)) {
                echo json_encode($respon);
            } else {
                echo json_encode(["kode" => "111", "pesan" => mysqli_error($conn)]);
            }
            mysqli_stmt_close($stmt);
            break;

        default:
            echo json_encode(["kode" => "111", "pesan" => "Mode tidak dikenal"]);
            break;
    }

    mysqli_close($conn);
}
?>