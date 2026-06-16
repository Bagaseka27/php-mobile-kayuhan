package com.example.kayuhan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.json.JSONArray
import java.util.*

class FragmentTransaksi : Fragment() {

    private lateinit var listView: ListView
    private lateinit var tvTotal: TextView
    private val apiUrl = "http://192.168.0.109/php-mobile-kayuhan/transaksi_action.php"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_fragment_transaksi, container, false)

        listView = view.findViewById(R.id.listTransaksi)
        tvTotal = view.findViewById(R.id.tvTotalPendapatan)
        val etDari: EditText = view.findViewById(R.id.etDari)
        val etSampai: EditText = view.findViewById(R.id.etSampai)
        val btnTampil: Button = view.findViewById(R.id.btnTampil)

        val calendar = Calendar.getInstance()

        fun showDatePicker(editText: EditText) {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
                    editText.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fun setupDateTimePicker(editText: EditText) {
            editText.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val drawableRight = 2
                    if (editText.compoundDrawables[drawableRight] != null) {
                        if (event.rawX >= (editText.right - editText.compoundDrawables[drawableRight].bounds.width() - editText.paddingEnd)) {
                            showDatePicker(editText)
                            return@setOnTouchListener true
                        }
                    }
                }
                false
            }
        }

        setupDateTimePicker(etDari)
        setupDateTimePicker(etSampai)

        btnTampil.setOnClickListener {
            val dari = etDari.text.toString()
            val sampai = etSampai.text.toString()
            tampilData(dari, sampai)
        }

        tampilData()

        return view
    }

    private fun tampilData(dari: String = "", sampai: String = "") {
        val email = when (val act = activity) {
            is DashboardBaristaActivity -> act.intent.getStringExtra("EXTRA_EMAIL") ?: ""
            is MainActivity -> act.intent.getStringExtra("EXTRA_EMAIL") ?: ""
            else -> ""
        }
        val role = when (activity) {
            is DashboardBaristaActivity -> "barista"
            is MainActivity -> "admin"
            else -> "admin"
        }

        val params = mutableMapOf<String, String>()
        params["role"] = role
        params["email"] = email

        if (dari.isNotEmpty() && sampai.isNotEmpty()) {
            params["dari"] = "$dari 00:00:00"
            params["sampai"] = "$sampai 23:59:59"
        }

        postKeServer(requireContext(), apiUrl, params) { response ->
            try {
                val list = ArrayList<String>()
                val data = JSONArray(response)
                var totalPendapatan = 0

                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.getString("ID_TRANSAKSI")
                    val jumlahItem = obj.getInt("JUMLAH_ITEM")
                    val datetime = obj.getString("WAKTU_TRANSAKSI")
                    val total = obj.getInt("TOTAL_HARGA")
                    val metode = obj.getString("METODE_BAYAR")

                    totalPendapatan += total

                    val text = """
                        ID Trx: $id
                        Waktu: $datetime
                        Item: $jumlahItem item
                        Metode: $metode
                        Total Bayar: Rp $total
                    """.trimIndent()

                    list.add(text)
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    list
                )

                listView.adapter = adapter
                tvTotal.text = "Rp $totalPendapatan"
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}