package com.example.poseencryption.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.poseencryption.MainActivity
import com.example.poseencryption.MainViewModel
import com.example.poseencryption.databinding.FragmentHomeBinding

class HomeFragment: Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentHomeBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        binding.guideLayout.setOnClickListener {
            viewModel.selectMenu("guide")
        }
        binding.keyLayout.setOnClickListener {
            viewModel.selectMenu("key")
        }
        binding.captureLayout.setOnClickListener {
            viewModel.selectMenu("capture")
        }
        binding.resultLayout.setOnClickListener {
            viewModel.selectMenu("result")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}