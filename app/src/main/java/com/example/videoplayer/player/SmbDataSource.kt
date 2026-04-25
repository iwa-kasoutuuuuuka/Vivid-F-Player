package com.example.videoplayer.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.SmbRandomAccessFile
import java.io.IOException

class SmbDataSource(
    private val cifsContext: CIFSContext = SingletonContext.getInstance()
) : BaseDataSource(true) {

    private var file: SmbRandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        try {
            val smbFile = jcifs.smb.SmbFile(dataSpec.uri.toString(), cifsContext)
            file = SmbRandomAccessFile(smbFile, "r")
            
            file?.seek(dataSpec.position)
            
            val length = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                smbFile.length() - dataSpec.position
            }
            
            bytesRemaining = length
            opened = true
            transferStarted(dataSpec)
            return length
        } catch (e: IOException) {
            throw SmbDataSourceException(e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            Math.min(bytesRemaining, length.toLong()).toInt()
        }

        val bytesRead = try {
            file?.read(buffer, offset, bytesToRead) ?: -1
        } catch (e: IOException) {
            throw SmbDataSourceException(e)
        }

        if (bytesRead == -1) {
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            file?.close()
        } catch (e: IOException) {
            throw SmbDataSourceException(e)
        } finally {
            file = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    class SmbDataSourceException(cause: IOException) : IOException(cause)

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbDataSource()
    }
}
