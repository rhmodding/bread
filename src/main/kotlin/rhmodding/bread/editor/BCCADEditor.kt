package rhmodding.bread.editor

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.HBox
import rhmodding.bread.Bread
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.model.bccad.*
import java.awt.image.BufferedImage
import java.io.File


class BCCADEditor(app: Bread, dataFile: File, data: BCCAD, image: BufferedImage)
    : Editor<BCCAD>(app, dataFile, data, image) {
    
    class BCCADAnimationsTab(editor: Editor<BCCAD>) : AnimationsTab<BCCAD>(editor) {
        
        val animationNameLabel: Label = Label((currentAnimation as Animation).name)
        
        init {
            animationSpinner.valueProperty().addListener { _, _, _ ->
                animationNameLabel.text = (currentAnimation as Animation).name
            }
            
            sectionAnimation.children.add(1, HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Name: ")
                children += animationNameLabel
                children += Button("Change Name").apply {
                    setOnAction {
                        val animation = currentAnimation as Animation
                        TextInputDialog(animation.name).apply {
                            this.title = "Renaming animation \"${animation.name}\""
                            this.headerText = "Rename animation \"${animation.name}\" to...\n"
                        }.showAndWait().ifPresent { newName ->
                            if (newName.isNotBlank()) {
                                val n = newName.take(127)
                                animation.name = n
                                animationNameLabel.text = n
                            }
                        }
                    }
                }
            })
        }
    }
    
    override val spritesTab: SpritesTab<BCCAD> = SpritesTab(this)
    override val animationsTab: AnimationsTab<BCCAD> = BCCADAnimationsTab(this)
    
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