package ru.hse.miem.cryptotrendreader.core

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.core.net.toUri
import coil3.EventListener
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.load
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.svg.SvgDecoder
import coil3.toBitmap
import com.yandex.div.core.images.BitmapSource
import com.yandex.div.core.images.CachedBitmap
import com.yandex.div.core.images.DivImageDownloadCallback
import com.yandex.div.core.images.DivImageLoader
import com.yandex.div.core.images.LoadReference

class CoilDivImageLoader(
    private val context: Context
) : DivImageLoader {

    private val imageLoader = ImageLoader.Builder(context)
        .components {
            // SVG support
            add(SvgDecoder.Factory())

            // Animated images (GIF / animated WebP / HEIF)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    override fun loadImage(imageUrl: String, imageView: ImageView): LoadReference {
        val imageUri = imageUrl.toUri()
        val disposable = imageView.load(imageUri, imageLoader)
        return LoadReference { disposable.dispose() }
    }

    override fun loadImage(imageUrl: String, callback: DivImageDownloadCallback): LoadReference {
        val imageUri = imageUrl.toUri()
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .allowHardware(false)
            .listener(BitmapRequestListener(callback, imageUri))
            .build()
        val disposable = imageLoader.enqueue(request)
        return LoadReference { disposable.dispose() }
    }

    override fun loadImageBytes(
        imageUrl: String,
        callback: DivImageDownloadCallback
    ): LoadReference {
        val imageUri = imageUrl.toUri()
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .allowHardware(false)
            .listener(GifRequestListener(callback, imageUri))
            .build()
        val disposable = imageLoader.enqueue(request)
        return LoadReference { disposable.dispose() }
    }

    private class BitmapRequestListener(
        private val callback: DivImageDownloadCallback,
        private val imageUri: Uri
    ) : EventListener() {

        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            callback.onSuccess(
                CachedBitmap(
                    result.image.toBitmap(),
                    imageUri,
                    result.dataSource.toBitmapSource()
                )
            )
        }

        override fun onError(request: ImageRequest, result: ErrorResult) {
            callback.onError()
        }
    }

    private class GifRequestListener(
        private val callback: DivImageDownloadCallback,
        private val imageUri: Uri
    ) : EventListener() {

        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            callback.onSuccess(
                CachedBitmap(
                    (result.image.toBitmap()),
                    imageUri,
                    result.dataSource.toBitmapSource()
                )
            )
        }

        override fun onError(request: ImageRequest, result: ErrorResult) {
            callback.onError()
        }
    }
}

private fun DataSource.toBitmapSource(): BitmapSource =
    when (this) {
        DataSource.MEMORY, DataSource.MEMORY_CACHE -> BitmapSource.MEMORY
        DataSource.DISK -> BitmapSource.DISK
        DataSource.NETWORK -> BitmapSource.NETWORK
    }

