package rhmodding.bread.model.bccad

import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.util.Unknown
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign


class SpritePart : ISpritePart {
    
    override var regionX: UShort = 0u
    override var regionY: UShort = 0u
    override var regionW: UShort = 1u
    override var regionH: UShort = 1u
    
    override var posX: Short = 0
    override var posY: Short = 0
    
    override var stretchX: Float = 1f
    override var stretchY: Float = 1f
    
    override var rotation: Float = 0f
    
    override var flipX: Boolean = false
    override var flipY: Boolean = false
    
    override var opacity: UByte = 255u
    
    var multColor: Color = Color.WHITE
    var screenColor: Color = Color.BLACK
    var designation: Byte = 0
    @Unknown
    var unknown: Short = 0
    var tlDepth: Float = 0f
    var blDepth: Float = 0f
    var trDepth: Float = 0f
    var brDepth: Float = 0f
    
    @Unknown
    var unknownData: MutableList<Byte> = mutableListOf()
    
    override fun copy(): SpritePart {
        return SpritePart().also {
            it.regionX = regionX
            it.regionY = regionY
            it.regionW = regionW
            it.regionH = regionH
            it.posX = posX
            it.posY = posY
            it.stretchX = stretchX
            it.stretchY = stretchY
            it.rotation = rotation
            it.flipX = flipX
            it.flipY = flipY
            it.opacity = opacity
            it.multColor = multColor
            it.screenColor = screenColor
            it.designation = designation
            it.unknown = unknown
            it.tlDepth = tlDepth
            it.blDepth = blDepth
            it.trDepth = trDepth
            it.brDepth = brDepth
            it.unknownData = unknownData.toMutableList()
        }
    }
    
    override fun transform(canvas: Canvas, g: GraphicsContext) {
        g.globalAlpha *= opacity.toInt() / 255.0
        g.transform(Affine().apply {
            append(Scale(stretchX.sign * 1.0, stretchY.sign * 1.0, posX - canvas.width / 2, posY - canvas.height / 2))
            val pivotX = posX - canvas.width / 2 + regionW.toInt() * stretchX.absoluteValue * 0.5
            val pivotY = posY - canvas.height / 2 + regionH.toInt() * stretchY.absoluteValue * 0.5
            append(Rotate(rotation * stretchX.sign * stretchY.sign * 1.0, pivotX, pivotY))
            if (flipX) {
                append(Scale(-1.0, 1.0, pivotX, pivotY))
            }
            if (flipY) {
                append(Scale(1.0, -1.0, pivotX, pivotY))
            }
        })
    }
    
    override fun createFXSubimage(texture: BufferedImage): Image {
        val region = texture.getSubimage(regionX.toInt(), regionY.toInt(), regionW.toInt(), regionH.toInt())
        val newWidth = abs(region.width * stretchX).toInt().coerceAtLeast(1)
        val newHeight = abs(region.height * stretchY).toInt().coerceAtLeast(1)
        val resized = BufferedImage(newWidth, newHeight, texture.type)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(region, 0, 0, newWidth, newHeight, 0, 0, region.width,
                    region.height, null)
        g.dispose()
        val raster = resized.raster
        val pixels = raster.getPixels(0, 0, raster.width, raster.height, null as IntArray?)
        for (i in 0 until raster.width) {
            for (j in 0 until raster.height) {
                val n = (i + j * raster.width) * 4
                val r = pixels[n] / 255.0
                val g = pixels[n + 1] / 255.0
                val b = pixels[n + 2] / 255.0
                val sr = 1 - (1 - screenColor.red) * (1 - r)
                val sg = 1 - (1 - screenColor.green) * (1 - g)
                val sb = 1 - (1 - screenColor.blue) * (1 - b)
                val mr = r * this.multColor.red
                val mg = g * this.multColor.green
                val mb = b * this.multColor.blue
                pixels[n] = ((sr * (1 - r) + r * mr) * multColor.red * 255).toInt()
                pixels[n + 1] = ((sg * (1 - g) + g * mg) * multColor.green * 255).toInt()
                pixels[n + 2] = ((sb * (1 - b) + b * mb) * multColor.blue * 255).toInt()
            }
        }
        raster.setPixels(0, 0, raster.width, raster.height, pixels)
        
        return SwingFXUtils.toFXImage(resized, null)
    }
    
    override fun toString(): String {
        return "SpritePart[region=[$regionX, $regionY, $regionW, $regionH], pos=[$posX, $posY], stretch=[$stretchX, $stretchY], rotation=$rotation, reflect=[x=$flipX, y=$flipY], opacity=$opacity, multColor=$multColor, screenColor=$screenColor, designation=$designation, tlDepth=$tlDepth, blDepth=$blDepth, trDepth=$trDepth, brDepth=$brDepth]"
    }
}