package com.kylemsguy.joeyclient

import android.hardware.usb.UsbDevice
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.hardware.usb.UsbManager

class MainActivity : AppCompatActivity() {

    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}
