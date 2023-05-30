package com.example.poseencryption.api

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val serverIp = MutableLiveData<String>()
    val serverPort = MutableLiveData<String>()
}
class UrlManager : Fragment() {

    companion object {

        val BASE_URL = "http://183.99.13.124:1080/"

        val service: ApiService? =
            RetrofitClient().getRetrofit(BASE_URL)?.create(ApiService::class.java)
    }


    fun getService() = RetrofitClient().getRetrofit(BASE_URL)?.create(ApiService::class.java)
}