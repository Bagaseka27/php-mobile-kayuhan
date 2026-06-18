package com.example.kayuhan

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.kayuhan.databinding.ActivityFragmentMenuBinding
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

class FragmentMenu : Fragment() {

    private var _binding: ActivityFragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val URL_MENU = ApiConfig.MENU
    private val URL_IMAGE_FOLDER = ApiConfig.IMAGES

    private var bitmapFoto: Bitmap? = null
    private var ivPreviewTemp: ImageView? = null

    private val VOLLEY_TAG = "MenuRequestTag"

    private val galeriLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(it)
                bitmapFoto = BitmapFactory.decodeStream(inputStream)
                ivPreviewTemp?.setImageBitmap(bitmapFoto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val kameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            bitmapFoto = bundle?.get("data") as Bitmap?
            ivPreviewTemp?.setImageBitmap(bitmapFoto)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentMenuBinding.inflate(inflater, container, false)

        loadMenu()
        binding.btnTambah.setOnClickListener { showTambahDialog() }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadMenu()
    }

    override fun onPause() {
        super.onPause()
        Volley.newRequestQueue(requireContext()).cancelAll(VOLLEY_TAG)
    }

    private fun tampilkanPilihanSumberFoto() {
        val opsi = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Ambil Gambar Menggunakan:")
            .setItems(opsi) { _, posisi ->
                when (posisi) {
                    0 -> {
                        val intentKamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        kameraLauncher.launch(intentKamera)
                    }
                    1 -> galeriLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun loadMenu() {
        if (_binding == null) return

        val childCount = binding.tableMenu.childCount
        if (childCount > 1) {
            binding.tableMenu.removeViews(1, childCount - 1)
        }

        val request = StringRequest(
            Request.Method.GET, URL_MENU,
            { response ->
                // PERBAIKAN: Dibungkus dengan IF agar aman dari error merah & anti-lag pas pindah menu
                if (_binding != null && isAdded) {
                    try {
                        val jsonArray = JSONArray(response)

                        for (x in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(x)

                            val id        = jsonObject.getString("ID_PRODUK")
                            val nama      = jsonObject.getString("NAMA_PRODUK")
                            val kategori  = jsonObject.getString("KATEGORI")
                            val hargaDasar = jsonObject.getInt("HARGA_DASAR")
                            val hargaJual  = jsonObject.getInt("HARGA_JUAL")
                            val namaFotoFile = jsonObject.optString("FOTO", "default.png")

                            val row = TableRow(requireContext()).apply {
                                setPadding(0, 4, 0, 4)
                                gravity = android.view.Gravity.CENTER_VERTICAL
                            }

                            val ivFotoMenu = ImageView(requireContext()).apply {
                                layoutParams = TableRow.LayoutParams(60.dp, 60.dp).apply {
                                    setMargins(8, 6, 8, 6)
                                }
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }

                            Picasso.get()
                                .load(URL_IMAGE_FOLDER + namaFotoFile)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_report_image)
                                .into(ivFotoMenu)

                            row.addView(ivFotoMenu)
                            row.addView(buatTextView(id, 60.dp))
                            row.addView(buatTextView(nama, 120.dp))
                            row.addView(buatTextView(kategori, 100.dp))
                            row.addView(buatTextView(hargaJual.toString(), 90.dp))

                            val layoutAksi = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity     = android.view.Gravity.CENTER
                                layoutParams = TableRow.LayoutParams(160.dp, TableRow.LayoutParams.WRAP_CONTENT)
                            }

                            val btnEdit = Button(requireContext()).apply {
                                text      = "Edit"
                                textSize  = 12f
                                setTextColor(Color.WHITE)
                                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC107"))
                                minWidth     = 0
                                minimumWidth = 0
                                setPadding(16, 0, 16, 0)
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80)
                            }

                            val btnHapus = Button(requireContext()).apply {
                                text      = "Hapus"
                                textSize  = 12f
                                setTextColor(Color.WHITE)
                                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935"))
                                minWidth     = 0
                                minimumWidth = 0
                                setPadding(16, 0, 16, 0)
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80).apply { marginStart = 8 }
                            }

                            layoutAksi.addView(btnEdit)
                            layoutAksi.addView(btnHapus)
                            row.addView(layoutAksi)
                            binding.tableMenu.addView(row)

                            btnEdit.setOnClickListener {
                                showEditDialog(id, nama, kategori, hargaDasar, hargaJual, namaFotoFile)
                            }

                            btnHapus.setOnClickListener {
                                val ctx = requireContext()
                                AlertDialog.Builder(ctx)
                                    .setTitle("Konfirmasi")
                                    .setMessage("Apakah yakin hapus menu ini?")
                                    .setPositiveButton("Ya") { _, _ ->
                                        eksekusiQueryMenu("delete", id, "", "", "0", "0", "")
                                    }
                                    .setNegativeButton("Tidak", null)
                                    .show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Format data tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            {
                if (isAdded) Toast.makeText(context, "Gagal memuat data dari Laragon", Toast.LENGTH_SHORT).show()
            }
        ).apply {
            tag = VOLLEY_TAG
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun buatTextView(isi: String, lebar: Int): TextView {
        return TextView(requireContext()).apply {
            text = isi
            setPadding(8, 12, 8, 12)
            layoutParams = TableRow.LayoutParams(lebar, TableRow.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap?): String {
        if (bitmap == null) return ""
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun eksekusiQueryMenu(
        modeAction: String, id: String, nama: String, kategori: String, hd: String, hj: String, fotoBase64: String
    ) {
        val request = object : StringRequest(
            Method.POST, URL_MENU,
            { response ->
                if (_binding != null && isAdded) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (jsonObject.getString("kode") == "000") {
                            Toast.makeText(context, "Operasi $modeAction Sukses!", Toast.LENGTH_SHORT).show()
                            loadMenu()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Response tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { if (isAdded) Toast.makeText(context, "Terjadi kesalahan jaringan", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "mode"        to modeAction,
                    "id_produk"   to id,
                    "nama_produk" to nama,
                    "kategori"    to kategori,
                    "harga_dasar" to hd,
                    "harga_jual"  to hj,
                    "foto"        to fotoBase64
                )
            }
        }
        request.tag = VOLLEY_TAG
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    private fun setupSpinner(spinner: Spinner, selected: String = "Coffee") {
        val kategoriList = listOf("Coffee", "Non-Coffee")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kategoriList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val idx = kategoriList.indexOf(selected)
        if (idx >= 0) spinner.setSelection(idx)
    }

    private fun showTambahDialog() {
        bitmapFoto = null
        val view         = layoutInflater.inflate(R.layout.dialog_menu, null)
        val etId         = view.findViewById<EditText>(R.id.etIdMenu)
        val etNama       = view.findViewById<EditText>(R.id.etNamaMenu)
        val spinner      = view.findViewById<Spinner>(R.id.spKategori)
        val etHargaDasar = view.findViewById<EditText>(R.id.etHargaDasar)
        val etHargaJual  = view.findViewById<EditText>(R.id.etHargaJual)

        ivPreviewTemp    = view.findViewById<ImageView>(R.id.ivPreviewMenu)
        val btnPilihFoto = view.findViewById<Button>(R.id.btnPilihFoto)

        setupSpinner(spinner)
        btnPilihFoto.setOnClickListener { tampilkanPilihanSumberFoto() }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Menu")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val id       = etId.text.toString().trim()
                val nama     = etNama.text.toString().trim()
                val kategori = spinner.selectedItem.toString()
                val hd       = etHargaDasar.text.toString().trim()
                val hj       = etHargaJual.text.toString().trim()

                if (id.isNotEmpty() && nama.isNotEmpty() && hd.isNotEmpty() && hj.isNotEmpty()) {
                    val strFoto = bitmapToBase64(bitmapFoto)
                    eksekusiQueryMenu("insert", id, nama, kategori, hd, hj, strFoto)
                } else {
                    Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditDialog(
        id: String, nama: String, kategori: String, hargaDasar: Int, hargaJual: Int, fotoLama: String
    ) {
        bitmapFoto = null
        val view         = layoutInflater.inflate(R.layout.dialog_menu, null)
        val etId         = view.findViewById<EditText>(R.id.etIdMenu)
        val etNama       = view.findViewById<EditText>(R.id.etNamaMenu)
        val spinner      = view.findViewById<Spinner>(R.id.spKategori)
        val etHargaDasar = view.findViewById<EditText>(R.id.etHargaDasar)
        val etHargaJual  = view.findViewById<EditText>(R.id.etHargaJual)

        ivPreviewTemp    = view.findViewById<ImageView>(R.id.ivPreviewMenu)
        val btnPilihFoto = view.findViewById<Button>(R.id.btnPilihFoto)

        etId.setText(id)
        etId.isEnabled = false
        etNama.setText(nama)
        setupSpinner(spinner, kategori)
        etHargaDasar.setText(hargaDasar.toString())
        etHargaJual.setText(hargaJual.toString())

        Picasso.get()
            .load(URL_IMAGE_FOLDER + fotoLama)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivPreviewTemp!!)

        btnPilihFoto.setOnClickListener { tampilkanPilihanSumberFoto() }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Menu")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val newNama     = etNama.text.toString().trim()
                val newKategori = spinner.selectedItem.toString()
                val newHd       = etHargaDasar.text.toString().trim()
                val newHj       = etHargaJual.text.toString().trim()

                if (newNama.isNotEmpty() && newHd.isNotEmpty() && newHj.isNotEmpty()) {
                    val strFoto = bitmapToBase64(bitmapFoto)
                    eksekusiQueryMenu("update", id, newNama, newKategori, newHd, newHj, strFoto)
                } else {
                    Toast.makeText(context, "Field tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        Volley.newRequestQueue(requireContext()).cancelAll(VOLLEY_TAG)
        super.onDestroyView()
        _binding = null
    }
} 