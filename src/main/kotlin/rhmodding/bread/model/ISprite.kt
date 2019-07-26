package rhmodding.bread.model

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import java.awt.image.BufferedImage


interface ISprite {
    
    val parts: MutableList<out ISpritePart>
    
}

interface ISpritePart {
    
    var regionX: UShort
    var regionY: UShort
    var regionW: UShort
    var regionH: UShort
    
    var posX: Short
    var posY: Short
    
    var stretchX: Float
    var stretchY: Float
    
    var rotation: Float
    
    var flipX: Boolean
    var flipY: Boolean
    
    var opacity: UByte
    
    fun transform(canvas: Canvas, g: GraphicsContext)
    
    fun createFXSubimage(texture: BufferedImage): Image
    
}
