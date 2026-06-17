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
    val email: String, val idJabatan: Int, val idRombong: String, val idCabang: String,
    val nama: String, val noHp: String, val posisi: String
)

data class Jabatan(val id: Int, val nama: String, val gaji: Long, val bonus: Long)

data class DataGaji(
    val idGaji: String, val email: String, val nama: String, val periode: String,
    val gajiPokok: Long, val bonus: Long, val kompensasi: Long, val total: Long
)

data class JadwalShift(
    val idJadwal: String, val email: String, val nama: String,
    val tanggal: String, val jamMulai: String, val jamSelesai: String, val lokasi: String,
    val idCabang: String
)

// ─── UTILS ──────────────────────────────────────────────────────────────────

fun postKeServer(context: android.content.Context, url: String, params: Map<String, String>, callback: (String) -> Unit) {
    val queue = com.android.volley.toolbox.Volley.newRequestQueue(context)
    val request = object : com.android.volley.toolbox.StringRequest(
        Method.POST, url,
        { response -> callback(response) },
        { error -> 
            android.util.Log.e("API_ERROR", "Error at $url: ${error.message}")
            android.widget.Toast.makeText(context, "Koneksi Bermasalah", android.widget.Toast.LENGTH_SHORT).show()
        }
    ) {
        override fun getParams(): MutableMap<String, String> = params.toMutableMap()
    }
    queue.add(request)
}

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
                0 -> "Data Karyawan"
                1 -> "Data Gaji"
                2 -> "Jadwal Shift"
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
    private val apiUrl = ApiConfig.KARYAWAN

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabDataKaryawanBinding.inflate(inflater, container, false)
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
        postKeServer(requireContext(), apiUrl, mapOf("action" to "list")) { response ->
            try {
                val json = org.json.JSONObject(response)
                if (json.getString("status") == "success") {
                    val data = json.getJSONArray("data")
                    val listAll = mutableListOf<Map<String, String>>()
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        listAll.add(mapOf(
                            "email" to if (obj.isNull("email")) "" else obj.getString("email"),
                            "nama" to if (obj.isNull("nama")) "" else obj.getString("nama"),
                            "no_hp" to if (obj.isNull("no_hp")) "" else obj.getString("no_hp"),
                            "id_rombong" to if (obj.isNull("id_rombong")) "-" else obj.getString("id_rombong"),
                            "posisi" to if (obj.isNull("posisi")) "" else obj.getString("posisi"),
                            "cabang" to if (obj.isNull("nama_lokasi")) "-" else obj.getString("nama_lokasi"),
                            "id_jabatan" to if (obj.isNull("id_jabatan")) "0" else obj.getString("id_jabatan"),
                            "id_cabang" to if (obj.isNull("id_cabang")) "-" else obj.getString("id_cabang")
                        ))
                    }
                    val adminList = listAll.filter { it["posisi"] == "Admin" }
                    val baristaList = listAll.filter { it["posisi"] == "Barista" }
                    binding.rvKaryawanAdmin.adapter = KaryawanAdapter(adminList)
                    binding.rvKaryawanBarista.adapter = KaryawanAdapter(baristaList)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showKaryawanDialog(item: Karyawan?) {
        val d = DialogKelolaKaryawanBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(d.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val jabNames = mutableListOf<String>(); val jabIds = mutableListOf<Int>()
        postKeServer(requireContext(), ApiConfig.JABATAN, mapOf("action" to "list")) { res ->
            val data = org.json.JSONObject(res).getJSONArray("data")
            for(i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                jabIds.add(o.getInt("id_jabatan")); jabNames.add(o.getString("nama_jabatan"))
            }
            d.spinnerJabatan.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jabNames))
            item?.let {
                val jIdx = jabIds.indexOf(it.idJabatan)
                if (jIdx != -1) d.spinnerJabatan.setText(jabNames[jIdx], false)
            }
        }

        d.rgRole.setOnCheckedChangeListener { _, checkedId ->
            d.layoutLokasi.visibility = if (checkedId == d.rbAdmin.id) View.GONE else View.VISIBLE
        }

        // Load Cabang from Server
        val cabNames = mutableListOf<String>(); val cabIds = mutableListOf<String>()
        postKeServer(requireContext(), ApiConfig.LOKASI, mapOf()) { res ->
            try {
                val data = org.json.JSONArray(res)
                for(i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    cabIds.add(o.getString("ID_CABANG"))
                    cabNames.add(o.getString("NAMA_LOKASI"))
                }
                d.spinnerCabang.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cabNames))
                item?.let {
                    val cIdx = cabIds.indexOf(it.idCabang)
                    if (cIdx != -1) d.spinnerCabang.setText(cabNames[cIdx], false)
                }
            } catch(e: Exception) { e.printStackTrace() }
        }

        // Load Rombong from Server
        postKeServer(requireContext(), ApiConfig.ROMBONG, mapOf("action" to "list")) { res ->
            val data = org.json.JSONObject(res).getJSONArray("data")
            val romList = mutableListOf<String>()
            for(i in 0 until data.length()) romList.add(data.getJSONObject(i).getString("id_rombong"))
            d.spinnerRombong.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, romList))
            item?.let { d.spinnerRombong.setText(it.idRombong, false) }
        }

        item?.let {
            d.etEmail.setText(it.email); d.etEmail.isEnabled = false
            d.etNamaLengkap.setText(it.nama)
            d.etNoHp.setText(it.noHp)
            if (it.posisi == "Admin") d.rbAdmin.isChecked = true else d.rbBarista.isChecked = true
        }

        d.btnSimpan.setOnClickListener {
            val role = if(d.rbAdmin.isChecked) "Admin" else "Barista"
            val selJab = jabNames.indexOf(d.spinnerJabatan.text.toString())
            val selCab = cabNames.indexOf(d.spinnerCabang.text.toString())
            val params = mapOf(
                "action" to (if(item == null) "insert" else "update"),
                "email" to d.etEmail.text.toString(),
                "nama" to d.etNamaLengkap.text.toString(),
                "no_hp" to d.etNoHp.text.toString(),
                "posisi" to role,
                "id_jabatan" to (if(selJab != -1) jabIds[selJab] else 0).toString(),
                "id_rombong" to (if(role == "Admin") "" else d.spinnerRombong.text.toString()),
                "id_cabang" to (if(role == "Admin") "" else (if(selCab != -1) cabIds[selCab] else ""))
            )
            postKeServer(requireContext(), apiUrl, params) { loadData(); dialog.dismiss() }
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
            val obj = Karyawan(m["email"]!!, m["id_jabatan"]!!.toInt(), m["id_rombong"]!!, m["id_cabang"]!!, m["nama"]!!, m["no_hp"]!!, m["posisi"]!!)

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
                    postKeServer(requireContext(), apiUrl, mapOf("action" to "delete", "email" to email)) { loadData() }
                }.setNegativeButton("Batal", null).show()
        }

        override fun getItemCount() = list.size
        inner class AdminVH(val b: ItemKaryawanAdminBinding) : RecyclerView.ViewHolder(b.root)
        inner class BaristaVH(val b: ItemKaryawanBaristaBinding) : RecyclerView.ViewHolder(b.root)
    }
}

// ─── TAB 5: PERSETUJUAN GAJI BARISTA ────────────────────────────────────────

data class PengajuanGaji(
    val id: Int,
    val email: String,
    val nama: String,
    val tanggal: String,
    val nominal: Double,
    val status: String,
    val tipe: String,
    val catatan: String
)

class TabPersetujuanGajiFragment : Fragment() {
    private lateinit var rvPengajuan: RecyclerView
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var btnFilterMenunggu: com.google.android.material.button.MaterialButton
    private lateinit var btnFilterDisetujui: com.google.android.material.button.MaterialButton
    private lateinit var btnFilterDitolak: com.google.android.material.button.MaterialButton

    private val apiUrl = ApiConfig.GAJI
    private val listPengajuan = ArrayList<PengajuanGaji>()
    private var currentFilter = "menunggu"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.tab_persetujuan_gaji, container, false)
        rvPengajuan = view.findViewById(R.id.rvPengajuan)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnFilterMenunggu = view.findViewById(R.id.btnFilterMenunggu)
        btnFilterDisetujui = view.findViewById(R.id.btnFilterDisetujui)
        btnFilterDitolak = view.findViewById(R.id.btnFilterDitolak)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvPengajuan.layoutManager = LinearLayoutManager(requireContext())

        btnFilterMenunggu.setOnClickListener { setFilter("menunggu") }
        btnFilterDisetujui.setOnClickListener { setFilter("disetujui") }
        btnFilterDitolak.setOnClickListener { setFilter("ditolak") }

        loadPengajuan()
    }

    private fun setFilter(status: String) {
        currentFilter = status
        btnFilterMenunggu.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (status == "menunggu") android.graphics.Color.parseColor("#F39C12") else android.graphics.Color.parseColor("#7F8C8D")
        )
        btnFilterDisetujui.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (status == "disetujui") android.graphics.Color.parseColor("#27AE60") else android.graphics.Color.parseColor("#7F8C8D")
        )
        btnFilterDitolak.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (status == "ditolak") android.graphics.Color.parseColor("#E53935") else android.graphics.Color.parseColor("#7F8C8D")
        )
        loadPengajuan()
    }

    private fun loadPengajuan() {
        postKeServer(requireContext(), apiUrl, mapOf("action" to "list_pengajuan", "status" to currentFilter)) { response ->
            try {
                val json = org.json.JSONObject(response)
                if (json.optString("status") == "success") {
                    listPengajuan.clear()
                    val data = json.getJSONArray("data")
                    for (i in 0 until data.length()) {
                        val o = data.getJSONObject(i)
                        listPengajuan.add(PengajuanGaji(
                            o.getInt("id"),
                            o.optString("email"),
                            o.optString("nama", "?"),
                            o.optString("tanggal"),
                            o.optDouble("nominal", 0.0),
                            o.optString("status"),
                            o.optString("tipe"),
                            o.optString("catatan", "")
                        ))
                    }
                    if (listPengajuan.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvPengajuan.visibility = View.GONE
                        tvEmpty.text = when(currentFilter) {
                            "menunggu" -> "Tidak ada pengajuan yang menunggu persetujuan"
                            "disetujui" -> "Belum ada pengajuan yang disetujui"
                            "ditolak" -> "Belum ada pengajuan yang ditolak"
                            else -> "Tidak ada data"
                        }
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvPengajuan.visibility = View.VISIBLE
                    }
                    rvPengajuan.adapter = PengajuanAdapter(listPengajuan, currentFilter == "menunggu")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prosesApproval(item: PengajuanGaji, approve: Boolean) {
        val actionName = if (item.tipe == "pengambilan") {
            if (approve) "terima_pengambilan" else "tolak_pengambilan"
        } else {
            if (approve) "terima_penyimpanan" else "tolak_penyimpanan"
        }

        val verb = if (approve) "menyetujui" else "menolak"
        val tipeName = if (item.tipe == "pengambilan") "pengambilan" else "penyimpanan"

        AlertDialog.Builder(requireContext())
            .setTitle(if (approve) "Setujui Pengajuan" else "Tolak Pengajuan")
            .setMessage("Apakah Anda yakin ingin $verb $tipeName gaji ${item.nama} sebesar Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).format(item.nominal)}?")
            .setPositiveButton("Ya") { _, _ ->
                postKeServer(requireContext(), apiUrl, mapOf(
                    "action" to actionName,
                    "id" to item.id.toString(),
                    "catatan" to ""
                )) { response ->
                    try {
                        val json = org.json.JSONObject(response)
                        val msg = json.optString("message", "Berhasil")
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        loadPengajuan()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    inner class PengajuanAdapter(
        private val list: List<PengajuanGaji>,
        private val showActions: Boolean
    ) : RecyclerView.Adapter<PengajuanAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNama: android.widget.TextView = itemView.findViewById(R.id.tvNamaPengajuan)
            val tvTipe: android.widget.TextView = itemView.findViewById(R.id.tvTipePengajuan)
            val tvTanggal: android.widget.TextView = itemView.findViewById(R.id.tvTanggalPengajuan)
            val tvNominal: android.widget.TextView = itemView.findViewById(R.id.tvNominalPengajuan)
            val tvStatus: android.widget.TextView = itemView.findViewById(R.id.tvStatusPengajuan)
            val layoutAksi: android.widget.LinearLayout = itemView.findViewById(R.id.layoutAksi)
            val btnSetujui: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnSetujui)
            val btnTolak: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnTolak)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pengajuan_gaji, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))

            holder.tvNama.text = item.nama
            holder.tvTanggal.text = item.tanggal
            holder.tvNominal.text = "Rp " + formatter.format(item.nominal)

            // Tipe badge
            val tipeBg = android.graphics.drawable.GradientDrawable()
            tipeBg.cornerRadius = 12f
            if (item.tipe == "pengambilan") {
                tipeBg.setColor(android.graphics.Color.parseColor("#2980B9"))
                holder.tvTipe.text = "Pengambilan"
            } else {
                tipeBg.setColor(android.graphics.Color.parseColor("#E67E22"))
                holder.tvTipe.text = "Penyimpanan"
            }
            holder.tvTipe.background = tipeBg

            // Status badge (visible for non-menunggu)
            if (!showActions) {
                holder.tvStatus.visibility = View.VISIBLE
                val statusBg = android.graphics.drawable.GradientDrawable()
                statusBg.cornerRadius = 12f
                when (item.status) {
                    "disetujui" -> statusBg.setColor(android.graphics.Color.parseColor("#27AE60"))
                    "ditolak" -> statusBg.setColor(android.graphics.Color.parseColor("#E53935"))
                    else -> statusBg.setColor(android.graphics.Color.parseColor("#F39C12"))
                }
                holder.tvStatus.background = statusBg
                holder.tvStatus.text = item.status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                }
                holder.layoutAksi.visibility = View.GONE
            } else {
                holder.tvStatus.visibility = View.GONE
                holder.layoutAksi.visibility = View.VISIBLE
                holder.btnSetujui.setOnClickListener { prosesApproval(item, true) }
                holder.btnTolak.setOnClickListener { prosesApproval(item, false) }
            }
        }

        override fun getItemCount() = list.size
    }
}

// ─── TAB 2: MANAJEMEN GAJI ─────────────────────────────────────────────────

class TabDataGajiFragment : Fragment() {
    private var _binding: TabDataGajiBinding? = null
    private val binding get() = _binding!!
    private val apiUrl = ApiConfig.GAJI

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabDataGajiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvGaji.layoutManager = LinearLayoutManager(context)
        loadGaji()

        // 3 sub-menu buttons
        binding.btnPengambilanGaji.setOnClickListener { showPengajuanListDialog("pengambilan") }
        binding.btnPenyimpananGaji.setOnClickListener { showPengajuanListDialog("penyimpanan") }
        binding.btnTabungan.setOnClickListener { showTabunganDialog() }
    }

    private fun formatRp(value: Long): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))
        return "Rp ${formatter.format(value)}"
    }

    private fun loadGaji() {
        postKeServer(requireContext(), apiUrl, mapOf("action" to "list")) { response ->
            try {
                val data = org.json.JSONObject(response).getJSONArray("data")
                val listGaji = mutableListOf<DataGaji>()
                for(i in 0 until data.length()){
                    val o = data.getJSONObject(i)
                    listGaji.add(DataGaji(
                        o.optString("id_gaji", "${o.getString("email")}-${o.getString("periode")}"),
                        o.getString("email"), o.getString("nama_karyawan"), o.getString("periode"),
                        o.optDouble("total_gaji_pokok", 0.0).toLong(),
                        o.optDouble("total_bonus", 0.0).toLong(),
                        o.optDouble("total_kompensasi", 0.0).toLong(),
                        o.optDouble("total_gaji_akhir", 0.0).toLong()
                    ))
                }
                binding.rvGaji.adapter = GajiAdapter(listGaji)
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    private fun showDetailDialog(item: DataGaji) {
        val msg = """
            Nama: ${item.nama}
            Periode: ${item.periode}
            Gaji Pokok: ${formatRp(item.gajiPokok)}
            Bonus: ${formatRp(item.bonus)}
            Kompensasi: ${formatRp(item.kompensasi)}
            Total Gaji Akhir: ${formatRp(item.total)}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Gaji - ${item.nama}")
            .setMessage(msg)
            .setPositiveButton("Tutup", null)
            .show()
    }

    // ─── Pengambilan / Penyimpanan Approval Dialog ─────────────────

    private fun showPengajuanListDialog(tipeFilter: String) {
        val title = if (tipeFilter == "pengambilan") "Pengambilan Gaji" else "Penyimpanan Gaji"
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.tab_persetujuan_gaji, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Tutup", null)
            .create()

        val rvPengajuan: RecyclerView = dialogView.findViewById(R.id.rvPengajuan)
        val tvEmpty: android.widget.TextView = dialogView.findViewById(R.id.tvEmpty)
        val btnMenunggu: com.google.android.material.button.MaterialButton = dialogView.findViewById(R.id.btnFilterMenunggu)
        val btnDisetujui: com.google.android.material.button.MaterialButton = dialogView.findViewById(R.id.btnFilterDisetujui)
        val btnDitolak: com.google.android.material.button.MaterialButton = dialogView.findViewById(R.id.btnFilterDitolak)

        rvPengajuan.layoutManager = LinearLayoutManager(requireContext())
        var currentStatus = "menunggu"

        fun updateFilterColors(status: String) {
            btnMenunggu.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (status == "menunggu") android.graphics.Color.parseColor("#F39C12") else android.graphics.Color.parseColor("#7F8C8D")
            )
            btnDisetujui.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (status == "disetujui") android.graphics.Color.parseColor("#27AE60") else android.graphics.Color.parseColor("#7F8C8D")
            )
            btnDitolak.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (status == "ditolak") android.graphics.Color.parseColor("#E53935") else android.graphics.Color.parseColor("#7F8C8D")
            )
        }

        fun loadList(status: String) {
            currentStatus = status
            updateFilterColors(status)
            postKeServer(requireContext(), apiUrl, mapOf("action" to "list_pengajuan", "status" to status)) { response ->
                try {
                    val json = org.json.JSONObject(response)
                    if (json.optString("status") == "success") {
                        val dataArr = json.getJSONArray("data")
                        val items = mutableListOf<PengajuanGaji>()
                        for (i in 0 until dataArr.length()) {
                            val o = dataArr.getJSONObject(i)
                            if (o.optString("tipe") == tipeFilter) {
                                items.add(PengajuanGaji(
                                    o.getInt("id"), o.optString("email"), o.optString("nama", "?"),
                                    o.optString("tanggal"), o.optDouble("nominal", 0.0),
                                    o.optString("status"), o.optString("tipe"), o.optString("catatan", "")
                                ))
                            }
                        }
                        if (items.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            rvPengajuan.visibility = View.GONE
                            tvEmpty.text = "Tidak ada data $title dengan status $status"
                        } else {
                            tvEmpty.visibility = View.GONE
                            rvPengajuan.visibility = View.VISIBLE
                        }
                        rvPengajuan.adapter = PengajuanDialogAdapter(items, status == "menunggu") { loadList(currentStatus) }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        btnMenunggu.setOnClickListener { loadList("menunggu") }
        btnDisetujui.setOnClickListener { loadList("disetujui") }
        btnDitolak.setOnClickListener { loadList("ditolak") }

        loadList("menunggu")
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // ─── Tabungan Dialog ───────────────────────────────────────────

    private fun showTabunganDialog() {
        postKeServer(requireContext(), ApiConfig.KARYAWAN, mapOf("action" to "list")) { kRes ->
            try {
                val kData = org.json.JSONObject(kRes).getJSONArray("data")
                val sb = StringBuilder()
                var count = 0
                val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))

                for (i in 0 until kData.length()) {
                    val karyawan = kData.getJSONObject(i)
                    val email = karyawan.getString("email")
                    val nama = karyawan.getString("nama")

                    // Get tabungan via barista_info
                    postKeServer(requireContext(), apiUrl, mapOf("action" to "barista_info", "email" to email)) { infoRes ->
                        count++
                        try {
                            val info = org.json.JSONObject(infoRes)
                            if (info.optString("status") == "success") {
                                val tabungan = info.optDouble("total_tabungan", 0.0)
                                if (tabungan > 0) {
                                    sb.append("• $nama: Rp ${formatter.format(tabungan)}\n")
                                }
                            }
                        } catch (_: Exception) {}

                        if (count >= kData.length()) {
                            requireActivity().runOnUiThread {
                                val finalText = if (sb.isEmpty()) "Belum ada data tabungan karyawan" else sb.toString().trimEnd()
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Tabungan Karyawan")
                                    .setMessage(finalText)
                                    .setPositiveButton("Tutup", null)
                                    .show()
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ─── Approval Process ──────────────────────────────────────────

    private fun prosesApproval(item: PengajuanGaji, approve: Boolean, onDone: () -> Unit) {
        val actionName = if (item.tipe == "pengambilan") {
            if (approve) "terima_pengambilan" else "tolak_pengambilan"
        } else {
            if (approve) "terima_penyimpanan" else "tolak_penyimpanan"
        }
        val verb = if (approve) "menyetujui" else "menolak"
        val tipeName = if (item.tipe == "pengambilan") "pengambilan" else "penyimpanan"
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))

        AlertDialog.Builder(requireContext())
            .setTitle(if (approve) "Setujui Pengajuan" else "Tolak Pengajuan")
            .setMessage("Apakah Anda yakin ingin $verb $tipeName gaji ${item.nama} sebesar Rp ${formatter.format(item.nominal)}?")
            .setPositiveButton("Ya") { _, _ ->
                postKeServer(requireContext(), apiUrl, mapOf(
                    "action" to actionName, "id" to item.id.toString(), "catatan" to ""
                )) { response ->
                    try {
                        val json = org.json.JSONObject(response)
                        Toast.makeText(requireContext(), json.optString("message", "Berhasil"), Toast.LENGTH_LONG).show()
                        onDone()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── Adapters ──────────────────────────────────────────────────

    inner class GajiAdapter(val list: List<DataGaji>) : RecyclerView.Adapter<GajiAdapter.VH>() {
        inner class VH(val b: ItemGajiBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ItemGajiBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val i = list[pos]
            h.b.tvNo.text = (pos + 1).toString()
            h.b.tvNama.text = i.nama
            h.b.tvPeriode.text = i.periode
            h.b.tvGajiPokok.text = formatRp(i.gajiPokok)
            h.b.tvBonus.text = formatRp(i.bonus)
            h.b.tvKompensasi.text = formatRp(i.kompensasi)
            h.b.tvTotalGaji.text = formatRp(i.total)
            h.b.btnEdit.setOnClickListener { showDetailDialog(i) }
            h.b.btnHapus.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Data Gaji")
                    .setMessage("Apakah Anda yakin ingin menghapus data gaji ${i.nama}?")
                    .setPositiveButton("Ya") { _, _ ->
                        postKeServer(requireContext(), apiUrl, mapOf("action" to "delete", "id_gaji" to i.idGaji)) { loadGaji() }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
        override fun getItemCount() = list.size
    }

    inner class PengajuanDialogAdapter(
        private val list: List<PengajuanGaji>,
        private val showActions: Boolean,
        private val onRefresh: () -> Unit
    ) : RecyclerView.Adapter<PengajuanDialogAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNama: android.widget.TextView = itemView.findViewById(R.id.tvNamaPengajuan)
            val tvTipe: android.widget.TextView = itemView.findViewById(R.id.tvTipePengajuan)
            val tvTanggal: android.widget.TextView = itemView.findViewById(R.id.tvTanggalPengajuan)
            val tvNominal: android.widget.TextView = itemView.findViewById(R.id.tvNominalPengajuan)
            val tvStatus: android.widget.TextView = itemView.findViewById(R.id.tvStatusPengajuan)
            val layoutAksi: android.widget.LinearLayout = itemView.findViewById(R.id.layoutAksi)
            val btnSetujui: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnSetujui)
            val btnTolak: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnTolak)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_pengajuan_gaji, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))

            holder.tvNama.text = item.nama
            holder.tvTanggal.text = item.tanggal
            holder.tvNominal.text = "Rp " + formatter.format(item.nominal)

            val tipeBg = android.graphics.drawable.GradientDrawable()
            tipeBg.cornerRadius = 12f
            if (item.tipe == "pengambilan") {
                tipeBg.setColor(android.graphics.Color.parseColor("#2980B9"))
                holder.tvTipe.text = "Pengambilan"
            } else {
                tipeBg.setColor(android.graphics.Color.parseColor("#E67E22"))
                holder.tvTipe.text = "Penyimpanan"
            }
            holder.tvTipe.background = tipeBg

            if (!showActions) {
                holder.tvStatus.visibility = View.VISIBLE
                val statusBg = android.graphics.drawable.GradientDrawable()
                statusBg.cornerRadius = 12f
                when (item.status) {
                    "disetujui" -> statusBg.setColor(android.graphics.Color.parseColor("#27AE60"))
                    "ditolak" -> statusBg.setColor(android.graphics.Color.parseColor("#E53935"))
                    else -> statusBg.setColor(android.graphics.Color.parseColor("#F39C12"))
                }
                holder.tvStatus.background = statusBg
                holder.tvStatus.text = item.status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                }
                holder.layoutAksi.visibility = View.GONE
            } else {
                holder.tvStatus.visibility = View.GONE
                holder.layoutAksi.visibility = View.VISIBLE
                holder.btnSetujui.setOnClickListener { prosesApproval(item, true, onRefresh) }
                holder.btnTolak.setOnClickListener { prosesApproval(item, false, onRefresh) }
            }
        }

        override fun getItemCount() = list.size
    }
}

// ─── TAB 3: JADWAL SHIFT (INTEGRASI MYSQL ONLINE) ──────────────────────────

class TabJadwalShiftFragment : Fragment() {
    private var _binding: TabJadwalShiftBinding? = null
    private val binding get() = _binding!!

    private val apiUrl = ApiConfig.JADWAL
    private val listJadwal = ArrayList<JadwalShift>()
    private lateinit var adapterJadwal: JadwalAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabJadwalShiftBinding.inflate(inflater, container, false)
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
        postKeServer(requireContext(), apiUrl, mapOf("mode" to "select")) { response ->
            try {
                val jsonArray = org.json.JSONArray(response)
                listJadwal.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    listJadwal.add(
                        JadwalShift(
                            if (obj.isNull("id_jadwal")) "" else obj.getString("id_jadwal"),
                            if (obj.isNull("EMAIL")) "" else obj.getString("EMAIL"),
                            if (obj.isNull("nama_karyawan")) "Karyawan" else obj.getString("nama_karyawan"),
                            if (obj.isNull("TANGGAL")) "" else obj.getString("TANGGAL"),
                            if (obj.isNull("JAM_MULAI")) "" else obj.getString("JAM_MULAI"),
                            if (obj.isNull("JAM_SELESAI")) "" else obj.getString("JAM_SELESAI"),
                            if (obj.isNull("NAMA_LOKASI")) (if (obj.isNull("ID_CABANG")) "-" else obj.getString("ID_CABANG")) else obj.getString("NAMA_LOKASI"),
                            if (obj.isNull("ID_CABANG")) "" else obj.getString("ID_CABANG")
                        )
                    )
                }
                adapterJadwal.notifyDataSetChanged()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showShiftDialog(item: JadwalShift?) {
        val d = DialogBuatJadwalBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(d.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val names = mutableListOf<String>(); val emails = mutableListOf<String>()
        postKeServer(requireContext(), ApiConfig.KARYAWAN, mapOf("action" to "list")) { res ->
            val data = org.json.JSONObject(res).getJSONArray("data")
            for(i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                emails.add(o.getString("email")); names.add(o.getString("nama"))
            }
            d.spinnerKaryawan.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))
            item?.let { d.spinnerKaryawan.setText(it.nama, false) }
        }

        val cabIds = mutableListOf<String>(); val cabNames = mutableListOf<String>()
        postKeServer(requireContext(), ApiConfig.LOKASI, mapOf()) { res ->
            val data = org.json.JSONArray(res)
            for(i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                cabIds.add(o.getString("ID_CABANG")); cabNames.add(o.getString("NAMA_LOKASI"))
            }
            d.spinnerLokasi.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cabNames))
            item?.let { d.spinnerLokasi.setText(it.lokasi, false) }
        }

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
            val tanggal = d.etTanggal.text.toString()
            val jamMulai = d.etJamMulai.text.toString()
            val jamSelesai = d.etJamSelesai.text.toString()

            val selIdx = names.indexOf(d.spinnerKaryawan.text.toString())
            val lokasiIdx = cabNames.indexOf(d.spinnerLokasi.text.toString())

            var emailParam = ""
            var cabangParam = ""

            if (selIdx != -1) {
                emailParam = emails[selIdx]
            } else if (item != null && d.spinnerKaryawan.text.toString() == item.nama) {
                emailParam = item.email
            }

            if (lokasiIdx != -1) {
                cabangParam = cabIds[lokasiIdx]
            } else if (item != null && (d.spinnerLokasi.text.toString() == item.lokasi || d.spinnerLokasi.text.toString() == item.idCabang)) {
                cabangParam = item.idCabang
            }

            if (emailParam.isEmpty() || cabangParam.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val params = mutableMapOf(
                "mode" to (if (item == null) "insert" else "update"),
                "email" to emailParam,
                "id_cabang" to cabangParam,
                "tanggal" to tanggal,
                "jam_mulai" to jamMulai,
                "jam_selesai" to jamSelesai
            )
            item?.let { params["id_jadwal"] = it.idJadwal }

            postKeServer(requireContext(), apiUrl, params) {
                loadJadwalDataFromMySQL()
                dialog.dismiss()
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
            h.b.tvIdJadwal.text = i.idJadwal
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
                        postKeServer(requireContext(), apiUrl, mapOf("mode" to "delete", "id_jadwal" to i.idJadwal)) {
                            loadJadwalDataFromMySQL()
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
    private val apiUrl = ApiConfig.JABATAN

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabJabatanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvJabatan.layoutManager = LinearLayoutManager(requireContext())
        loadJabatan()
        binding.btnTambahJabatan.setOnClickListener { showJabatanDialog(null) }
    }

    private fun loadJabatan() {
        postKeServer(requireContext(), apiUrl, mapOf("action" to "list")) { res ->
            try {
                val data = org.json.JSONObject(res).getJSONArray("data")
                val listJabatan = mutableListOf<Jabatan>()
                for(i in 0 until data.length()){
                    val o = data.getJSONObject(i)
                    listJabatan.add(Jabatan(o.getInt("id_jabatan"), o.getString("nama_jabatan"), o.getLong("gaji_per_jam"), o.getLong("bonus_percup")))
                }
                binding.rvJabatan.adapter = JabatanAdapter(listJabatan)
            } catch(e: Exception) { e.printStackTrace() }
        }
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
            val params = mapOf(
                "action" to (if(item == null) "insert" else "update"),
                "id_jabatan" to (item?.id?.toString() ?: "0"),
                "nama_jabatan" to d.etNamaJabatan.text.toString(),
                "gaji_per_jam" to (d.etGajiPokok.text.toString().toLongOrNull() ?: 0L).toString(),
                "bonus_percup" to (d.etBonusCup.text.toString().toLongOrNull() ?: 0L).toString()
            )
            postKeServer(requireContext(), apiUrl, params) { loadJabatan(); dialog.dismiss() }
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
            h.b.tvGajiHari.text = "Rp ${i.gaji}/jam" // Updated Label
            h.b.btnEdit.setOnClickListener { showJabatanDialog(i) }
            h.b.btnHapus.setOnClickListener {
                postKeServer(requireContext(), apiUrl, mapOf("action" to "delete", "id_jabatan" to i.id.toString())) { loadJabatan() }
            }
        }
        override fun getItemCount() = list.size
    }
}
