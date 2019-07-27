package rhmodding.bread.model.bccad

import javafx.scene.paint.Color
import rhmodding.bread.model.IDataModel
import java.nio.ByteBuffer
import java.nio.ByteOrder


class BCCAD : IDataModel {
    
    companion object {
        fun read(bytes: ByteBuffer): BCCAD {
            bytes.order(ByteOrder.LITTLE_ENDIAN)
            return BCCAD().apply {
                timestamp = bytes.int
                sheetW = bytes.short.toUShort()
                sheetH = bytes.short.toUShort()
                repeat(bytes.int) {
                    sprites += Sprite().apply {
                        repeat(bytes.int) {
                            parts += SpritePart().apply {
                                regionX = bytes.short.toUShort()
                                regionY = bytes.short.toUShort()
                                regionW = bytes.short.toUShort()
                                regionH = bytes.short.toUShort()
                                posX = bytes.short
                                posY = bytes.short
                                stretchX = bytes.float
                                stretchY = bytes.float
                                rotation = bytes.float
                                flipX = bytes.get() != 0.toByte()
                                flipY = bytes.get() != 0.toByte()
                                multColor = Color.rgb(bytes.get().toInt() and 0xFF, bytes.get().toInt() and 0xFF, bytes.get().toInt() and 0xFF)
                                screenColor = Color.rgb(bytes.get().toInt() and 0xFF, bytes.get().toInt() and 0xFF, bytes.get().toInt() and 0xFF)
                                opacity = bytes.get().toUByte()
                                repeat(12) {
                                    unknownData.add(bytes.get())
                                }
                                designation = bytes.get()
                                unknown = bytes.short
                                tlDepth = bytes.float
                                blDepth = bytes.float
                                trDepth = bytes.float
                                brDepth = bytes.float
                            }
                        }
                    }
                }
                repeat(bytes.int) {
                    var s = ""
                    val n = bytes.get().toInt()
                    repeat(n) {
                        s += bytes.get().toChar()
                    }
                    repeat(4 - ((n + 1) % 4)) {
                        bytes.get()
                    }
                    animations += Animation().apply {
                        name = s
                        interpolationInt = bytes.int
                        repeat(bytes.int) {
                            steps.add(AnimationStep().apply {
                                spriteIndex = bytes.short.toUShort()
                                delay = bytes.short.toUShort()
                                translateX = bytes.short
                                translateY = bytes.short
                                depth = bytes.float
                                stretchX = bytes.float
                                stretchY = bytes.float
                                rotation = bytes.float
                                color = Color.rgb(bytes.get().toInt() and 0xFF, bytes.get().toInt() and 0xFF, bytes.get().toInt() and 0xFF)
                                bytes.get()
                                unknownData.clear()
                                repeat(2) {
                                    unknownData.add(bytes.get())
                                }
                                opacity = (bytes.short.toInt() and 0xFF).toUByte()
                            })
                        }
                    }
                }
            }
        }
    }
    
    var timestamp: Int = 0
    var sheetW: UShort = 1u
    var sheetH: UShort = 1u
    override val sprites: MutableList<Sprite> = mutableListOf()
    override val animations: MutableList<Animation> = mutableListOf()
    
    fun toBytes(): ByteBuffer {
        val first = ByteArray(12)
        val buf = ByteBuffer.wrap(first).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(timestamp)
                .putShort(sheetW.toShort())
                .putShort(sheetH.toShort())
                .putInt(sprites.size)
        val list = first.toMutableList()
        sprites.forEach { s ->
            val firstBytes = ByteArray(4)
            ByteBuffer.wrap(firstBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(s.parts.size)
            val l = firstBytes.toMutableList()
            s.parts.forEach { p ->
                with(p) {
                    val bytes = ByteArray(0x40)
                    val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    b.putShort(regionX.toShort())
                    b.putShort(regionY.toShort())
                    b.putShort(regionW.toShort())
                    b.putShort(regionH.toShort())
                    b.putShort(posX)
                    b.putShort(posY)
                    b.putFloat(stretchX)
                    b.putFloat(stretchY)
                    b.putFloat(rotation)
                    b.put((if (flipX) 1 else 0).toByte())
                    b.put((if (flipY) 1 else 0).toByte())
                    b.put((multColor.red * 255).toByte())
                    b.put((multColor.green * 255).toByte())
                    b.put((multColor.blue * 255).toByte())
                    b.put((screenColor.red * 255).toByte())
                    b.put((screenColor.green * 255).toByte())
                    b.put((screenColor.blue * 255).toByte())
                    b.put(opacity.toByte())
                    for (i in unknownData) {
                        b.put(i)
                    }
                    b.put(designation)
                    b.putShort(unknown)
                    b.putFloat(tlDepth)
                    b.putFloat(blDepth)
                    b.putFloat(trDepth)
                    b.putFloat(brDepth)
                    l.addAll(bytes.toList())
                }
            }
        }
        
        val animationSizeBytes = ByteArray(4)
        ByteBuffer.wrap(animationSizeBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(animations.size)
        list.addAll(animationSizeBytes.toList())
        animations.forEach { a ->
            with(a) {
                val l = mutableListOf<Byte>()
                l.add(name.length.toByte())
                l.addAll(name.toCharArray().map { it.toByte() })
                l.addAll(ByteArray(4 - ((name.length + 1) % 4)).toList())
                val a = ByteArray(8)
                val bb = ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN)
                bb.putInt(interpolationInt)
                bb.putInt(steps.size)
                l.addAll(a.toList())
                steps.forEach { s ->
                    with(s) {
                        val firstBytes = ByteArray(28)
                        val b = ByteBuffer.wrap(firstBytes).order(ByteOrder.LITTLE_ENDIAN)
                        b.putShort(spriteIndex.toShort())
                        b.putShort(delay.toShort())
                        b.putShort(translateX)
                        b.putShort(translateY)
                        b.putFloat(depth)
                        b.putFloat(stretchX)
                        b.putFloat(stretchY)
                        b.putFloat(rotation)
                        b.put((color.red * 255).toByte())
                        b.put((color.green * 255).toByte())
                        b.put((color.blue * 255).toByte())
                        b.put(0.toByte())
                        val l = firstBytes.toMutableList()
                        l.addAll(unknownData)
                        val lastBytes = ByteArray(2)
                        val b2 = ByteBuffer.wrap(lastBytes).order(ByteOrder.LITTLE_ENDIAN)
                        b2.putShort(opacity.toShort())
                        l.addAll(lastBytes.toList())
                    }
                }
            }
        }
        list.add(0)
        return ByteBuffer.wrap(list.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)
    }
    
    override fun toString(): String {
        return """BCCAD=[
            |  timestamp=$timestamp, width=$sheetW, height=$sheetH,
            |  numSprites=${sprites.size},
            |  sprites=[${sprites.joinToString(separator = "\n")}],
            |  numAnimations=${animations.size},
            |  animations=[${animations.joinToString(separator = "\n")}]
            |]""".trimMargin()
    }
    
}