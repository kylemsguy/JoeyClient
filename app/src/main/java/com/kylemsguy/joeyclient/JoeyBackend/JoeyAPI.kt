package com.kylemsguy.joeyclient.JoeyBackend

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import java.lang.Math


class JoeyAPI(private val iface: UsbInterface, private val conn: UsbDeviceConnection){

    val readEndpoint: UsbEndpoint? = iface.getEndpoint(0x81)
    val writeEndpoint: UsbEndpoint? = iface.getEndpoint(0x01)

    val RAMTypes = intArrayOf(0,2048,8192,32768,(32768*4),(32768*2))

    /* PyUSB-like helpers */
    fun read(size: int){
        val result = ByteArray(size)
        conn.bulkTransfer(readEndpoint, result, result.length, 0)
        return result
    }

    fun write(cmd){
        conn.bulkTransfer(writeEndpoint, cmd, cmd.length, 0)
    }

    /* Ported functions from JoeyJoebags v3.36Beta */
    fun MBCDumpRAM(){
        val bankSize: int = 16384
        val header = readCartHeader()

        //conn.claimInterface(iface, true)
        write(byteArrayOf(0x0A,0x00,0x01,0x60,0x00,0x01)) // If MBC1, set mode to max RAM
        read(64)
        return dumpRAM(header, bankSize)
    }

    fun MBCBurnRAM(){
        val bankSize: int = 16384
        val header = readCartHeader()
        write(byteArrayOf(0x0A,0x00,0x01,0x60,0x00,0x01))
        read(64)

    }

    fun readCartHeader(): CartHeader{
        setBank(0, 0)
        ROMBankSwitch(0)

        val header = ArrayList<Byte>()

        write(byteArrayOf(0x10,0x00,0x00,0x01,0x00))
        dat = read(64)
        header.addAll(dat)

        write(byteArrayOf(0x10,0x00,0x00,0x01,0x40))
        dat = read(64)
        header.addAll(dat)

        write(byteArrayOf(0x10,0x00,0x00,0x01,0x80))
        dat = read(64)
        header.addAll(dat)

        val ROMSize = 32768 * Math.pow(2.toDouble(), header[0x48].toDouble()).toInt()
        val RAMSize = RAMTypes[header[0x49].toInt()]
        val ROMTitle = header.subList(0x34, 0x43).joinToString("")

        return CartHeader(ROMSize, RAMSize, ROMTitle, header.toByteArray())
    }

    fun setBank(blk: byte, sublk: byte): ByteArray {
        // TODO: Figure out what this actually does
        val sublk = sublk * 64
        val cmd = byteArrayOf(0x0A,0x00,0x03,0x70,0x00,sublk,0x70,0x01,0xE0,0x70,0x02,blk)
//    print (hex(blk),hex(sublk))

        write(writeEndpoint, cmd, cmd.length, 0) // Lock flash block(?)
        return read(64)
    }

    fun ROMBankSwitch(bankNumber: byte){
        // Convert 16bit bank number to 2 x 8bit numbers
        // Write to address defined under MBC settings to swap banks. This will change depending on certain cart types...
        val bhi = bankNumber shr 8
        val blo = bankNumber and 0xFF

        write(byteArrayOf(0x0A,0x00,0x01,0x30,0x00,bhi))
        read(64) // Do I even need to store this???
        write(byteArrayOf(0x0A,0x00,0x01,0x21,0x00,blo))
        dev.read(64)
    }

    fun RAMBankSwitch(bankNumber: byte){
        //print ("Bank:"+str(bankNumber))
        // Convert 16bit bank number to 2 x 8bit numbers
        // Write to address defined under MBC settings to swap banks. This will change depending on certain cart types...
        val blo = bankNumber and 0xFF
        write(byteArrayOf(0x0A,0x00,0x01,0x40,0x00,blo))
        read(64)
    }

    fun dumpRAM(header: CartHeader, bankSize: int){
        val ramBuffer = ArrayList<Byte>()
        val numBanks: int = header.RAMSize / 8192

        //for bankNumber in range(0,(int(RAMsize/8192))):
        for(bankNumber in 0..numBanks){
            var ramAddress = 0xA000
            RAMBankSwitch(bankNumber)
            val numPackets = 8192 / 64
            for(packetNumber in 0..numPackets){
                val addHi = RAMaddress shr 8
                val addLo = RAMaddress and 0xFF
                write(byteArrayOf(0x11, 0x00, 0x00, addHi, addLo))
                val result = read(64)
                ramAddress += 64
                ramBuffer.addAll(result)
            }
        }
        return ramBuffer.toByteArray()
    }

    fun burnRAM(ramData: byteArray, bankSize: int){
        // note: seems bankSize does nothing and all bank sizes are hardcoaded to 8192????
        val numBanks = RAMsize / 8192
        var rPos = 0

        for (bankNumber: 0..numBanks):
            var ramAddress = 0xA000
            RAMBankSwitch(bankNumber)
            for(packetNumber in 0..128):
                val AddHi = ramAddress shr 8
                val AddLo = ramAddress and 0xFF
                write(byteArrayOf(0x12,0x00,0x00,AddHi,AddLo))
                read(64)
                write(ramData.slice(rPos, rPos+64))
                read(64)
                ramAddress += 64
                Rpos += 64
    }
}