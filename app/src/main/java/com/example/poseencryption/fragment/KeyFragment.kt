package com.example.poseencryption.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.poseencryption.Constants
import com.example.poseencryption.MainActivity
import com.example.poseencryption.api.SharedViewModel
import com.example.poseencryption.api.UrlManager
import com.example.poseencryption.databinding.FragmentKeyBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import androidx.fragment.app.activityViewModels
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class KeyFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var binding: FragmentKeyBinding

    private var encFile: File? = null
    private var mulFile: File? = null
    private var conjFile: File? = null
    private var rotFile: File? = null

    private val path: String = Constants.dataDir

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentKeyBinding.inflate(layoutInflater)

        initView()

        return binding.root
    }

    private fun initView() = with(binding) {
        viewModel.serverIp.value = edittextIp.text.toString()
        viewModel.serverPort.value = edittextIp.text.toString()

        createKeyLy.setOnClickListener {
            createKeyPB.visibility = View.VISIBLE
            checkKeyTv.text = "Connecting to ${viewModel.serverIp.value.toString()} : ${viewModel.serverPort.value.toString()} \n"
            CoroutineScope(Dispatchers.Main).launch {
                createKeyPB.visibility = View.VISIBLE
                delay(100L)
                encFile = File(path, "EncKey.txt")
                if (encFile!!.exists()) {
                    checkKeyTv.append("Encrypt Key is Generated\n")
                }
                mulFile = File(path, "MulKey.txt")
                if (mulFile!!.exists()) {
                    checkKeyTv.append("Multiple Key is Generated\n")
                }
                conjFile = File(path, "ConjKey.txt")
                if (conjFile!!.exists()) {
                    checkKeyTv.append("Conjunction Key is Generated\n")
                }
                rotFile = File(path, "RotKey_1.txt")
                if (rotFile!!.exists()) {
                    checkKeyTv.append("Rotation Key is Generated\n")
                }
                if (MainActivity().createNewKeys(path)?.isNotEmpty() == true) {
                    createKeyPB.visibility = View.GONE
                }
            }
        }
        uploadKeyLy.setOnClickListener {
            uploadKeyPB.visibility = View.VISIBLE
            encFile = File(path, "EncKey.txt")
            if (!encFile!!.exists()) {
                Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mulFile = File(path, "MulKey.txt")
            if (!mulFile!!.exists()) {
                Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestFile = RequestBody.create(MediaType.parse("text/*"), encFile)
            val encBody = MultipartBody.Part.createFormData("file", "EncKey.txt", requestFile)
            UrlManager.service?.postKeyFile("enc_key", encBody)?.enqueue(object :
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

            val mulRequestFile = RequestBody.create(MediaType.parse("text/plain"), mulFile)
            val body = MultipartBody.Part.createFormData("file", "MulKey.txt", mulRequestFile)
            UrlManager.service?.postKeyFile("mul_key", body)
                ?.enqueue(object : Callback<ResponseBody?> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        println(response.body())
                        println(response.code())
                        Toast.makeText(requireContext(), "키 전송 성공!", Toast.LENGTH_SHORT).show()
                        uploadKeyPB.visibility = View.GONE
                    }

                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        t.printStackTrace()
                    }
                })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}