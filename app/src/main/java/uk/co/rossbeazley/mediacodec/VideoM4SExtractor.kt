package uk.co.rossbeazley.mediacodec

class VideoM4SExtractor(val bytes: ByteArray) {

    val boxes: MutableMap<String, Box> = mutableMapOf()

    var frameCount: Int = 0

    init {


        //moof, it has boxes in it
        // unsigned int(32) size;
        // unsigned int(32) type = boxtype;

        var byteOffset = 0
        val moofSize = boxSize(bytes, byteOffset)
        //slice off Moof worth of bytes
        val sizeHeader = 4
        val moofBytes = bytes.sliceArray(byteOffset + sizeHeader until moofSize.toInt())
        val moofBox = parseBox(moofBytes)
        boxes.addBox(moofBox)


        val remainingBytes = bytes.sliceArray((moofSize).toInt() until bytes.size)
        val mdatSize = boxSize(remainingBytes, 0)
        //slice off Moof worth of bytes
        val mdatBytes = remainingBytes.sliceArray(0 + sizeHeader until mdatSize.toInt())
        val mdatBox = parseBox(mdatBytes)
        boxes.addBox(mdatBox)
    }

}


fun MutableMap<String, Box>.addBox(box : Box) {
    this += box.name to box
}

fun parseBox(bytes: ByteArray): Box {
    val boxName = boxName(bytes, 0)
    return when (boxName) {
        "moof" -> MoofBox.from(bytes)
        "mdat" -> MdatBox.from(bytes)
        "mfhd" -> MfhdBox.from(bytes)
        "traf" -> TrafBox.from(bytes)
        "tfhd" -> TfhdBox.from(bytes)
        "tfdt" -> TfdtBox.from(bytes)
        else -> Box(boxName, bytes.sliceArray(4 until bytes.size))
    }
}

fun boxName(bytes: ByteArray, byteOffset: Int): String {
    val charArray = CharArray(4)
    charArray[0] = bytes[byteOffset + 0].toChar()
    charArray[1] = bytes[byteOffset + 1].toChar()
    charArray[2] = bytes[byteOffset + 2].toChar()
    charArray[3] = bytes[byteOffset + 3].toChar()

    val name = String(charArray)
    return name
}

fun boxSize(bytes: ByteArray, byteOffset: Int): Long {
    return readBitsAsLong(32, bytes)
}

fun readBitsAsLong(number: Int, bytes: ByteArray) : Long {
    var startBits = number
    var idx = 0
    var result = 0L
    while ( startBits > 0) {
        startBits -= 8
        result = result.or((0xffL and (bytes[idx].toLong())).shl(startBits))
        idx++
    }
    return result
}
