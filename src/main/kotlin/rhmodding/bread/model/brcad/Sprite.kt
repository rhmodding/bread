package rhmodding.bread.model.brcad

import rhmodding.bread.model.ISprite


@ExperimentalUnsignedTypes
class Sprite : ISprite {

    @Unknown
    var unknown: Short = 0

    override val parts: MutableList<SpritePart> = mutableListOf()

    override fun toString(): String {
        return "Sprite=[numParts=${parts.size}, parts=[${parts.joinToString(separator = "\n")}]]"
    }

}