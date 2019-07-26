package rhmodding.bread.editor

import rhmodding.bread.Bread
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.model.brcad.*
import java.awt.image.BufferedImage
import java.io.File


@ExperimentalUnsignedTypes
class BRCADEditor(app: Bread, dataFile: File, data: BRCAD, image: BufferedImage, val headerFile: File /* TODO actually use this file */)
    : Editor<BRCAD>(app, dataFile, data, image) {
    
    override fun saveData(file: File) {
        file.writeBytes(data.toBytes().array())
    }
    
    override fun addSprite(sprite: ISprite) {
        if (sprite is Sprite) {
            data.sprites += sprite
        }
    }
    
    override fun removeSprite(sprite: ISprite) {
        if (sprite is Sprite) {
            data.sprites -= sprite
        }
    }
    
    override fun addSpritePart(sprite: ISprite, part: ISpritePart) {
        if (sprite is Sprite && part is SpritePart) {
            sprite.parts += part
        }
    }
    
    override fun removeSpritePart(sprite: ISprite, part: ISpritePart) {
        if (sprite is Sprite && part is SpritePart) {
            sprite.parts -= part
        }
    }
    
    override fun addAnimation(animation: IAnimation) {
        if (animation is Animation) {
            data.animations += animation
        }
    }
    
    override fun removeAnimation(animation: IAnimation) {
        if (animation is Animation) {
            data.animations -= animation
        }
    }
    
    override fun addAnimationStep(animation: IAnimation, animationStep: IAnimationStep) {
        if (animation is Animation && animationStep is AnimationStep) {
            animation.steps += animationStep
        }
    }
    
    override fun removeAnimationStep(animation: IAnimation, animationStep: IAnimationStep) {
        if (animation is Animation && animationStep is AnimationStep) {
            animation.steps -= animationStep
        }
    }
    
    override fun createSprite(): ISprite {
        return Sprite()
    }
    
    override fun createSpritePart(): ISpritePart {
        return SpritePart()
    }
    
    override fun createAnimation(): IAnimation {
        return Animation()
    }
    
    override fun createAnimationStep(): IAnimationStep {
        return AnimationStep()
    }
    
}