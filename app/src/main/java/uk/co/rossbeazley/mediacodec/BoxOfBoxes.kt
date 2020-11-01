package uk.co.rossbeazley.mediacodec

class BoxOfBoxes(name:String, boxByes: ByteArray) : Box(name, boxByes, boxByes.size+8) {



    companion object {
        private fun sliceOffOneBox(bytes: ByteArray): Pair<Box, ByteArray> {
            val boxSize = boxSize(bytes, 0)
            //slice off Moof worth of bytes
            val sizeHeader = 4
            val boxBytes = bytes.sliceArray(sizeHeader until boxSize.toInt())

            val box = parseBox(boxBytes)
            val remainingBytes = bytes.sliceArray(boxSize.toInt() until bytes.size)
            return Pair(box, remainingBytes)
        }

        fun from(bytes: ByteArray, boxName: String): BoxOfBoxes {

            val moofBox = BoxOfBoxes(boxName, bytes.sliceArray(4 until bytes.size))

            var theBytes = moofBox.payload
            while(theBytes.size > 0) {
                val (box, remainingBytes) = sliceOffOneBox(theBytes)
                moofBox.boxes.addBox(box)
                theBytes = remainingBytes
            }

            return moofBox
        }

    }
}