package rhmodding.bread.brcad


@ExperimentalUnsignedTypes
class SpritePart {

    var regionX: UShort = 0u
    var regionY: UShort = 0u
    var regionW: UShort = 1u
    var regionH: UShort = 1u

    @Unknown
    var unknown: Int = 0

    var posX: Short = 0
    var posY: Short = 0

    var stretchX: Float = 1f
    var stretchY: Float = 1f

    var rotation: Float = 0f

    var reflectX: Boolean = false
    var reflectY: Boolean = false

    var opacity: UByte = 255u

    @Unknown
    var unknownLast: Byte = 0

    override fun toString(): String {
        return "SpritePart[region=[$regionX, $regionY, $regionW, $regionH], pos=[$posX, $posY], stretch=[$stretchX, $stretchY], rotation=$rotation, reflect=[x=$reflectX, y=$reflectY], opacity=$opacity]"
    }
}