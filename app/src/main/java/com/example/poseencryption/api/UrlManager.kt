package com.example.poseencryption.api

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*

class SharedViewModel : ViewModel() {
    val serverIp = MutableLiveData<String>()
    val serverPort = MutableLiveData<String>()
}
class UrlManager : Fragment() {
    companion object {
        val instance = UrlManager()
    }
    private val viewModel: SharedViewModel by activityViewModels()
    // Companion object is a singleton with a scope limited to the following {}
    private var service: ApiService? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        service = RetrofitClient().getRetrofit(getServerUrl())?.create(ApiService::class.java)
    }
    private fun getServerUrl(): String {
        return "http://${viewModel.serverIp.value}:${viewModel.serverPort.value}/"
    }

    fun postKeyFile(fileName: String, fileType: String, encFile: File) {
        val requestFile = RequestBody.create(MediaType.parse("text/*"), encFile)
        val encBody = MultipartBody.Part.createFormData("file", fileType, requestFile)

        service?.postKeyFile(fileName, encBody)?.enqueue(object :
            Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        println(response.code())
                    }
                } else {
                    println(response)
                }
                println(response.body().toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    fun postCtxt(fileName: String, action: Int, body: MultipartBody.Part){
        service?.postEncryptionFile(fileName, action, body)
            ?.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    println(response.body())
                    println(response.code())
                    Toast.makeText(requireContext(), "성공적으로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    t.printStackTrace()
                }
            })

    }
    fun downloadCtxt(){
        service?.downloadEncryption()?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        writeResponseBodyToDisk(response.body()!!)
                        Toast.makeText(requireContext(), "다운로드 성공!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    println(response)
                }
                println(response.body().toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }

        })
    }

    private fun writeResponseBodyToDisk(body: ResponseBody): Boolean {
        return try {
            // todo change the file location/name according to your needs
            val futureStudioIconFile =
                File(
                    requireContext().getExternalFilesDir(null)
                        .toString() + File.separator + "Download Data @#$%^"
                )
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(futureStudioIconFile)
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                    Log.d("TAG", "file download: $fileSizeDownloaded of $fileSize")
                }
                outputStream.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            false
        }
    }
}