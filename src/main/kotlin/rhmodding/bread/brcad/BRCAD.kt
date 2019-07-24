package rhmodding.bread.brcad

import java.nio.ByteBuffer


@ExperimentalUnsignedTypes
class BRCAD {

    var spritesheetNumber: UShort = 0u
    @Unknown
    var spritesheetControlWord2: UShort = 0u
    var width: UShort = 0u
    var height: UShort = 0u

    @Unknown
    var unknownAfterSpriteCount: Short = 0
    @Unknown
    var unknownAfterAnimationCount: Short = 0

    val sprites: MutableList<Sprite> = mutableListOf()
    val animations: MutableList<Animation> = mutableListOf()

    companion object {
        const val HEADER_MAGIC: Int = 0x0132B4D8

        fun read(bytes: ByteBuffer): BRCAD {
            val magic = bytes.int
            if (magic != HEADER_MAGIC) {
                throw IllegalStateException("BRCAD did not have magic header ${HEADER_MAGIC.toString(16)}, got ${magic.toString(16)}")
            }
            if (bytes.int != 0x0) {
                throw IllegalStateException("Expected next int after magic to be 0")
            }

            return BRCAD().apply {
                spritesheetNumber = bytes.short.toUShort()
                spritesheetControlWord2 = bytes.short.toUShort()
                width = bytes.short.toUShort()
                height = bytes.short.toUShort()

                // Sprites
                val numEntries: Int = bytes.short.toInt()
                unknownAfterSpriteCount = bytes.short
                val entries = mutableListOf<Sprite>()

                repeat(numEntries) {
                    entries += Sprite().apply {
                        val numParts = bytes.short.toUShort().toInt()
                        unknown = bytes.short
                        repeat(numParts) {
                            parts += SpritePart().apply {
                                regionX = bytes.short.toUShort()
                                regionY = bytes.short.toUShort()
                                regionW = bytes.short.toUShort()
                                regionH = bytes.short.toUShort()
                                unknown = bytes.int
                                posX = bytes.short
                                posY = bytes.short
                                stretchX = bytes.float
                                stretchY = bytes.float
                                rotation = bytes.float
                                reflectX = bytes.get() != 0.toByte()
                                reflectY = bytes.get() != 0.toByte()
                                opacity = bytes.get().toUByte()
                                unknownLast = bytes.get()
                            }
                        }
                    }
                }

                sprites.clear()
                sprites.addAll(entries)

                // Animations
                animations.clear()
                val numAnimations = bytes.short.toUShort().toInt()
                unknownAfterAnimationCount = bytes.short
                repeat(numAnimations) {
                    animations += Animation().apply {
                        val numSteps = bytes.short.toUShort().toInt()
                        unknown = bytes.short
                        repeat(numSteps) {
                            steps += AnimationStep().apply {
                                spriteIndex = bytes.short.toUShort()
                                delay = bytes.short.toUShort()
                                unknown1 = bytes.int
                                stretchX = bytes.float
                                stretchY = bytes.float
                                unknown2 = bytes.int
                                opacity = bytes.get().toUByte()
                                unknown3 = bytes.get()
                                unknown4 = bytes.get()
                                unknown5 = bytes.get()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun toString(): String {
        return """BRCAD=[
            |  spritesheetNum=$spritesheetNumber, width=$width, height=$height,
            |  numSprites=${sprites.size},
            |  sprites=[${sprites.joinToString(separator = "\n")}],
            |  numAnimations=${animations.size},
            |  animations=[${animations.joinToString(separator = "\n")}]
            |]""".trimMargin()
    }
}