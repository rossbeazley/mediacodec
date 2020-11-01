package uk.co.rossbeazley.mediacodec

open class Box(val name: String, val payload: ByteArray, val size: Int = payload.size+8) {
    var boxes: MutableMap<String, Box> = mutableMapOf()
}