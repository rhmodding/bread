package rhmodding.bread.brcad

@ExperimentalUnsignedTypes
class AnimationStep {
    var spriteIndex: UShort = 0u
    var delay: UShort = 1u
    @Unknown
    var unknown1: Int = 0
    var stretchX: Float = 1f
    var stretchY: Float = 1f
    @Unknown
    var unknown2: Int = 0
    var opacity: UByte = 255u

    @Unknown
    var unknown3: Byte = 0
    @Unknown
    var unknown4: Byte = 0
    @Unknown
    var unknown5: Byte = 0

    override fun toString(): String {
        return "AnimationStep=[spriteIndex=$spriteIndex, speed=$delay, stretch=[$stretchX, $stretchY], opacity=$opacity]"
    }
}