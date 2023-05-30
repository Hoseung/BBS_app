package com.example.poseencryption

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.poseencryption.databinding.ActivityMainBinding
import com.example.poseencryption.fragment.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    var CatIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setPaths()
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.menuItem.observe(this) {
            when (it) {
                "home" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.home
                }
                "result" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.result
                }
                "capture" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.capture
                }
                "key" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.key
                }
                "guide" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.guide
                }
            }
        }
        initView()

    }

    private fun initView() = with(binding) {
        replaceFragment(HomeFragment())
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.result -> {
                    replaceFragment(ResultFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.capture -> {
                    replaceFragment(CaptureFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.key -> {
                    replaceFragment(KeyFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.guide -> {
                    replaceFragment(GuideFragment())
                    return@setOnItemSelectedListener true
                }
                else -> return@setOnItemSelectedListener true
            }
        }
    }

    fun select() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.key
    }

    fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.menu_frame_layout, fragment)
        fragmentTransaction.commit()
    }

    companion object {
        init {
            System.loadLibrary("fhe-lib")
        }
    }

    external fun DoEncryptionCpp(
        logN: Int,
        inputarr: FloatArray?,
        Num: Int,
        fname: String?,
        path: String?
    ): String?

    external fun DoDecryptionCpp(logN: Int, Num: Int, path: String?): Int

    external fun createNewKeys(path: String?): String?

    external fun Test(path: String?)

    private fun setPaths() {
        Constants.dataDir = applicationContext.filesDir.toString() + "/"
    }

}