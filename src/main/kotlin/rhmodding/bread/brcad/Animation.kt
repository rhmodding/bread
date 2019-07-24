package rhmodding.bread.brcad


@ExperimentalUnsignedTypes
class Animation {

    @Unknown
    var unknown: Short = 0
    val steps: MutableList<AnimationStep> = mutableListOf()

    override fun toString(): String {
        return "Animation=[numSteps=${steps.size}, steps=[${steps.joinToString(separator = "\n")}]]"
    }

}