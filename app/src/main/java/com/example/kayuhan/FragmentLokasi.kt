package com.example.kayuhan

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.kayuhan.postKeServer
import org.json.JSONArray
import org.json.JSONObject

class FragmentLokasi : Fragment() {
    private val apiUrl = "http://192.168.0.109/php-mobile-kayuhan/lokasi_action.php"
    private val apiRombongUrl = "http://192.168.0.109/php-mobile-kayuhan/rombong_action.php"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_fragment_lokasi, container, false)

        val btnTambahCabang = view.findViewById<Button>(R.id.btnTambahCabang)
        val btnTambahRombong = view.findViewById<Button>(R.id.btnTambahRombong)

        btnTambahCabang.setOnClickListener { showDialogCabang(null, null) }
        btnTambahRombong.setOnClickListener { showDialogRombong(null, null) }

        loadData(view)
        return view
    }

    private fun loadData(view: View) {
        val tableCabang = view.findViewById<TableLayout>(R.id.tableCabang)
        val tableRombong = view.findViewById<TableLayout>(R.id.tableRombong)

        // Clear tables except headers (assuming first row is header if defined in XML)
        // If whole TableLayout is dynamic, removeAllViews is fine
        tableCabang.removeAllViews()
        tableRombong.removeAllViews()

        // Load Cabang
        postKeServer(requireContext(), apiUrl, mapOf()) { response ->
            try {
                val data = JSONArray(response)
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.getString("ID_CABANG")
                    val nama = obj.getString("NAMA_LOKASI")

                    val row = TableRow(context).apply {
                        setPadding(8, 12, 8, 12)
                        setBackgroundColor(Color.WHITE)
                    }
                    row.addView(TextView(context).apply { text = id; layoutParams = TableRow.LayoutParams(0, -2, 1f) })
                    row.addView(TextView(context).apply { text = nama; layoutParams = TableRow.LayoutParams(0, -2, 2f) })

                    val actionLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        layoutParams = TableRow.LayoutParams(0, -2, 1f)
                    }

                    val btnEdit = ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_menu_edit)
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { showDialogCabang(id, nama) }
                    }
                    val btnDelete = ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_menu_delete)
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { deleteCabang(id) }
                    }

                    actionLayout.addView(btnEdit)
                    actionLayout.addView(btnDelete)
                    row.addView(actionLayout)
                    tableCabang.addView(row)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Load Rombong
        postKeServer(requireContext(), apiRombongUrl, mapOf("action" to "list")) { response ->
            try {
                val json = JSONObject(response)
                val data = json.getJSONArray("data")
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val idRombong = obj.getString("id_rombong")
                    val namaLokasi = obj.optString("nama_lokasi", "-")
                    val idCabang = obj.getString("id_cabang")

                    val row = TableRow(context).apply {
                        setPadding(8, 12, 8, 12)
                        setBackgroundColor(Color.WHITE)
                    }
                    row.addView(TextView(context).apply { text = idRombong; layoutParams = TableRow.LayoutParams(0, -2, 1f) })
                    row.addView(TextView(context).apply { text = namaLokasi; layoutParams = TableRow.LayoutParams(0, -2, 2f) })

                    val actionLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        layoutParams = TableRow.LayoutParams(0, -2, 1f)
                    }

                    val btnEdit = ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_menu_edit)
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { showDialogRombong(idRombong, idCabang) }
                    }
                    val btnDelete = ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_menu_delete)
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { deleteRombong(idRombong) }
                    }

                    actionLayout.addView(btnEdit)
                    actionLayout.addView(btnDelete)
                    row.addView(actionLayout)
                    tableRombong.addView(row)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showDialogCabang(id: String?, nama: String?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_kelola_cabang, null)
        val etId = dialogView.findViewById<EditText>(R.id.etIdCabang)
        val etNama = dialogView.findViewById<EditText>(R.id.etNamaLokasi)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpan)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)

        if (id != null) {
            tvTitle.text = "Update Cabang"
            etId.setText(id)
            etId.isEnabled = false
            etNama.setText(nama)
        }

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        btnBatal.setOnClickListener { dialog.dismiss() }
        btnSimpan.setOnClickListener {
            val params = mapOf(
                "mode" to (if (id == null) "insert" else "update"),
                "id_cabang" to etId.text.toString(),
                "nama_cabang" to etNama.text.toString()
            )
            postKeServer(requireContext(), apiUrl, params) {
                loadData(requireView())
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDialogRombong(idR: String?, idC: String?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tambah_rombong, null)
        val etId = dialogView.findViewById<EditText>(R.id.etIdRombong)
        val spCabang = dialogView.findViewById<Spinner>(R.id.spCabang)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpan)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)

        val listCabang = mutableListOf<String>()
        val listIdCabang = mutableListOf<String>()

        postKeServer(requireContext(), apiUrl, mapOf()) { response ->
            try {
                val data = JSONArray(response)
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    listIdCabang.add(obj.getString("ID_CABANG"))
                    listCabang.add("${obj.getString("NAMA_LOKASI")} (${obj.getString("ID_CABANG")})")
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listCabang)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spCabang.adapter = adapter

                if (idR != null) {
                    val index = listIdCabang.indexOf(idC)
                    if (index != -1) spCabang.setSelection(index)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        if (idR != null) {
            tvTitle.text = "Update Rombong"
            etId.setText(idR)
            etId.isEnabled = false
        }

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        btnBatal.setOnClickListener { dialog.dismiss() }
        btnSimpan.setOnClickListener {
            if (listIdCabang.isEmpty()) {
                Toast.makeText(context, "Tambah Cabang terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val params = mapOf(
                "mode" to (if (idR == null) "insert" else "update"),
                "id_rombong" to etId.text.toString(),
                "id_cabang" to listIdCabang[spCabang.selectedItemPosition]
            )
            postKeServer(requireContext(), apiRombongUrl, params) {
                loadData(requireView())
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun deleteCabang(id: String) {
        AlertDialog.Builder(context)
            .setTitle("Hapus Cabang")
            .setMessage("Apakah Anda yakin ingin menghapus cabang ini? Semua rombong di cabang ini juga akan terhapus.")
            .setPositiveButton("Ya") { _, _ ->
                postKeServer(requireContext(), apiUrl, mapOf("mode" to "delete", "id_cabang" to id)) {
                    loadData(requireView())
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun deleteRombong(id: String) {
        AlertDialog.Builder(context)
            .setTitle("Hapus Rombong")
            .setMessage("Yakin hapus unit ini?")
            .setPositiveButton("Ya") { _, _ ->
                postKeServer(requireContext(), apiRombongUrl, mapOf("mode" to "delete", "id_rombong" to id)) {
                    loadData(requireView())
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
}
