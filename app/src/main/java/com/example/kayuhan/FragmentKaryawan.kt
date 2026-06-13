package com.example.kayuhan

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.kayuhan.databinding.*
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog

// ─── DATA MODELS ───────────────────────────────────────────────────────────

data class Karyawan(
    val email: String, val idJabatan: Int, val idRombong: String,
    val nama: String, val noHp: String, val posisi: String
)

data class Jabatan(val id: Int, val nama: String, val gaji: Long, val bonus: Long)

data class DataGaji(
    val idGaji: String, val email: String, val nama: String, val periode: String,
    val gajiPokok: Long, val bonus: Long, val kompensasi: Long, val total: Long
)

data class JadwalShift(
    val idJadwal: Int, val email: String, val nama: String,
    val tanggal: String, val jamMulai: String, val jamSelesai: String, val lokasi: String
)

// ─── MAIN CONTAINER ────────────────────────────────────────────────────────

class FragmentKaryawan : Fragment() {
    private var _binding: ActivityFragmentKaryawanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityFragmentKaryawanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.adapter = KaryawanPagerAdapter(requireActivity())
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> "Karyawan"
                1 -> "Gaji"
                2 -> "Jadwal"
                3 -> "Jabatan"
                else -> null
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class KaryawanPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount() = 4
    override fun createFragment(pos: Int): Fragment = when (pos) {
        0 -> TabDataKaryawanFragment()
        1 -> TabDataGajiFragment()
        2 -> TabJadwalShiftFragment()
        3 -> TabJabatanFragment()
        else -> TabDataKaryawanFragment()
    }
}

// ─── TAB 1: MANAJEMEN KARYAWAN ─────────────────────────────────────────────

class TabDataKaryawanFragment : Fragment() {
    private var _binding: TabDataKaryawanBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DBOpenHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabDataKaryawanBinding.inflate(inflater, container, false)
        db = DBOpenHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvKaryawanAdmin.layoutManager = LinearLayoutManager(context)
        binding.rvKaryawanBarista.layoutManager = LinearLayoutManager(context)
        loadData()
        binding.btnTambahKaryawan.setOnClickListener { showKaryawanDialog(null) }
    }

    private fun loadData() {
        val listAll = mutableListOf<Map<String, String>>()
        val query = """
            SELECT k.email, k.nama, k.no_hp, k.id_rombong, k.posisi, c.nama_lokasi, k.id_jabatan
            FROM karyawan k
            LEFT JOIN rombong r ON k.id_rombong = r.id_rombong
            LEFT JOIN cabang c ON r.id_cabang = c.id_cabang
        """.trimIndent()

        val cursor = db.readableDatabase.rawQuery(query, null)
        while (cursor.moveToNext()) {
            listAll.add(mapOf(
                "email" to cursor.getString(0),
                "nama" to cursor.getString(1),
                "no_hp" to cursor.getString(2),
                "id_rombong" to (cursor.getString(3) ?: "-"),
                "posisi" to cursor.getString(4),
                "cabang" to (cursor.getString(5) ?: "-"),
                "id_jabatan" to cursor.getInt(6).toString()
            ))
        }
        cursor.close()

        val adminList = listAll.filter { it["posisi"] == "Admin" }
        val baristaList = listAll.filter { it["posisi"] == "Barista" }

        binding.rvKaryawanAdmin.adapter = KaryawanAdapter(adminList)
        binding.rvKaryawanBarista.adapter = KaryawanAdapter(baristaList)
    }

    private fun showKaryawanDialog(item: Karyawan?) {
        val d = DialogKelolaKaryawanBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(d.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val jabNames = mutableListOf<String>(); val jabIds = mutableListOf<Int>()
        val curJab = db.readableDatabase.rawQuery("SELECT id_jabatan, nama_jabatan FROM jabatan", null)
        while(curJab.moveToNext()){ jabIds.add(curJab.getInt(0)); jabNames.add(curJab.getString(1)) }
        curJab.close()
        d.spinnerJabatan.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jabNames))

        d.rgRole.setOnCheckedChangeListener { _, checkedId ->
            d.layoutLokasi.visibility = if (checkedId == d.rbAdmin.id) View.GONE else View.VISIBLE
        }

        val cabIds = mutableListOf<String>(); val cabNames = mutableListOf<String>()
        val curCab = db.readableDatabase.rawQuery("SELECT id_cabang, nama_lokasi FROM cabang", null)
        while(curCab.moveToNext()){ cabIds.add(curCab.getString(0)); cabNames.add(curCab.getString(1)) }
        curCab.close()
        d.spinnerCabang.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cabNames))

        d.spinnerCabang.setOnItemClickListener { _, _, position, _ ->
            val romList = mutableListOf<String>()
            val curR = db.readableDatabase.rawQuery("SELECT id_rombong FROM rombong WHERE id_cabang = ?", arrayOf(cabIds[position]))
            while(curR.moveToNext()) romList.add(curR.getString(0))
            curR.close()
            d.spinnerRombong.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, romList))
        }

        item?.let {
            d.etEmail.setText(it.email); d.etEmail.isEnabled = false
            d.etNamaLengkap.setText(it.nama)
            d.etNoHp.setText(it.noHp)
            if (it.posisi == "Admin") d.rbAdmin.isChecked = true else d.rbBarista.isChecked = true
            val jIdx = jabIds.indexOf(it.idJabatan)
            if (jIdx != -1) d.spinnerJabatan.setText(jabNames[jIdx], false)
            d.spinnerRombong.setText(it.idRombong, false)
        }

        d.btnSimpan.setOnClickListener {
            val role = if(d.rbAdmin.isChecked) "Admin" else "Barista"
            val selJab = jabNames.indexOf(d.spinnerJabatan.text.toString())

            val v = ContentValues().apply {
                put("email", d.etEmail.text.toString())
                put("nama", d.etNamaLengkap.text.toString())
                put("no_hp", d.etNoHp.text.toString())
                put("posisi", role)
                put("id_jabatan", if(selJab != -1) jabIds[selJab] else 0)
                put("id_rombong", if(role == "Admin") "" else d.spinnerRombong.text.toString())
            }

            if (item == null) db.writableDatabase.insert("karyawan", null, v)
            else db.writableDatabase.update("karyawan", v, "email=?", arrayOf(item.email))

            loadData(); dialog.dismiss()
        }
        d.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class KaryawanAdapter(val list: List<Map<String, String>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemViewType(pos: Int) = if (list[pos]["posisi"] == "Admin") 1 else 2

        override fun onCreateViewHolder(p: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(p.context)
            return if (viewType == 1) AdminVH(ItemKaryawanAdminBinding.inflate(inflater, p, false))
            else BaristaVH(ItemKaryawanBaristaBinding.inflate(inflater, p, false))
        }

        override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
            val m = list[pos]
            val obj = Karyawan(m["email"]!!, m["id_jabatan"]!!.toInt(), m["id_rombong"]!!, m["nama"]!!, m["no_hp"]!!, m["posisi"]!!)

            if (h is AdminVH) {
                h.b.tvEmail.text = m["email"]
                h.b.tvNama.text = m["nama"]
                h.b.tvNoHp.text = m["no_hp"]
                h.b.btnEdit.setOnClickListener { showKaryawanDialog(obj) }
                h.b.btnHapus.setOnClickListener { hapus(m["email"]!!) }
            } else if (h is BaristaVH) {
                h.b.tvEmail.text = m["email"]
                h.b.tvNama.text = m["nama"]
                h.b.tvNoHp.text = m["no_hp"]
                h.b.tvIdRombong.text = m["id_rombong"]
                h.b.tvCabang.text = m["cabang"]
                h.b.btnEdit.setOnClickListener { showKaryawanDialog(obj) }
                h.b.btnHapus.setOnClickListener { hapus(m["email"]!!) }
            }
        }

        private fun hapus(email: String) {
            AlertDialog.Builder(requireContext()).setTitle("Hapus?").setMessage("Yakin hapus $email?")
                .setPositiveButton("Ya") { _, _ ->
                    db.writableDatabase.delete("karyawan", "email=?", arrayOf(email))
                    loadData()
                }.setNegativeButton("Batal", null).show()
        }

        override fun getItemCount() = list.size
        inner class AdminVH(val b: ItemKaryawanAdminBinding) : RecyclerView.ViewHolder(b.root)
        inner class BaristaVH(val b: ItemKaryawanBaristaBinding) : RecyclerView.ViewHolder(b.root)
    }
}

// ─── TAB 2: MANAJEMEN GAJI ─────────────────────────────────────────────────

class TabDataGajiFragment : Fragment() {
    private var _binding: TabDataGajiBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DBOpenHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabDataGajiBinding.inflate(inflater, container, false)
        db = DBOpenHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvGaji.layoutManager = LinearLayoutManager(context)
        loadGaji()
        binding.btnTambahGaji.setOnClickListener { showGajiDialog(null) }
    }

    private fun loadGaji() {
        val listGaji = mutableListOf<DataGaji>()
        val cursor = db.readableDatabase.rawQuery(
            "SELECT g.*, IFNULL(k.nama, 'Tanpa Nama') FROM gaji g LEFT JOIN karyawan k ON g.email = k.email",
            null
        )
        while (cursor.moveToNext()) {
            listGaji.add(DataGaji(
                cursor.getString(0), cursor.getString(1), cursor.getString(7), cursor.getString(2),
                cursor.getLong(3), cursor.getLong(4), cursor.getLong(5), cursor.getLong(6)
            ))
        }
        cursor.close()
        binding.rvGaji.adapter = GajiAdapter(listGaji)
    }

    private fun showGajiDialog(item: DataGaji?) {
        val d = DialogKelolaGajiBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext()).setView(d.root).create()

        val karEmails = mutableListOf<String>()
        val karNames = mutableListOf<String>()
        val gajis = mutableListOf<Long>()
        val bonuses = mutableListOf<Long>()

        val cur = db.readableDatabase.rawQuery("SELECT k.email, k.nama, j.gaji_pokok_per_hari, j.bonus_percup FROM karyawan k JOIN jabatan j ON k.id_jabatan = j.id_jabatan", null)
        while(cur.moveToNext()){
            karEmails.add(cur.getString(0)); karNames.add(cur.getString(1))
            gajis.add(cur.getLong(2)); bonuses.add(cur.getLong(3))
        }
        cur.close()
        d.spinnerKaryawan.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, karNames))

        fun kalkulasi() {
            val idx = karNames.indexOf(d.spinnerKaryawan.text.toString())
            if(idx == -1) return
            val hari = d.etHariMasuk.text.toString().toLongOrNull() ?: 0L
            val cup = d.etJumlahCup.text.toString().toLongOrNull() ?: 0L
            d.etGajiPokok.setText((hari * gajis[idx]).toString())
            d.etTotalBonus.setText((cup * bonuses[idx]).toString())
        }

        d.spinnerKaryawan.setOnItemClickListener { _, _, _, _ -> kalkulasi() }
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { kalkulasi() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        d.etHariMasuk.addTextChangedListener(watcher)
        d.etJumlahCup.addTextChangedListener(watcher)

        d.btnSimpan.setOnClickListener {
            val idx = karNames.indexOf(d.spinnerKaryawan.text.toString())
            if(idx == -1) return@setOnClickListener

            val gapok = d.etGajiPokok.text.toString().toLongOrNull() ?: 0L
            val bonus = d.etTotalBonus.text.toString().toLongOrNull() ?: 0L
            val kompen = d.etKompensasi.text.toString().toLongOrNull() ?: 0L

            val v = ContentValues().apply {
                put("email", karEmails[idx])
                put("periode", d.etPeriode.text.toString())
                put("total_gaji_pokok", gapok)
                put("total_bonus", bonus)
                put("total_kompensasi", kompen)
                put("total_gaji_akhir", gapok + bonus + kompen)
            }
            if(item == null) {
                v.put("id_gaji", "G" + System.currentTimeMillis().toString().takeLast(5))
                db.writableDatabase.insert("gaji", null, v)
            } else {
                db.writableDatabase.update("gaji", v, "id_gaji=?", arrayOf(item.idGaji))
            }
            loadGaji(); dialog.dismiss()
        }
        d.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class GajiAdapter(val list: List<DataGaji>) : RecyclerView.Adapter<GajiAdapter.VH>() {
        inner class VH(val b: ItemGajiBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ItemGajiBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val i = list[pos]
            h.b.tvNo.text = (pos + 1).toString()
            h.b.tvNama.text = i.nama
            h.b.tvTotalGaji.text = "Rp ${i.total}"
            h.b.btnHapus.setOnClickListener { db.writableDatabase.delete("gaji", "id_gaji=?", arrayOf(i.idGaji)); loadGaji() }
        }
        override fun getItemCount() = list.size
    }
}

// ─── TAB 3: JADWAL SHIFT (INTEGRASI MYSQL ONLINE) ──────────────────────────

class TabJadwalShiftFragment : Fragment() {
    private var _binding: TabJadwalShiftBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DBOpenHelper

    private val urlWebServiceJadwal = "http://192.168.0.32/kayuhanmobile/query_jadwal.php"
    private val listJadwal = ArrayList<JadwalShift>()
    private lateinit var adapterJadwal: JadwalAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabJadwalShiftBinding.inflate(inflater, container, false)
        db = DBOpenHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvJadwal.layoutManager = LinearLayoutManager(context)
        adapterJadwal = JadwalAdapter(listJadwal)
        binding.rvJadwal.adapter = adapterJadwal

        loadJadwalDataFromMySQL()
        binding.btnBuatJadwal.setOnClickListener { showShiftDialog(null) }
    }

    private fun loadJadwalDataFromMySQL() {
        kotlin.concurrent.thread {
            try {
                val url = java.net.URL("$urlWebServiceJadwal?mode=select")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val responseText = conn.inputStream.bufferedReader().readText()
                val jsonArray = org.json.JSONArray(responseText)

                listJadwal.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    listJadwal.add(
                        JadwalShift(
                            obj.getInt("id_jadwal"),
                            obj.getString("EMAIL"),
                            obj.optString("nama_karyawan", "Karyawan"),
                            obj.getString("TANGGAL"),
                            obj.getString("JAM_MULAI"),
                            obj.getString("JAM_SELESAI"),
                            // FIX: Tampilkan NAMA_LOKASI dari hasil JOIN di PHP
                            obj.optString("NAMA_LOKASI", obj.optString("ID_CABANG", "-"))
                        )
                    )
                }

                activity?.runOnUiThread {
                    adapterJadwal.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showShiftDialog(item: JadwalShift?) {
        val d = DialogBuatJadwalBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(d.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // 1. Ambil list Karyawan dari SQLite lokal untuk Spinner
        val names = mutableListOf<String>()
        val emails = mutableListOf<String>()
        val curK = db.readableDatabase.rawQuery("SELECT email, nama FROM karyawan", null)
        while(curK.moveToNext()){
            emails.add(curK.getString(0))
            names.add(curK.getString(1))
        }
        curK.close()
        d.spinnerKaryawan.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))

        // 2. Ambil list Cabang dari SQLite lokal untuk Spinner
        // FIX: Simpan id_cabang juga, bukan hanya nama_lokasi
        val cabIds = mutableListOf<String>()
        val cabNames = mutableListOf<String>()
        val curC = db.readableDatabase.rawQuery("SELECT id_cabang, nama_lokasi FROM cabang", null)
        while(curC.moveToNext()){
            cabIds.add(curC.getString(0))
            cabNames.add(curC.getString(1))
        }
        curC.close()
        d.spinnerLokasi.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cabNames))

        // 3. Setup Dialog Date & Time Picker
        d.etTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, day ->
                d.etTanggal.setText(String.format("%d-%02d-%02d", y, m + 1, day))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        d.etJamMulai.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                d.etJamMulai.setText(String.format("%02d:%02d", h, m))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        d.etJamSelesai.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                d.etJamSelesai.setText(String.format("%02d:%02d", h, m))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        // Jika dalam Mode Edit, tampilkan data yang lama
        item?.let {
            d.spinnerKaryawan.setText(it.nama, false)
            d.spinnerLokasi.setText(it.lokasi, false)
            d.etTanggal.setText(it.tanggal)
            d.etJamMulai.setText(it.jamMulai)
            d.etJamSelesai.setText(it.jamSelesai)
        }

        d.btnSimpan.setOnClickListener {
            val selIdx = names.indexOf(d.spinnerKaryawan.text.toString())
            val lokasiIdx = cabNames.indexOf(d.spinnerLokasi.text.toString())
            val tanggal = d.etTanggal.text.toString()
            val jamMulai = d.etJamMulai.text.toString()
            val jamSelesai = d.etJamSelesai.text.toString()

            if (selIdx == -1 || lokasiIdx == -1 || tanggal.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = emails[selIdx]
            // FIX: Kirim id_cabang (misal "CBG-KDR1") bukan nama lokasi string
            val idCabang = cabIds[lokasiIdx]

            kotlin.concurrent.thread {
                try {
                    val url = java.net.URL(urlWebServiceJadwal)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true

                    val mode = if (item == null) "insert" else "update"
                    var params = "mode=$mode&email=${java.net.URLEncoder.encode(email, "UTF-8")}" +
                            "&id_cabang=${java.net.URLEncoder.encode(idCabang, "UTF-8")}" +
                            "&tanggal=${java.net.URLEncoder.encode(tanggal, "UTF-8")}" +
                            "&jam_mulai=${java.net.URLEncoder.encode(jamMulai, "UTF-8")}" +
                            "&jam_selesai=${java.net.URLEncoder.encode(jamSelesai, "UTF-8")}"

                    if (item != null) {
                        params += "&id_jadwal=${item.idJadwal}"
                    }

                    val writer = java.io.OutputStreamWriter(conn.outputStream)
                    writer.write(params)
                    writer.flush()
                    writer.close()

                    val res = conn.inputStream.bufferedReader().readText()
                    val obj = org.json.JSONObject(res)

                    activity?.runOnUiThread {
                        if (obj.getString("kode") == "000") {
                            Toast.makeText(requireContext(), "Jadwal berhasil disimpan ke MySQL", Toast.LENGTH_SHORT).show()
                            loadJadwalDataFromMySQL()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(requireContext(), "Gagal: ${obj.getString("pesan")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Koneksi Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        d.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class JadwalAdapter(val list: List<JadwalShift>) : RecyclerView.Adapter<JadwalAdapter.VH>() {
        inner class VH(val b: ItemJadwalBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(ItemJadwalBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val i = list[pos]
            h.b.tvIdJadwal.text = i.idJadwal.toString()
            h.b.tvNama.text = i.nama
            h.b.tvLokasi.text = i.lokasi
            h.b.tvTanggal.text = i.tanggal
            h.b.tvJam.text = "${i.jamMulai} - ${i.jamSelesai}"

            h.b.btnEdit.setOnClickListener { showShiftDialog(i) }
            h.b.btnHapus.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Jadwal")
                    .setMessage("Yakin ingin menghapus jadwal ID ${i.idJadwal} dari MySQL?")
                    .setPositiveButton("Ya") { _, _ ->
                        kotlin.concurrent.thread {
                            try {
                                val url = java.net.URL(urlWebServiceJadwal)
                                val conn = url.openConnection() as java.net.HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.doOutput = true

                                val params = "mode=delete&id_jadwal=${i.idJadwal}"
                                val writer = java.io.OutputStreamWriter(conn.outputStream)
                                writer.write(params)
                                writer.flush()
                                writer.close()

                                val res = conn.inputStream.bufferedReader().readText()
                                val obj = org.json.JSONObject(res)

                                activity?.runOnUiThread {
                                    if (obj.getString("kode") == "000") {
                                        Toast.makeText(requireContext(), "Jadwal terhapus dari MySQL", Toast.LENGTH_SHORT).show()
                                        loadJadwalDataFromMySQL()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── TAB 4: MANAJEMEN JABATAN ──────────────────────────────────────────────

class TabJabatanFragment : Fragment() {
    private var _binding: TabJabatanBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DBOpenHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabJabatanBinding.inflate(inflater, container, false)
        db = DBOpenHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvJabatan.layoutManager = LinearLayoutManager(requireContext())
        loadJabatan()
        binding.btnTambahJabatan.setOnClickListener { showJabatanDialog(null) }
    }

    private fun loadJabatan() {
        val listJabatan = mutableListOf<Jabatan>()
        val cur = db.readableDatabase.rawQuery("SELECT * FROM jabatan", null)
        while(cur.moveToNext()) {
            listJabatan.add(Jabatan(cur.getInt(0), cur.getString(1), cur.getLong(2), cur.getLong(3)))
        }
        cur.close()
        binding.rvJabatan.adapter = JabatanAdapter(listJabatan)
    }

    private fun showJabatanDialog(item: Jabatan?) {
        val d = DialogKelolaJabatanBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(d.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        item?.let {
            d.etNamaJabatan.setText(it.nama)
            d.etGajiPokok.setText(it.gaji.toString())
            d.etBonusCup.setText(it.bonus.toString())
        }

        d.btnSimpan.setOnClickListener {
            val v = ContentValues().apply {
                put("nama_jabatan", d.etNamaJabatan.text.toString())
                put("gaji_pokok_per_hari", d.etGajiPokok.text.toString().toLongOrNull() ?: 0L)
                put("bonus_percup", d.etBonusCup.text.toString().toLongOrNull() ?: 0L)
            }
            if (item == null) db.writableDatabase.insert("jabatan", null, v)
            else db.writableDatabase.update("jabatan", v, "id_jabatan=?", arrayOf(item.id.toString()))
            loadJabatan(); dialog.dismiss()
        }
        d.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class JabatanAdapter(val list: List<Jabatan>) : RecyclerView.Adapter<JabatanAdapter.VH>() {
        inner class VH(val b: ItemJabatanBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ItemJabatanBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val i = list[pos]
            h.b.tvNamaJabatan.text = i.nama
            h.b.tvGajiHari.text = "Rp ${i.gaji}"
            h.b.btnEdit.setOnClickListener { showJabatanDialog(i) }
            h.b.btnHapus.setOnClickListener {
                db.writableDatabase.delete("jabatan", "id_jabatan=?", arrayOf(i.id.toString()))
                loadJabatan()
            }
        }
        override fun getItemCount() = list.size
    }
}