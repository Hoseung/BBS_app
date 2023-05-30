package com.example.poseencryption.api

import com.example.poseencryption.API
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface ApiService {

    @GET("result")
    fun downloadEncryption(): Call<ResponseBody>

    @Multipart
    @POST(API.UPLOAD_CTXT_URL)
    fun postEncryptionFile(
        @Header("dtype") ctxt: String,
        @Header("action") action: Int,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @Multipart
    @POST(API.UPLOAD_KEY_URL)
    fun postKeyFile(
        @Header("dtype") dtype: String,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

}