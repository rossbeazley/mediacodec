package uk.co.rossbeazley.mediacodec

class TrunBox(val sampleCount: Int, boxBytes: ByteArray, val dataOffset: Int) : Box("trun", boxBytes)
{
    /*     class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
           {
               unsigned int(8)   version = v;
               bit(24)  flags   =   f;
           }
           aligned(8) class TrackRunBox extends FullBox(‘trun’, version, tr_flags)
           {
                unsigned   int(32)   sample_count;

                // the following are optional fields
                signed int(32) data_offset;
                unsigned   int(32)   first_sample_flags;

                // all fields in the following array are optional
                {
                    unsigned   int(32)   sample_duration;
                    unsigned   int(32)   sample_size;
                    unsigned   int(32)   sample_flags
                    if (version == 0)
                    {
                        unsigned   int(32)   sample_composition_time_offset;
                    }
                    else
                    {
                        signed   int(32)      sample_composition_time_offset;
                    }
                }[ sample_count ]
           }
 */
    companion object {
        fun from(bytes: ByteArray) : TrunBox {
            //currently ignoring full box header hence 8 (4 from box)
            val boxBytes = bytes.sliceArray(8 until bytes.size)
            val sampleCount = readBitsAsLong(32,boxBytes)
            val dataOffset = readBitsAsLong(32,boxBytes,4)
            return TrunBox(sampleCount.toInt(), boxBytes, dataOffset.toInt())
        }
    }
}