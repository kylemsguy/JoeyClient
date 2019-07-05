package com.kylemsguy.joeyclient.joeybackend

data class CartHeader (val ROMSize: Int, val RAMSize: Int, val ROMTitle: String, val raw: ByteArray){
    override fun toString(): String {
        return "ROM Title: " + ROMTitle + " ROM Size: " + ROMSize.toString() + " RAMSize: " + RAMSize.toString()
    }
}