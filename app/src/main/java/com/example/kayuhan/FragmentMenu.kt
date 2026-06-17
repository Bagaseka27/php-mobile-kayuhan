package com.example.kayuhan

import android.app.AlertDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.kayuhan.databinding.ActivityFragmentMenuBinding

val Int.dp: Int get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

class FragmentMenu : Fragment() {

    private var _binding: ActivityFragmentMenuBinding? = null
    private val binding get() = _binding!!
    private val apiUrl = ApiConfig.MENU

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentMenuBinding.inflate(inflater, container, false)
        
        val isBarista = activity is DashboardBaristaActivity
        if (isBarista) {
            binding.btnTambah.visibility = View.GONE
            binding.tvTitleMenu.text = "Daftar Menu"
        }
        
        loadMenu()
        binding.btnTambah.setOnClickListener { showTambahDialog() }
        return binding.root
    }

    private fun loadMenu() {
        val isBarista = activity is DashboardBaristaActivity
        postKeServer(requireContext(), apiUrl, mapOf()) { response ->
            try {
                // Remove existing rows except header
                val childCount = binding.tableMenu.childCount
                if (childCount > 1) {
                    binding.tableMenu.removeViews(1, childCount - 1)
                }
                
                if (isBarista) {
                    binding.tvHeaderAksi.visibility = View.GONE
                } else {
                    binding.tvHeaderAksi.visibility = View.VISIBLE
                }

                val data = org.json.JSONArray(response)
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.getString("ID_PRODUK")
                    val nama = obj.getString("NAMA_PRODUK")
                    val kategori = obj.getString("KATEGORI")
                    val hargaDasar = obj.getInt("HARGA_DASAR")
                    val hargaJual = obj.getInt("HARGA_JUAL")

                    val row = TableRow(requireContext()).apply {
                        setPadding(0, 4, 0, 4)
                    }

                    fun buatTextView(isi: String, lebar: Int): TextView {
                        return TextView(requireContext()).apply {
                            text = isi
                            setPadding(8, 12, 8, 12)
                            layoutParams = TableRow.LayoutParams(lebar, TableRow.LayoutParams.WRAP_CONTENT)
                        }
                    }

                    row.addView(buatTextView(id, 55.dp))
                    row.addView(buatTextView(nama, 100.dp))
                    row.addView(buatTextView(kategori, 100.dp))
                    row.addView(buatTextView(hargaJual.toString(), 80.dp))

                    if (!isBarista) {
                        val layoutAksi = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER
                            layoutParams = TableRow.LayoutParams(160.dp, TableRow.LayoutParams.WRAP_CONTENT)
                        }

                        val btnEdit = Button(requireContext()).apply {
                            text = "Edit"
                            textSize = 12f
                            setTextColor(Color.WHITE)
                            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC107"))
                            minWidth = 0
                            minimumWidth = 0
                            setPadding(16, 0, 16, 0)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80)
                        }

                        val btnHapus = Button(requireContext()).apply {
                            text = "Hapus"
                            textSize = 12f
                            setTextColor(Color.WHITE)
                            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935"))
                            minWidth = 0
                            minimumWidth = 0
                            setPadding(16, 0, 16, 0)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80).apply { marginStart = 8 }
                        }

                        layoutAksi.addView(btnEdit)
                        layoutAksi.addView(btnHapus)
                        row.addView(layoutAksi)

                        btnEdit.setOnClickListener { showEditDialog(id, nama, kategori, hargaDasar, hargaJual) }
                        btnHapus.setOnClickListener {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Konfirmasi")
                                .setMessage("Apakah yakin hapus menu ini?")
                                .setPositiveButton("Ya") { _, _ ->
                                    val params = mapOf("mode" to "delete", "id_produk" to id)
                                    postKeServer(requireContext(), apiUrl, params) { loadMenu() }
                                }
                                .setNegativeButton("Tidak", null)
                                .show()
                        }
                    }

                    binding.tableMenu.addView(row)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showTambahDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_menu, null)
        val etId = view.findViewById<EditText>(R.id.etIdMenu)
        val etNama = view.findViewById<EditText>(R.id.etNamaMenu)
        val spinner = view.findViewById<Spinner>(R.id.spKategori)
        val etHargaDasar = view.findViewById<EditText>(R.id.etHargaDasar)
        val etHargaJual = view.findViewById<EditText>(R.id.etHargaJual)

        setupSpinner(spinner)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Menu")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val params = mapOf(
                    "mode" to "insert",
                    "id_produk" to etId.text.toString(),
                    "nama_produk" to etNama.text.toString(),
                    "kategori" to spinner.selectedItem.toString(),
                    "harga_dasar" to etHargaDasar.text.toString(),
                    "harga_jual" to etHargaJual.text.toString()
                )
                postKeServer(requireContext(), apiUrl, params) { loadMenu() }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSpinner(spinner: Spinner, selection: String? = null) {
        val categories = arrayOf("Coffee", "Non-Coffee", "Food", "Snack")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        selection?.let {
            val pos = categories.indexOf(it)
            if (pos != -1) spinner.setSelection(pos)
        }
    }

    private fun showEditDialog(id: String, nama: String, kategori: String, hargaDasar: Int, hargaJual: Int) {
        val view = layoutInflater.inflate(R.layout.dialog_menu, null)
        val etId = view.findViewById<EditText>(R.id.etIdMenu)
        val etNama = view.findViewById<EditText>(R.id.etNamaMenu)
        val spinner = view.findViewById<Spinner>(R.id.spKategori)
        val etHargaDasar = view.findViewById<EditText>(R.id.etHargaDasar)
        val etHargaJual = view.findViewById<EditText>(R.id.etHargaJual)

        etId.setText(id); etId.isEnabled = false
        etNama.setText(nama)
        setupSpinner(spinner, kategori)
        etHargaDasar.setText(hargaDasar.toString())
        etHargaJual.setText(hargaJual.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Menu")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val params = mapOf(
                    "mode" to "update",
                    "id_produk" to id,
                    "nama_produk" to etNama.text.toString(),
                    "kategori" to spinner.selectedItem.toString(),
                    "harga_dasar" to etHargaDasar.text.toString(),
                    "harga_jual" to etHargaJual.text.toString()
                )
                postKeServer(requireContext(), apiUrl, params) { loadMenu() }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}