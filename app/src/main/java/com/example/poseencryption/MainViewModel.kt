package com.example.poseencryption

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private var _menuItem = MutableLiveData("home")
    val menuItem: LiveData<String> = _menuItem

    fun selectMenu(item: String) {
        _menuItem.value = item
    }


}