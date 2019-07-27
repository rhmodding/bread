package rhmodding.bread.model.bccad

import rhmodding.bread.model.ISprite


class Sprite : ISprite {
    
    override val parts: MutableList<SpritePart> = mutableListOf()
    
    override fun copy(): Sprite {
        return Sprite().also {
            parts.mapTo(it.parts) { it.copy() }
        }
    }
}