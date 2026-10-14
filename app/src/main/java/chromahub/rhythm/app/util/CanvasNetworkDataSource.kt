package chromahub.rhythm.app.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException

/** Enforce consent on the video bytes, including each HLS segment and a mid-stream route change. */
@UnstableApi
class CanvasNetworkDataSource(private val delegate: DataSource) : DataSource {
    private var remote = false
    override fun addTransferListener(transferListener: TransferListener) = delegate.addTransferListener(transferListener)
    override fun open(dataSpec: DataSpec): Long {
        remote = dataSpec.uri.scheme in listOf("http", "https")
        checkAllowed()
        return delegate.open(dataSpec)
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        checkAllowed()
        return delegate.read(buffer, offset, length)
    }
    override fun getUri(): Uri? = delegate.uri
    override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders
    override fun close() = delegate.close()
    private fun checkAllowed() {
        if (remote && !ArtworkDownloadPolicy.canvasAllowed()) throw IOException("Motion artwork disabled on this network")
    }
    class Factory(context: Context) : DataSource.Factory {
        private val delegate = DefaultDataSource.Factory(context.applicationContext)
        override fun createDataSource(): DataSource = CanvasNetworkDataSource(delegate.createDataSource())
    }
}
