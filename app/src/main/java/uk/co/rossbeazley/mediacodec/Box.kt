package uk.co.rossbeazley.mediacodec

open class Box(val name: String, val payload: ByteArray) {
    var boxes: MutableMap<String, Box> = mutableMapOf()
}