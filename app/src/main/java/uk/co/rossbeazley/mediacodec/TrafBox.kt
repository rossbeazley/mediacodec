package uk.co.rossbeazley.mediacodec

class TrafBox(boxBytes: ByteArray) : Box("traf", boxBytes)
{
    companion object {
        fun from(bytes: ByteArray) : TrafBox {
            val trafBox = TrafBox(bytes.sliceArray(4 until bytes.size))

            var bytesToParse = trafBox.payload
            val tfhdSize = boxSize(bytesToParse, 0)
            val sizeHeader = 4
            val tfhdBytes = bytesToParse.sliceArray(sizeHeader until tfhdSize.toInt())
            val tfhdBox = parseBox(tfhdBytes)

            trafBox.boxes.addBox(tfhdBox)
            var remainingBytes = bytesToParse.sliceArray((tfhdSize).toInt() until bytesToParse.size)


            bytesToParse = remainingBytes
            val tfdtSize = boxSize(bytesToParse, 0)
            val tfdtBytes = bytesToParse.sliceArray(sizeHeader until tfdtSize.toInt())
            trafBox.boxes.addBox(parseBox(tfdtBytes))
            remainingBytes = bytesToParse.sliceArray((tfdtSize).toInt() until bytesToParse.size)


            bytesToParse = remainingBytes
            val trunSize = boxSize(bytesToParse, 0)
            val trunBytes = bytesToParse.sliceArray(sizeHeader until trunSize.toInt())
            trafBox.boxes.addBox(parseBox(trunBytes))
            remainingBytes = bytesToParse.sliceArray((trunSize).toInt() until bytesToParse.size)

            return trafBox
        }
    }
}