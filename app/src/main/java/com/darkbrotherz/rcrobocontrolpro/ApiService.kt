package com.darkbrotherz.rcrobocontrolpro

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("api_get_products.php")
    fun getProducts(): Call<ApiResponse>
}
