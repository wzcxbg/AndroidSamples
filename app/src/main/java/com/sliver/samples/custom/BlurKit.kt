package com.sliver.samples.custom

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RSIllegalArgumentException
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

class BlurKit {
    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    private var scriptInput: Allocation? = null
    private var scriptOutput: Allocation? = null

    fun prepare(context: Context) {
        renderScript = RenderScript.create(context)
        blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
    }

    fun blur(input: Bitmap, output: Bitmap, radius: Float) {
        blurScript?.setRadius(radius)
        if (scriptInput == null) {
            scriptInput = createAllocation(input)
        }
        if (scriptOutput == null) {
            scriptOutput = createAllocation(output)
        }
        try {
            scriptInput?.copyFrom(input)
            blurScript?.setInput(scriptInput)
        } catch (e: RSIllegalArgumentException) {
            scriptInput?.destroy()
            scriptInput = createAllocation(input)
            scriptInput?.copyFrom(input)
            blurScript?.setInput(scriptInput)
        }
        try {
            blurScript?.forEach(scriptOutput)
            scriptOutput?.copyTo(output)
        } catch (e: RSIllegalArgumentException) {
            scriptOutput?.destroy()
            scriptOutput = createAllocation(output)
            blurScript?.forEach(scriptOutput)
            scriptOutput?.copyTo(output)
        }
    }

    fun release() {
        scriptOutput?.destroy()
        scriptOutput = null
        scriptInput?.destroy()
        scriptInput = null
        blurScript?.destroy()
        blurScript = null
        renderScript?.destroy()
        renderScript = null
    }

    private fun createAllocation(bitmap: Bitmap): Allocation? {
        return Allocation.createFromBitmap(
            renderScript, bitmap,
            Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
        )
    }
}