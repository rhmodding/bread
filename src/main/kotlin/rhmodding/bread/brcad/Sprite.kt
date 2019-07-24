package rhmodding.bread.brcad


@ExperimentalUnsignedTypes
class Sprite {

    @Unknown
    var unknown: Short = 0

    val parts: MutableList<SpritePart> = mutableListOf()

    override fun toString(): String {
        return "Sprite=[numParts=${parts.size}, parts=[${parts.joinToString(separator = "\n")}]]"
    }

}