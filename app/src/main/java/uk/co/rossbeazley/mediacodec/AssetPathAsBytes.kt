package uk.co.rossbeazley.mediacodec

import android.content.res.AssetManager

fun asBytes(path : String, assetManager: AssetManager) : ByteArray
{
    val assetFileDescriptor = assetManager.openFd("seg1.m4s")
    val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
    return bytes
}