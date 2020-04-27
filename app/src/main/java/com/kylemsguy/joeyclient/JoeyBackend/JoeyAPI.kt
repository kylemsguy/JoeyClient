package com.kylemsguy.joeyclient.joeybackend

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import java.lang.Math


class JoeyAPI(private val iface: UsbInterface, private val conn: UsbDeviceConnection){

//    val readEndpoint: UsbEndpoint? = iface.getEndpoint(0x81)
//    val writeEndpoint: UsbEndpoint? = iface.getEndpoint(0x01)
    val readEndpoint: UsbEndpoint? = iface.getEndpoint(0)
    val writeEndpoint: UsbEndpoint? = iface.getEndpoint(1)

    val RAMTypes = intArrayOf(0,2048,8192,32768,(32768*4),(32768*2))

    /* PyUSB-like helpers */
    fun read(size: Int): ByteArray {
        val result = ByteArray(size)
        conn.bulkTransfer(readEndpoint, result, result.size, 10)
        return result
    }

    fun write(cmd: ByteArray){
        conn.bulkTransfer(writeEndpoint, cmd, cmd.size, 10)
    }

    /* Ported functions from JoeyJoebags v3.36Beta */
    fun MBCDumpROM(): ByteArray{
        val bankSize = 16384
        val header = readCartHeader()
        return dumpROM(header, bankSize)
    }

    fun MBCDumpRAM(): ByteArray{
        val bankSize = 16384
        val header = readCartHeader()

        //conn.claimInterface(iface, true)
        write(byteArrayOf(0x0A,0x00,0x01,0x60,0x00,0x01)) // If MBC1, set mode to max RAM
        read(64)
        return dumpRAM(header, bankSize)
    }

    fun MBCBurnRAM(sram: ByteArray) {
        val bankSize = 16384
        val header = readCartHeader()
        write(byteArrayOf(0x0A,0x00,0x01,0x60,0x00,0x01))
        read(64)
        burnRAM(header, sram, bankSize)
    }

    fun readCartHeader(): CartHeader{
        setBank(0, 0)
        ROMBankSwitch(0)

        val header = ArrayList<Byte>()

        write(byteArrayOf(0x10,0x00,0x00,0x01,0x00))
        read(64).let { dat ->
            header.addAll(dat.toList())
        }

        write(byteArrayOf(0x10,0x00,0x00,0x01,0x40))
        read(64).let { dat ->
            header.addAll(dat.toList())
        }

        write(byteArrayOf(0x10, 0x00, 0x00, 0x01, 0x80.toByte()))
        val dat = read(64).let { dat ->
            header.addAll(dat.toList())
        }

        val ROMSize = 32768 * Math.pow(2.toDouble(), header[0x48].toDouble()).toInt()
        val RAMSize = RAMTypes[header[0x49].toInt()]
        val ROMTitle = header.subList(0x34, 0x43).joinToString("")

        return CartHeader(ROMSize, RAMSize, ROMTitle, header.toByteArray())
    }

    fun setBank(blk: Byte, sublk: Byte): ByteArray {
        // TODO: Figure out what this actually does
        // Lock cart before writing
        val sublk = sublk * 64
        val cmd = byteArrayOf(0x0A, 0x00, 0x03, 0x70, 0x00, sublk.toByte(), 0x70, 0x01, 0xE0.toByte(), 0x70, 0x02, blk)
//    print (hex(blk),hex(sublk))
        write(cmd) // Lock flash block(?)
        return read(64)
    }

    fun ROMBankSwitch(bankNumber: Int){
        // Convert 16bit bank number to 2 x 8bit numbers
        // Write to address defined under MBC settings to swap banks. This will change depending on certain cart types...
        val bhi = (bankNumber shr 8).toByte()
        val blo = (bankNumber and 0xFF).toByte()

        write(byteArrayOf(0x0A,0x00,0x01,0x30,0x00,bhi))
        read(64) // Do I even need to store this???
        write(byteArrayOf(0x0A,0x00,0x01,0x21,0x00,blo))
        read(64)
    }

    fun dumpROM(header: CartHeader, bankSize: Int): ByteArray{
        // main_dumpROM
        val romBuffer = ArrayList<Byte>()
        val numBanks: Int = header.ROMSize / bankSize
        var romAddress: Int

        kotlin.io.println("ROM size " + header.ROMSize.toString())
        kotlin.io.println("number of banks " + numBanks.toString())

        for(bankNumber in 0 until numBanks){
            kotlin.io.println("bank number " + bankNumber.toString())
            if (bankNumber == 0){
                romAddress = 0 // get bank 0 from address 0, not setbank(0) and get from high bank...
            } else {
                romAddress = bankSize
            }
            ROMBankSwitch(bankNumber) // switch to new bank.
            val numPackets = bankSize / 64
            for(packetNumber in 0 until numPackets){
                kotlin.io.println("packet number " + packetNumber.toString())
                val addHi = (romAddress shr 8).toByte()
                val addLo = (romAddress and 0xFF).toByte()
                write(byteArrayOf(0x10, 0x00, 0x00, addHi, addLo))
                val result = read(64)
                romAddress += 64
                romBuffer.addAll(result.toList())
            }
        }
        kotlin.io.println("Number of bytes read: " + romBuffer.size.toString())
        return romBuffer.toByteArray()
    }

    fun RAMBankSwitch(bankNumber: Int){
        //print ("Bank:"+str(bankNumber))
        // Convert 16bit bank number to 2 x 8bit numbers
        // Write to address defined under MBC settings to swap banks. This will change depending on certain cart types...
        val blo = (bankNumber and 0xFF).toByte()
        write(byteArrayOf(0x0A, 0x00, 0x01, 0x40, 0x00, blo))
        read(64)
    }

    fun dumpRAM(header: CartHeader, bankSize: Int): ByteArray{
        // I'm not sure what bankSize if for if it's always 8192 (see numBanks)
        val ramBuffer = ArrayList<Byte>()
        val numBanks: Int = header.RAMSize / 8192

        kotlin.io.println("RAM size " + header.RAMSize.toString())
        kotlin.io.println("number of banks " + numBanks.toString())

        //for bankNumber in range(0,(int(RAMsize/8192))):
        for(bankNumber in 0 until numBanks){
            kotlin.io.println("bank number " + bankNumber.toString())
            var ramAddress = 0xA000
            RAMBankSwitch(bankNumber)
            val numPackets = 8192 / 64
            for(packetNumber in 0 until numPackets){
                kotlin.io.println("packet number " + packetNumber.toString())
                val addHi = (ramAddress shr 8).toByte()
                val addLo = (ramAddress and 0xFF).toByte()
                write(byteArrayOf(0x11, 0x00, 0x00, addHi, addLo))
                val result = read(64)
                ramAddress += 64
                ramBuffer.addAll(result.toList())
            }
        }
        kotlin.io.println("Number of bytes read: " + ramBuffer.size.toString())
        return ramBuffer.toByteArray()
    }

    fun burnRAM(header: CartHeader, ramData: ByteArray, bankSize: Int){
        // note: seems bankSize does nothing and all bank sizes are hardcoaded to 8192????
        val numBanks: Int = header.RAMSize / 8192
        var rPos = 0

        for (bankNumber in 0 until numBanks) {
            var ramAddress = 0xA000
            RAMBankSwitch(bankNumber)
            for (packetNumber in 0 until 128) {
                val AddHi = (ramAddress shr 8).toByte()
                val AddLo = (ramAddress and 0xFF).toByte()
                write(byteArrayOf(0x12, 0x00, 0x00, AddHi, AddLo))
                read(64)
                write(ramData.sliceArray(rPos until rPos + 64))
                read(64)
                ramAddress += 64
                rPos += 64
            }
        }
    }
}
