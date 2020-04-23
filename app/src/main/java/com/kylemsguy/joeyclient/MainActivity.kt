package com.kylemsguy.joeyclient

import android.Manifest
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import com.kylemsguy.joeyclient.joeybackend.JoeyAPI
import java.io.File

class MainActivity : AppCompatActivity() {

    var manager: UsbManager? = null
    var device: UsbDevice? = null

    var readSRAMButton: Button? = null
    var writeSRAMButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 23) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice

        if(device == null){
            val builder = AlertDialog.Builder(this)
            builder.apply{
                setMessage("Error detecting Joey Joebags. Please unplug and replug the device and try again.")
                setPositiveButton("Ok") { dialog, id ->
                    // User clicked OK button
                    finishAffinity()
                }
                setOnCancelListener {
                    // User clicked OK button
                    finishAffinity()
                }
                show()
            }
        }

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
        kotlin.io.println(device?.interfaceCount)
        kotlin.io.println(device?.manufacturerName)
        kotlin.io.println(device?.productId)
        kotlin.io.println(device?.productName)
        device?.getInterface(0).also {intf ->
            manager?.openDevice(device)?.apply{
                claimInterface(intf, true)
                if(intf is UsbInterface) {
                    kotlin.io.println(intf.endpointCount)
                    val joeyAPI = JoeyAPI(intf, this)
                    val sram: ByteArray = joeyAPI.MBCDumpRAM()
//                    val file = File(Environment.getExternalStoragePublicDirectory(
//                        Environment.DIRECTORY_DOCUMENTS), "JoeyClient")
                    val file = File(getExternalFilesDir(null), "sram.sav")
                    file.writeBytes(sram)
                }
            }
        }

    }

    fun writeSRAMClicked(view: View){
        val intent = Intent()
            .setType("*/")
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select a .sav file"), 111)
    }

    fun doWriteSRAM(fileUri: Uri){
        device?.getInterface(0).also {intf ->
            manager?.openDevice(device)?.apply{
                claimInterface(intf, true)
                if(intf is UsbInterface) {
                    kotlin.io.println(intf.endpointCount)
                    val joeyAPI = JoeyAPI(intf, this)
                    val file = File(fileUri.path)
                    val sram = file.readBytes()
                    joeyAPI.MBCBurnRAM(sram)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == RESULT_OK) {
            val selectedFile = data?.data
            if(selectedFile == null){
                val builder : AlertDialog.Builder? = this.let {
                    AlertDialog.Builder(it)
                }
                builder?.setMessage("SAV file load cancelled")
                builder?.create()
            } else {
                // TODO: add confirmation dialog
                doWriteSRAM(selectedFile)
            }
        }
    }
}
