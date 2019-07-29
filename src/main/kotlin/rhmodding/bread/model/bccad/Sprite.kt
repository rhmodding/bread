package rhmodding.bread.model.bccad

import rhmodding.bread.model.ISprite


class Sprite : ISprite {
    
    override val parts: MutableList<SpritePart> = mutableListOf()
    
    override fun copy(): Sprite {
        return Sprite().also {
            parts.mapTo(it.parts) { it.copy() }
        }
    }
    
    override fun toString(): String {
        return "Sprite=[numParts=${parts.size}, parts=[${parts.joinToString(separator = "\n")}]]"
    }
    
}