package com.example.kayuhan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class KasirActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var etSearch: EditText
    private lateinit var btnFloatingCart: Button

    private lateinit var btnCatAll: TextView
    private lateinit var btnCatCoffee: TextView
    private lateinit var btnCatNonCoffee: TextView

    private var emailLogin: String = ""
    private var selectedCategory: String = "Semua"

    val cartMap = mutableMapOf<String, CartItem>()

    data class CartItem(
        val idProduk: String,
        val namaProduk: String,
        val hargaJual: Int,
        var qty: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kasir)

        emailLogin = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        rvProducts      = findViewById(R.id.rvProducts)
        etSearch        = findViewById(R.id.etSearchMenu)
        btnFloatingCart = findViewById(R.id.btnFloatingCart)
        btnCatAll       = findViewById(R.id.btnCatAll)
        btnCatCoffee    = findViewById(R.id.btnCatCoffee)
        btnCatNonCoffee = findViewById(R.id.btnCatNonCoffee)

        // Grid 2 kolom untuk card produk
        rvProducts.layoutManager = GridLayoutManager(this, 2)

        btnCatAll.setOnClickListener       { setCategory("Semua") }
        btnCatCoffee.setOnClickListener    { setCategory("Coffee") }
        btnCatNonCoffee.setOnClickListener { setCategory("Non-Coffee") }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { loadProducts() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFloatingCart.setOnClickListener { showCartBottomSheet() }

        loadProducts()
    }

    private fun setCategory(category: String) {
        selectedCategory = category

        // reset semua chip
        listOf(btnCatAll, btnCatCoffee, btnCatNonCoffee).forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
            it.setTextColor(getColor(android.R.color.black))
        }

        val selectedChip = when (category) {
            "Coffee"     -> btnCatCoffee
            "Non-Coffee" -> btnCatNonCoffee
            else         -> btnCatAll
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected)
        selectedChip.setTextColor(getColor(android.R.color.white))

        loadProducts()
    }

    private fun loadProducts() {
        val keyword = etSearch.text.toString().trim()

        val url = ApiConfig.GET_MENU

        val queue = com.android.volley.toolbox.Volley.newRequestQueue(this)
        val stringRequest = com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = org.json.JSONArray(response)
                    val productList = mutableListOf<ProductAdapter.Product>()

                    // 2. Looping langsung dari hasil JSON MySQL
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)

                        val id = item.getString("id_produk")
                        val nama = item.getString("nama_produk")
                        val kat = item.getString("kategori") // Membaca "Coffee" atau "Non-Coffee" dari MySQL
                        val harga = item.getInt("harga_jual")

                        // Filter Kategori di sisi Android (Client-side filtering)
                        if (selectedCategory != "Semua" && kat != selectedCategory) {
                            continue
                        }

                        // Filter Search Keyword di sisi Android
                        if (keyword.isNotEmpty() && !nama.contains(keyword, ignoreCase = true)) {
                            continue
                        }

                        val qty = cartMap[id]?.qty ?: 0
                        productList.add(ProductAdapter.Product(id, nama, kat, harga, qty))
                    }

                    // 3. Set ke Adapter
                    productAdapter = ProductAdapter(
                        products = productList,
                        onQtyChanged = { product, newQty ->
                            if (newQty > 0) {
                                cartMap[product.id] = CartItem(product.id, product.nama, product.harga, newQty)
                            } else {
                                cartMap.remove(product.id)
                            }
                            updateCartButton()
                        }
                    )
                    rvProducts.adapter = productAdapter

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal mengurai data menu MySQL", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Gagal konek ke MySQL server", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(stringRequest)
    }

    private fun updateCartButton() {
        val totalItem = cartMap.values.sumOf { it.qty }
        btnFloatingCart.text = "Lihat Keranjang ($totalItem Item)"
    }

    private fun showCartBottomSheet() {
        if (cartMap.isEmpty()) {
            Toast.makeText(this, "Keranjang masih kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view   = layoutInflater.inflate(R.layout.bottom_sheet_cart, null)
        dialog.setContentView(view)

        val rvCart      = view.findViewById<RecyclerView>(R.id.rvCartItems)
        val tvTotal     = view.findViewById<TextView>(R.id.tvTotalPrice)
        val btnBayar    = view.findViewById<Button>(R.id.btnBayar)
        val optTunai    = view.findViewById<LinearLayout>(R.id.optTunai)
        val optQris     = view.findViewById<LinearLayout>(R.id.optQris)
        val tvClearCart = view.findViewById<TextView>(R.id.tvClearCart)

        var metodePembayaran = "Tunai"

        fun hitungTotal() = cartMap.values.sumOf { it.hargaJual * it.qty }
        fun formatRp(amount: Int) = "Rp ${String.format("%,d", amount).replace(',', '.')}"

        tvTotal.text = formatRp(hitungTotal())

        rvCart.layoutManager = LinearLayoutManager(this)
        val cartAdapter = CartAdapter(
            items = cartMap.values.toMutableList(),
            onQtyChanged = { item, newQty ->
                if (newQty > 0) cartMap[item.idProduk]?.qty = newQty
                else cartMap.remove(item.idProduk)
                tvTotal.text = formatRp(hitungTotal())
                updateCartButton()
                if (cartMap.isEmpty()) dialog.dismiss()
            }
        )
        rvCart.adapter = cartAdapter

        fun selectMetode(metode: String) {
            metodePembayaran = metode
            if (metode == "Tunai") {
                optTunai.setBackgroundResource(R.drawable.bg_payment_selected)
                optQris.setBackgroundResource(R.drawable.bg_payment_unselected)
            } else {
                optTunai.setBackgroundResource(R.drawable.bg_payment_unselected)
                optQris.setBackgroundResource(R.drawable.bg_payment_selected)
            }
        }

        optTunai.setOnClickListener { selectMetode("Tunai") }
        optQris.setOnClickListener  { selectMetode("QRIS") }

        tvClearCart.setOnClickListener {
            cartMap.clear()
            updateCartButton()
            dialog.dismiss()
            loadProducts()
        }

        btnBayar.setOnClickListener {
            simpanTransaksi(metodePembayaran)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun simpanTransaksi(metode: String) {
        if (cartMap.isEmpty()) return

        // ID Transaksi maksimal 10 karakter untuk tipe VARCHAR(10) di MySQL
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val randomString = (1..7).map { chars.random() }.joinToString("")
        val idTransaksi = "TRX$randomString"
        val totalBayar  = cartMap.values.sumOf { it.hargaJual * it.qty }

        // Buat JSON Array untuk cart_items sesuai dengan format php insert_transaksi
        val jsonArray = org.json.JSONArray()
        for (item in cartMap.values) {
            val jsonItem = org.json.JSONObject()
            jsonItem.put("id_produk", item.idProduk)
            jsonItem.put("jml_item", item.qty)
            jsonArray.put(jsonItem)
        }
        val cartItemsJson = jsonArray.toString()

        val url = ApiConfig.INSERT_TRANSAKSI
        val queue = com.android.volley.toolbox.Volley.newRequestQueue(this)
        
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.POST, url,
            { response ->
                try {
                    val jsonResponse = org.json.JSONObject(response)
                    val status = jsonResponse.getString("status")
                    val message = jsonResponse.getString("message")
                    if (status == "success") {
                        cartMap.clear()
                        updateCartButton()
                        loadProducts()
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal: $message", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal memproses respon server", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Gagal konek ke server: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id_transaksi"] = idTransaksi
                params["email"] = emailLogin
                params["total_bayar"] = totalBayar.toString()
                params["metode_pembayaran"] = metode
                params["cart_items"] = cartItemsJson
                return params
            }
        }
        queue.add(stringRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}