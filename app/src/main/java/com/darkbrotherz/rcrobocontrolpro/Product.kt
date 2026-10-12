package com.darkbrotherz.rcrobocontrolpro

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val price: String,
    val image_url: String,
    val category: String? = null
)

data class ApiResponse(
    val status: String,
    val data: List<Product>
)
