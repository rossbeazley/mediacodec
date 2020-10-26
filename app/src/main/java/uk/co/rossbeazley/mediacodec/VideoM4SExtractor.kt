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


    fun sample(i: Int): ByteArray {
        //read the data offset from the trun
        val box : TrunBox = boxes["moof"]!!.boxes["traf"]!!.boxes["trun"]!! as TrunBox

        val mdat = boxes["mdat"] !! as MdatBox

        val dataOffset = box.dataOffset
        val mdatHeader = mdat.size - mdat.payload.size
        val offsetInMdatFromStart = dataOffset - (boxes["moof"]!!.size)
        val offsetInMdatPayload = offsetInMdatFromStart - mdatHeader

        var sampleOffset = offsetInMdatPayload + (allOtherSampleSizesBefore(i))

        val sample = mdat.payload.sliceArray(sampleOffset until box.sampleRecords[i].sampleSize + sampleOffset)
        return sample
    }

    private fun allOtherSampleSizesBefore(i: Int): Int {
        var sum = 0

        if (i == 0) return sum

        val box : TrunBox = boxes["moof"]!!.boxes["traf"]!!.boxes["trun"]!! as TrunBox
        for(idx in (i-1) downTo 0) {
            sum += box.sampleRecords[idx].sampleSize
        }
        return sum
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
        "trun" -> TrunBox.from(bytes)
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

fun readBytesAsLong(numberOfBytes: Int, bytes: ByteArray, byteOffset: Int = 0) : Long {
    return readBitsAsLong(numberOfBytes*8, bytes, byteOffset)
}

fun readBitsAsLong(number: Int, bytes: ByteArray, byteOffset: Int = 0) : Long {
    var startBits = number
    var idx = 0
    var result = 0L
    while ( startBits > 0) {
        startBits -= 8
        result = result.or((0xffL and (bytes[byteOffset + idx].toLong())).shl(startBits))
        idx++
    }
    return result
}
