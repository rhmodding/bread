package rhmodding.bread.model.bccad

import rhmodding.bread.model.IAnimation


class Animation : IAnimation {

    override val steps: MutableList<AnimationStep> = mutableListOf()
    var interpolationInt: Int = 0
    var interpolated: Boolean
        get() = (interpolationInt and 0b1) > 0
        set(value) {
            interpolationInt = if (value) (interpolationInt or 0b1) else (interpolationInt and 0b1.inv())
        }
    var name: String = ""
    
    override fun copy(): Animation {
        return Animation().also {
            it.interpolationInt = interpolationInt
            steps.mapTo(it.steps) { it.copy() }
        }
    }
    
    override fun toString(): String {
        return "Animation=[interpolation=0x${interpolationInt.toUInt().toString(16)}, numSteps=${steps.size}, steps=[${steps.joinToString(separator = "\n")}]]"
    }
    
}