package com.sliver.samples.floatingwindow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.nio.ByteBuffer

class ScreenCapturer(
    private val context: Context,
    private val resultData: Intent
) {
    private val mediaProjectionManager by lazy {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
    }
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    @Synchronized
    fun start() {
        synchronized(this) {
            mediaProjection = mediaProjectionManager
                .getMediaProjection(Activity.RESULT_OK, resultData)
            val displayMetrics = getDisplayMetrics()
            imageReader = ImageReader.newInstance(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                PixelFormat.RGBA_8888,
                2,
            )
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapturer",
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )
        }
    }

    @Synchronized
    fun getNextBitmap(): Bitmap? {
        val immReader = imageReader ?: return null
        val image = immReader.acquireNextImage()
        val plane = image?.planes?.get(0) ?: return null
        val pixelStride = plane.pixelStride
        val width = plane.rowStride / plane.pixelStride
        val height = plane.buffer.capacity() / plane.rowStride

        val bitmap = Bitmap.createBitmap(
            immReader.width, immReader.height, Bitmap.Config.ARGB_8888
        )
        if (width == immReader.width && height == immReader.height) {
            bitmap.copyPixelsFromBuffer(plane.buffer)
        } else {
            val bitmapBuffer = ByteBuffer
                .allocate(immReader.width * immReader.height * pixelStride)
            val temp = ByteArray(plane.rowStride)
            for (i in 0 until immReader.height) {
                plane.buffer.get(temp)
                bitmapBuffer.put(temp, 0, immReader.width * pixelStride)
            }
            bitmapBuffer.flip()
            bitmap.copyPixelsFromBuffer(bitmapBuffer)
        }
        image.close()
        return bitmap
    }

    @Synchronized
    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics
    }

    companion object {
        fun createScreenCaptureIntent(context: Context): Intent {
            val mediaProjectionManager = context
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
            return mediaProjectionManager.createScreenCaptureIntent()
        }

        fun registerScreenCaptureLauncher(
            caller: ActivityResultCaller
        ): ScreenCaptureLauncher {
            return when (caller) {
                is ComponentActivity -> ScreenCaptureLauncher(
                    caller, createScreenCaptureIntent(caller)
                )

                is Fragment -> ScreenCaptureLauncher(
                    caller, createScreenCaptureIntent(caller.requireContext())
                )

                else -> throw IllegalArgumentException(
                    "Support ComponentActivity and Fragment Only"
                )
            }
        }
    }

    class ScreenCaptureLauncher(
        caller: ActivityResultCaller,
        private val captureIntent: Intent,
    ) {
        private var callback: ActivityResultCallback<ActivityResult>? = null

        private val launcher = caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            callback?.onActivityResult(it)
        }

        fun launch(callback: ActivityResultCallback<ActivityResult>) {
            this.callback = callback
            launcher.launch(captureIntent)
        }
    }
}