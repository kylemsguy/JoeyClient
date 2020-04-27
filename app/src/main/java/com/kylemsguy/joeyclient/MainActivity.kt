package com.kylemsguy.joeyclient

import android.Manifest
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kylemsguy.joeyclient.joeybackend.JoeyAPI
import org.w3c.dom.Text
import java.io.File


class MainActivity : AppCompatActivity() {

    var manager: UsbManager? = null
    var device: UsbDevice? = null

    var textCartHeader: TextView? = null

    var btnGetCartInfo: Button? = null

    var readROMButton: Button? = null
    var writeROMButton: Button? = null

    var readSRAMButton: Button? = null
    var writeSRAMButton: Button? = null

    val debugDisablePathSelect = true

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

        textCartHeader = findViewById<TextView>(R.id.textCartHeader)

        btnGetCartInfo = findViewById<Button>(R.id.btnReadCartHeader)

        readROMButton = findViewById<Button>(R.id.btnDumpROM)
        writeROMButton = findViewById<Button>(R.id.btnBurnROM)

        readSRAMButton = findViewById<Button>(R.id.btnDumpSRAM)
        writeSRAMButton = findViewById<Button>(R.id.btnBurnSRAM)

        btnGetCartInfo?.let { button ->
            button.setOnClickListener {
                readCartHead(it)
            }
        }

        readROMButton?.let { button ->
            button.setOnClickListener {
                readROMClicked(it)
            }
        }
        writeROMButton?.let { button ->
            button.setOnClickListener {
                writeROMClicked(it)
            }
        }

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

    fun readCartHead(view: View){
        // TODO: Call readCartHead and display on screen
        device?.getInterface(0).also { intf ->
            manager?.openDevice(device)?.apply {
                claimInterface(intf, true)
                if (intf is UsbInterface) {
                    kotlin.io.println(intf.endpointCount)
                    val joeyAPI = JoeyAPI(intf, this)
                    val header = joeyAPI.readCartHeader()

                    textCartHeader?.text = header.toString()
                }
            }
        }
    }

    fun readROMClicked(view: View){
        kotlin.io.println(device?.interfaceCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            kotlin.io.println(device?.manufacturerName)
        }
        kotlin.io.println(device?.productId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            kotlin.io.println(device?.productName)
        }
        device?.getInterface(0).also {intf ->
            manager?.openDevice(device)?.apply{
                claimInterface(intf, true)
                if(intf is UsbInterface) {
                    kotlin.io.println(intf.endpointCount)
                    val joeyAPI = JoeyAPI(intf, this)
                    val rom: ByteArray = joeyAPI.MBCDumpROM()
//                    val file = File(Environment.getExternalStoragePublicDirectory(
//                        Environment.DIRECTORY_DOCUMENTS), "JoeyClient")
                    // TODO convert to file selector
                    // TODO make async
                    val file = File(getExternalFilesDir(null), "rom.gb")
                    file.writeBytes(rom)
                }
            }
        }

        val builder : AlertDialog.Builder? = this.let {
            AlertDialog.Builder(it)
        }
        builder?.setMessage("Read ROM Complete!")
        builder?.create()

//        Toast.makeText(
//            this, "Read ROM Complete!",
//            Toast.LENGTH_SHORT
//        ).show()

    }

    fun writeROMClicked(view: View){

    }

    fun readSRAMClicked(view: View){
        kotlin.io.println(device?.interfaceCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            kotlin.io.println(device?.manufacturerName)
        }
        kotlin.io.println(device?.productId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            kotlin.io.println(device?.productName)
        }
        device?.getInterface(0).also {intf ->
            manager?.openDevice(device)?.apply{
                claimInterface(intf, true)
                if(intf is UsbInterface) {
                    kotlin.io.println(intf.endpointCount)
                    val joeyAPI = JoeyAPI(intf, this)
                    val sram: ByteArray = joeyAPI.MBCDumpRAM()
//                    val file = File(Environment.getExternalStoragePublicDirectory(
//                        Environment.DIRECTORY_DOCUMENTS), "JoeyClient")
                    // TODO convert to file selector
                    val file = File(getExternalFilesDir(null), "sram.sav")
                    file.writeBytes(sram)
                }
            }
        }
    }

    fun writeSRAMClicked(view: View){
        Toast.makeText(
            this, "Write SRAM button Clicked",
            Toast.LENGTH_SHORT
        ).show()
        if(debugDisablePathSelect) {
            val file = File(getExternalFilesDir(null), "sram.sav")
            val fileURI = Uri.parse(file.toURI().toString())
            doWriteSRAM(fileURI)
            return
        }
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
                kotlin.io.println("SAV file loaded" + selectedFile.encodedPath)
                doWriteSRAM(selectedFile)
            }
        }
    }
}
