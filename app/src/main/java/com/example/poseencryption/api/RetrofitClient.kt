package com.example.poseencryption.api

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {

    var gson = GsonBuilder().setLenient().create()
    fun getRetrofit(baseUrl: String?): Retrofit? {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}