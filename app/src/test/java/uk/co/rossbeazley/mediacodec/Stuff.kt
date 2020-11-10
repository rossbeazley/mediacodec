package uk.co.rossbeazley.mediacodec

fun getBytesFromHexString(hexString: String): ByteArray {
    val data = ByteArray(hexString.length / 2)
    for (i in data.indices) {
        val stringOffset = i * 2
        data[i] = ((Character.digit(hexString[stringOffset], 16) shl 4)
                + Character.digit(hexString[stringOffset + 1], 16)).toByte()
    }
    return data
}
