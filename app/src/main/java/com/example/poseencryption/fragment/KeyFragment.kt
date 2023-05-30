package com.example.poseencryption.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private val path: String = Constants.dataDir!!

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

        edittextIp.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                viewModel.serverIp.value = s.toString()
            }
        })
        edittextPort.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                viewModel.serverPort.value = s.toString()
            }
        })

        createKeyLy.setOnClickListener {
            createKeyPB.visibility = View.VISIBLE
            checkKeyTv.text = "Connecting to ${viewModel.serverIp.value.toString()} : ${viewModel.serverPort.value.toString()} \n"
            CoroutineScope(Dispatchers.Main).launch {
                createKeyPB.visibility = View.VISIBLE
                delay(100L)
//                val encFile =
                if (File(path, "EncKey.txt").exists()) {
                    checkKeyTv.append("Encrypt Key is Generated\n")
                }
//                mulFile = File(path, "MulKey.txt")
                if (File(path, "MulKey.txt").exists()) {
                    checkKeyTv.append("Multiple Key is Generated\n")
                }
//                conjFile = File(path, "ConjKey.txt")
                if (File(path, "ConjKey.txt").exists()) {
                    checkKeyTv.append("Conjunction Key is Generated\n")
                }
//                rotFile = File(path, "RotKey_1.txt")
                if (File(path, "RotKey_1.txt").exists()) {
                    checkKeyTv.append("Rotation Key is Generated\n")
                }
                if (MainActivity().createNewKeys(path)?.isNotEmpty() == true) {
                    createKeyPB.visibility = View.GONE
                }
            }
        }
        uploadKeyLy.setOnClickListener {
            uploadKeyPB.visibility = View.VISIBLE
            val encFile = File(path, "EncKey.txt")
            if (!encFile.exists()) {
                Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            UrlManager.instance.postKeyFile("enc_key", "EncKey.txt", encFile)

            val mulFile = File(path, "MulKey.txt")
            if (!mulFile.exists()) {
                Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            UrlManager.instance.postKeyFile("mul_key", "MulKey.txt", mulFile)

            val rotFile = File(path, "RotKey.txt")
            if (!mulFile.exists()) {
                Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            UrlManager.instance.postKeyFile("rot_key", "RotKey.txt", rotFile)

            val conjFile = File(path, "ConjKey.txt")
            if (!conjFile.exists()) {
                Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            UrlManager.instance.postKeyFile("conj_key", "ConjKey.txt", conjFile)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}