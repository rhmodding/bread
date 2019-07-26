package rhmodding.bread.model


interface IAnimation {
    val steps: MutableList<out IAnimationStep>
}

interface IAnimationStep {

    var spriteIndex: UShort
    var delay: UShort

    var stretchX: Float
    var stretchY: Float

    var opacity: UByte

}