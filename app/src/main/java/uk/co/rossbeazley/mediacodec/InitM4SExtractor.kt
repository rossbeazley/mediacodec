package uk.co.rossbeazley.mediacodec

class InitM4SExtractor(val bytes: ByteArray) {


    val boxes: MutableMap<String, Box> = mutableMapOf()

    init {


        //moof, it has boxes in it
        // unsigned int(32) size;
        // unsigned int(32) type = boxtype;

        var theBytes = bytes
        while(theBytes.size > 0) {
            val (box, remainingBytes) = sliceOffOneBox(theBytes)
            boxes.addBox(box)
            theBytes = remainingBytes
        }
    }

    private fun sliceOffOneBox(bytes: ByteArray): Pair<Box, ByteArray> {
        val moofSize = boxSize(bytes, 0)
        //slice off Moof worth of bytes
        val sizeHeader = 4
        val moofBytes = bytes.sliceArray(sizeHeader until moofSize.toInt())

        val moofBox = parseBox(moofBytes)
        val remainingBytes = bytes.sliceArray(moofSize.toInt() until bytes.size)
        return Pair(moofBox, remainingBytes)
    }


}

