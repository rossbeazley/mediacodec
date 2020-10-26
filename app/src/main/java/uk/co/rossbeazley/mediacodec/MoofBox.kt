package uk.co.rossbeazley.mediacodec

class MoofBox(boxByes: ByteArray) : Box("moof", boxByes, boxByes.size+8) {
    companion object {
        fun from(bytes: ByteArray): MoofBox {

            val moofBox = MoofBox(bytes.sliceArray(4 until bytes.size))

            var bytesToParse = moofBox.payload
            val mfhdSize = boxSize(bytesToParse, 0)
            val sizeHeader = 4
            val mfhdBytes = bytesToParse.sliceArray(sizeHeader until mfhdSize.toInt()) //slice off mfhd worth of bytes
            val mfhdBox = parseBox(mfhdBytes)

            moofBox.boxes.addBox(mfhdBox)

            val remainingBytes = bytesToParse.sliceArray((mfhdSize).toInt() until bytesToParse.size)

            bytesToParse = remainingBytes
            val trafSize = boxSize(bytesToParse, 0)
            val trafBytes = bytesToParse.sliceArray(sizeHeader until trafSize.toInt())
            val trafBox = parseBox(trafBytes)

            moofBox.boxes.addBox(trafBox)

            return moofBox
        }
    }
}