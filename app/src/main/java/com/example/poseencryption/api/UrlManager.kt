package com.example.poseencryption.api

class UrlManager {

    companion object {

        val BASE_URL = "http://183.99.13.124:1080/"

        val service: ApiService? =
            RetrofitClient().getRetrofit(BASE_URL)?.create(ApiService::class.java)
    }


    fun getService() = RetrofitClient().getRetrofit(BASE_URL)?.create(ApiService::class.java)
}