package com.kylemsguy.joeyclient

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.hardware.usb.UsbManager
import android.os.Environment
import android.view.View
import android.widget.Button
import com.kylemsguy.joeyclient.joeybackend.JoeyAPI
import java.io.File

class MainActivity : AppCompatActivity() {

    val manager = getSystemService(Context.USB_SERVICE) as UsbManager
    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    var readSRAMButton: Button? = null
    var writeSRAMButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        readSRAMButton = findViewById<Button>(R.id.btnDumpSRAM)
        writeSRAMButton = findViewById<Button>(R.id.btnBurnSRAM)

        readSRAMButton?.let { button ->
            button.setOnClickListener {
                readSRAMClicked(it)
            }
        }

        writeSRAMButton?.let { button ->
            button.setOnClickListener {
                writeSRAMClicked(it)
            }
        }
    }

    /* Checks if external storage is available for read and write */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /* Checks if external storage is available to at least read */
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    fun readSRAMClicked(view: View){
        device?.getInterface(0).also {intf ->
            manager.openDevice(device)?.apply{
                claimInterface(intf, true)
                if(intf is UsbInterface) {
                    val joeyAPI = JoeyAPI(intf, this)
                    val sram: ByteArray = joeyAPI.MBCDumpRAM()
                    val file = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), "JoeyClient")
                    file.writeBytes(sram)
                }
            }
        }

    }

    fun writeSRAMClicked(view: View){
    }
}
