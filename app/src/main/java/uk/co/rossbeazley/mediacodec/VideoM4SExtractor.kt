package uk.co.rossbeazley.mediacodec

import java.nio.ByteBuffer

class VideoM4SExtractor(val bytes: ByteArray) {


    val boxes: MutableMap<String, Box> = mutableMapOf()

    var frameCount: Int = 96

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
        val box : BoxOfBoxes = boxes["moof"] as BoxOfBoxes
        val trun : TrunBox = (box.boxes["traf"] as BoxOfBoxes).boxes["trun"]!! as TrunBox
        val mdat = boxes["mdat"] !! as MdatBox

        val dataOffset = trun.dataOffset
        val mdatHeader = mdat.size - mdat.payload.size
        val offsetInMdatFromStart = dataOffset - (box.size)
        val offsetInMdatPayload = offsetInMdatFromStart - mdatHeader

        var sampleOffset = offsetInMdatPayload + (allOtherSampleSizesBefore(i))

        val sample = mdat.payload.sliceArray(sampleOffset until trun.sampleRecords[i].sampleSize + sampleOffset)

        return sample //avccToAnnexB(sample)
    }

    public fun avccToAnnexB(sample: ByteArray): ByteArray {
        val wrap = ByteBuffer.wrap(sample)
        //read first 4 bytes, into skip
        //replace first 4 bytes with 0 0 0 1
        //seek to skip plus the 4 byte headerunless EOF

        var offset = 0
        var size: Int

        val sampleSize = sample.size - 1
        while (offset < sampleSize) {
            //wrap.position(offset)
            size = wrap.int
            wrap.putInt(offset, 1)
            offset += (size + 4)
        }

        val array = wrap.array()
        return array
    }

    private fun allOtherSampleSizesBefore(i: Int): Int {
        var sum = 0

        if (i == 0) return sum

        val traf = trafBox()
        val box : TrunBox = traf.boxes["trun"]!! as TrunBox
        for(idx in (i-1) downTo 0) {
            sum += box.sampleRecords[idx].sampleSize
        }
        return sum
    }

    fun trafBox() = moofBox().boxes["traf"] as BoxOfBoxes

    fun moofBox() = (boxes["moof"] as BoxOfBoxes)
}


fun MutableMap<String, Box>.addBox(box : Box) {
    this += box.name to box
}

fun parseBox(bytes: ByteArray): Box {
    //already consumed the size bytes
    val boxName = readBytesAsString(bytes, 0)
    //really should now consume the name bytes
    return when (boxName) {
        "mdat" -> MdatBox.from(bytes)
        "mfhd" -> MfhdBox.from(bytes)
        "tfhd" -> TfhdBox.from(bytes)
        "tfdt" -> TfdtBox.from(bytes)
        "trun" -> TrunBox.from(bytes)
        "stsd" -> StsdBox.from(bytes)
        "avc3" -> Avc3Box.from(bytes)
        "moov","moof", "traf", "trak", "mdia", "minf", "stbl" -> BoxOfBoxes.from(bytes, boxName)
        else -> Box.from(bytes, boxName)
    }
}

fun readBytesAsString(bytes: ByteArray, byteOffset: Int, length: Int = 4): String {
    val charArray = CharArray(length)
    (0 until length).forEach {
        val byte = bytes[byteOffset + it]
        charArray[it] = byte.toChar()
    }
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
