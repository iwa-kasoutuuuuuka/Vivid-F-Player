package com.example.videoplayer.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.example.videoplayer.data.manager.SmbCredentialStore
import jcifs.smb.SmbRandomAccessFile
import java.io.IOException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SmbDataSource(
    private val credentialStore: SmbCredentialStore
) : BaseDataSource(true) {

    private var file: SmbRandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        try {
            val url = dataSpec.uri.toString()
            val smbFile = jcifs.smb.SmbFile(url, credentialStore.contextFor(url))
            val raf = SmbRandomAccessFile(smbFile, "r")
            file = raf
            
            raf.seek(dataSpec.position)
            
            val length = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                smbFile.length() - dataSpec.position
            }
            if (length < 0) {
                throw androidx.media3.datasource.DataSourceException(
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
                )
            }
            
            bytesRemaining = length
            opened = true
            transferStarted(dataSpec)
            return length
        } catch (e: Exception) {
            runCatching { file?.close() }
            file = null
            val ioEx = e as? IOException ?: IOException(e)
            throw SmbDataSourceException(ioEx)
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

    class Factory(private val credentialStore: SmbCredentialStore) : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbDataSource(credentialStore)
    }
}
