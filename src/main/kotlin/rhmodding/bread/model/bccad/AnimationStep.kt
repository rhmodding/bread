package rhmodding.bread.model.bccad

import javafx.scene.paint.Color
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.util.Unknown


class AnimationStep : IAnimationStep {
    
    override var spriteIndex: UShort = 0u
    override var delay: UShort = 1u
    override var stretchX: Float = 1f
    override var stretchY: Float = 1f
    override var opacity: UByte = 255u
    
    var depth: Float = 0f
    var translateX: Short = 0
    var translateY: Short = 0
    var color: Color = Color.WHITE
    var rotation: Float = 0f
    @Unknown
    val unknownData: MutableList<Byte> = mutableListOf(0, 0)
    
    override fun copy(): AnimationStep {
        return AnimationStep().also {
            it.spriteIndex = spriteIndex
            it.delay = delay
            it.stretchX = stretchX
            it.stretchY = stretchY
            it.opacity = opacity
            
            it.depth = depth
            it.translateX = translateX
            it.translateY = translateY
            it.color = color
            it.rotation = rotation
            it.unknownData.clear()
            it.unknownData.addAll(unknownData.toMutableList())
        }
    }
    
    
    override fun toString(): String {
        return "AnimationStep=[spriteIndex=$spriteIndex, delay=$delay, stretch=[$stretchX, $stretchY], opacity=$opacity, depth=$depth, translate=[$translateX, $translateY], color=$color]"
    }
    
}