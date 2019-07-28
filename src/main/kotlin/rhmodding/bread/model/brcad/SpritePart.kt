package rhmodding.bread.model.brcad

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
import kotlin.math.absoluteValue
import kotlin.math.sign


@ExperimentalUnsignedTypes
class SpritePart : ISpritePart {
    
    override var regionX: UShort = 0u
    override var regionY: UShort = 0u
    override var regionW: UShort = 1u
    override var regionH: UShort = 1u
    
    @Unknown
    var unknown: Int = 0
    
    override var posX: Short = 0
    override var posY: Short = 0
    
    override var stretchX: Float = 1f
    override var stretchY: Float = 1f
    
    override var rotation: Float = 0f
    
    override var flipX: Boolean = false
    override var flipY: Boolean = false
    
    override var opacity: UByte = 255u
    
    @Unknown
    var unknownLast: Byte = 0
    
    override fun copy(): SpritePart {
        return SpritePart().also {
            it.regionX = regionX
            it.regionY = regionY
            it.regionW = regionW
            it.regionH = regionH
            it.unknown = unknown
            it.posX = posX
            it.posY = posY
            it.stretchX = stretchX
            it.stretchY = stretchY
            it.rotation = rotation
            it.flipX = flipX
            it.flipY= flipY
            it.opacity = opacity
            it.unknownLast = unknownLast
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
    
    override fun createFXSubimage(texture: BufferedImage, regionSubimage: BufferedImage, multColor: Color): Image {
        // Note that multColor is ignored
        val newWidth = (regionW.toInt() * stretchX).absoluteValue.toInt().coerceAtLeast(1)
        val newHeight = (regionH.toInt() * stretchY).absoluteValue.toInt().coerceAtLeast(1)
        val resized = BufferedImage(newWidth, newHeight, texture.type)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(regionSubimage, 0, 0, newWidth, newHeight, 0, 0, regionW.toInt(), regionH.toInt(), null)
        g.dispose()
        return SwingFXUtils.toFXImage(resized, null)
    }
    
    override fun toString(): String {
        return "SpritePart[region=[$regionX, $regionY, $regionW, $regionH], pos=[$posX, $posY], stretch=[$stretchX, $stretchY], rotation=$rotation, reflect=[x=$flipX, y=$flipY], opacity=$opacity]"
    }
}