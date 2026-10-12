package com.darkbrotherz.rcrobocontrolpro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ShopFragment : Fragment() {

    private lateinit var storeRecyclerView: RecyclerView
    private lateinit var storeAdapter: StoreAdapter
    private val allProductsList = mutableListOf<Product>()
    private val filteredList = mutableListOf<Product>()
    private val cartList = mutableListOf<Product>()

    private var currentCategory = ""
    private var currentQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_shop, container, false)

        // Toolbar Buttons
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnCart = view.findViewById<ImageView>(R.id.btnCart)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val btnAdminPanel = view.findViewById<FloatingActionButton>(R.id.btnAdminPanel)

        btnBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        btnCart?.setOnClickListener {
            showCartDialog()
        }
        btnAdminPanel?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discount-purposely-rejoin.ngrok-free.dev/ceylon_shop_db/index.php"))
            startActivity(intent)
        }

        // Filter Buttons
        val btnFilterAll = view.findViewById<TextView>(R.id.btnFilterAll)
        val btnFilterBoards = view.findViewById<TextView>(R.id.btnFilterBoards)
        val btnFilterSensors = view.findViewById<TextView>(R.id.btnFilterSensors)

        btnFilterAll?.setOnClickListener { currentCategory = ""; updateFilteredList() }
        btnFilterBoards?.setOnClickListener { currentCategory = "Boards"; updateFilteredList() }
        btnFilterSensors?.setOnClickListener { currentCategory = "Sensors"; updateFilteredList() }

        // Search Setup
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                updateFilteredList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // RecyclerView Setup
        storeRecyclerView = view.findViewById(R.id.storeRecyclerView)
        storeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initial Data fetch from API
        fetchProducts()

        // Adapter Setup
        storeAdapter = StoreAdapter(filteredList, { product ->
            val intent = Intent(requireContext(), ProductDetailsActivity::class.java)
            intent.putExtra("NAME", product.title)
            intent.putExtra("PRICE", product.price)
            intent.putExtra("IMAGE_URL", product.image_url)
            intent.putExtra("DESCRIPTION", product.description)
            startActivity(intent)
        }, { product ->
            cartList.add(product)
            Toast.makeText(requireContext(), "${product.title} added to cart", Toast.LENGTH_SHORT).show()
        })
        storeRecyclerView.adapter = storeAdapter

        return view
    }

    private fun fetchProducts() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://discount-purposely-rejoin.ngrok-free.dev/ceylon_shop_db/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.getProducts().enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.status == "success") {
                        val productList = apiResponse.data
                        allProductsList.clear()
                        allProductsList.addAll(productList)
                        updateFilteredList()
                        Log.d("API_SUCCESS", "Items loaded: ${allProductsList.size}")
                    } else {
                        Log.e("API_ERROR", "Server status: ${apiResponse?.status}")
                        Toast.makeText(requireContext(), "Server error: ${apiResponse?.status}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("API_ERROR", "Response Code: ${response.code()}")
                    Toast.makeText(requireContext(), "Error code: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Network error: ${t.message}")
                Toast.makeText(requireContext(), "Network Error. check your server!", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showCartDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val cartView = layoutInflater.inflate(R.layout.bottom_sheet_cart, null)
        val rvCart = cartView.findViewById<RecyclerView>(R.id.rvCartItems)
        val txtEmpty = cartView.findViewById<TextView>(R.id.txtCartEmpty)

        if (cartList.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            rvCart.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            rvCart.visibility = View.VISIBLE
            rvCart.layoutManager = LinearLayoutManager(requireContext())
            rvCart.adapter = CartAdapter(cartList)
        }

        dialog.setContentView(cartView)
        dialog.show()
    }

    private fun updateFilteredList() {
        filteredList.clear()
        val query = currentQuery.trim().lowercase()
        
        allProductsList.forEach { product ->
            val matchesCategory = currentCategory.isEmpty() || 
                                (product.category != null && product.category.equals(currentCategory, ignoreCase = true))
            
            val matchesSearch = query.isEmpty() || 
                               product.title.lowercase().contains(query) || 
                               product.description.lowercase().contains(query)
            
            if (matchesCategory && matchesSearch) {
                filteredList.add(product)
            }
        }

        if (::storeAdapter.isInitialized) {
            storeAdapter.notifyDataSetChanged()
        }
        
        if (filteredList.isEmpty() && allProductsList.isNotEmpty()) {
            Log.d("FILTER", "No items matched filter: cat=$currentCategory, query=$query")
        }
    }
}
