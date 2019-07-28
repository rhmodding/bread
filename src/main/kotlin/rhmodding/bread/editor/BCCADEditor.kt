package rhmodding.bread.editor

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import javafx.scene.transform.Scale
import rhmodding.bread.Bread
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.model.bccad.*
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.intSpinnerFactory
import rhmodding.bread.util.spinnerArrowKeys
import java.awt.image.BufferedImage
import java.io.File


class BCCADEditor(app: Bread, dataFile: File, data: BCCAD, image: BufferedImage)
    : Editor<BCCAD>(app, dataFile, data, image) {
    
    class BCCADSpritesTab(editor: Editor<BCCAD>) : SpritesTab<BCCAD>(editor) {
        
        val designationSpinner: Spinner<Int> = intSpinnerFactory(0, 255, 255).spinnerArrowKeys()
        val multColorPicker: ColorPicker = ColorPicker(Color.WHITE)
        val screenColorPicker: ColorPicker = ColorPicker(Color.BLACK)
        val tlDepthSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        val blDepthSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        val trDepthSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        val brDepthSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        
        init {
            partPropertiesVBox.apply {
                children += TitledPane("BCCAD-specific", VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT
    
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Designation:")
                        children += designationSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Multiply Color:")
                        children += multColorPicker
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Screen Color:")
                        children += screenColorPicker
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Top-left Depth:")
                        children += tlDepthSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Top-right Depth:")
                        children += trDepthSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Bottom-left Depth:")
                        children += blDepthSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Bottom-right Depth:")
                        children += brDepthSpinner
                    }
                })
            }
            
            designationSpinner.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).designation = n.toUByte()
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            multColorPicker.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).multColor = n
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            screenColorPicker.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).multColor = n
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            tlDepthSpinner.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).tlDepth = n.toFloat()
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            blDepthSpinner.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).blDepth = n.toFloat()
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            trDepthSpinner.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).trDepth = n.toFloat()
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            brDepthSpinner.valueProperty().addListener { _, _, n ->
                (currentPart as SpritePart).brDepth = n.toFloat()
                this@BCCADSpritesTab.editor.repaintCanvas()
            }
            
            Platform.runLater {
                updateFieldsForPart()
            }
        }
        
        override fun updateFieldsForPart() {
            super.updateFieldsForPart()
            if (currentSprite.parts.isNotEmpty()) {
                val part = currentPart as SpritePart
                designationSpinner.valueFactoryProperty().get().value = part.designation.toInt()
                multColorPicker.value = part.multColor
                screenColorPicker.value = part.screenColor
                tlDepthSpinner.valueFactoryProperty().get().value = part.tlDepth.toDouble()
                blDepthSpinner.valueFactoryProperty().get().value = part.blDepth.toDouble()
                trDepthSpinner.valueFactoryProperty().get().value = part.trDepth.toDouble()
                brDepthSpinner.valueFactoryProperty().get().value = part.brDepth.toDouble()
            }
        }
    }
    
    class BCCADAnimationsTab(editor: Editor<BCCAD>) : AnimationsTab<BCCAD>(editor) {
        
        val animationNameLabel: Label = Label((currentAnimation as Animation).name).apply {
            id = "name-label"
        }
        
        val depthSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        val rotationSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        val colorPicker: ColorPicker = ColorPicker(Color.WHITE)
        val translateXSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeys()
        val translateYSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeys()
        
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
            
            stepPropertiesVBox.apply {
                children += TitledPane("BCCAD-specific", VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT
                    
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
        
                        children += Label("Depth:")
                        children += depthSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
        
                        children += Label("Rotation:")
                        children += rotationSpinner
                        children += Label("${Typography.degree}")
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
        
                        children += Label("Color:")
                        children += colorPicker
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
        
                        children += Label("Translation X:")
                        children += translateXSpinner
                        children += Label("Y:")
                        children += translateYSpinner
                    }
                })
            }
            
            depthSpinner.valueProperty().addListener { _, _, n ->
                (currentAnimationStep as AnimationStep).depth = n.toFloat()
                this@BCCADAnimationsTab.editor.repaintCanvas()
            }
            rotationSpinner.valueProperty().addListener { _, _, n ->
                (currentAnimationStep as AnimationStep).rotation = n.toFloat()
                this@BCCADAnimationsTab.editor.repaintCanvas()
            }
            colorPicker.valueProperty().addListener { _, _, n ->
                (currentAnimationStep as AnimationStep).color = n
                this@BCCADAnimationsTab.editor.repaintCanvas()
            }
            translateXSpinner.valueProperty().addListener { _, _, n ->
                (currentAnimationStep as AnimationStep).translateX = n.toShort()
                this@BCCADAnimationsTab.editor.repaintCanvas()
            }
            translateYSpinner.valueProperty().addListener { _, _, n ->
                (currentAnimationStep as AnimationStep).translateY = n.toShort()
                this@BCCADAnimationsTab.editor.repaintCanvas()
            }
        }
        
        override fun updateFieldsForStep() {
            super.updateFieldsForStep()
            if (currentAnimation.steps.isNotEmpty()) {
                val step = currentAnimationStep as AnimationStep
                depthSpinner.valueFactoryProperty().get().value = step.depth.toDouble()
                rotationSpinner.valueFactoryProperty().get().value = step.rotation.toDouble()
                colorPicker.value = step.color
                translateXSpinner.valueFactoryProperty().get().value = step.translateX.toInt()
                translateYSpinner.valueFactoryProperty().get().value = step.translateY.toInt()
            }
        }
    }
    
    override val spritesTab: SpritesTab<BCCAD> = BCCADSpritesTab(this)
    override val animationsTab: AnimationsTab<BCCAD> = BCCADAnimationsTab(this)
    
    init {
        stylesheets += "style/bccadEditor.css"
        this.applyCss()
    }
    
    override fun drawAnimationStep(step: IAnimationStep) {
        val g = canvas.graphicsContext2D
        val img = texture
        val sprite = data.sprites[step.spriteIndex.toInt()]
        for (part in sprite.parts) {
            val subImg = part.createFXSubimage(img)
            g.save()
            g.transform(getZoomTransformation())
            g.globalAlpha = step.opacity.toInt() / 255.0
            
            // BCCAD animation step stuff
            if (step is AnimationStep) {
                g.transform(Affine().apply {
                    appendTranslation(step.translateX.toDouble(), step.translateY.toDouble())
                    appendScale(step.stretchX * 1.0, step.stretchY * 1.0, canvas.width / 2, canvas.height / 2)
                    appendRotation(step.rotation * 1.0, canvas.width / 2, canvas.height / 2)
                })
            }
            
            g.transform(Affine(Scale(step.stretchX.toDouble(), step.stretchY.toDouble(), canvas.width / 2, canvas.height / 2)))
            part.transform(canvas, g)
            g.drawImage(subImg, part.posX - canvas.width / 2, part.posY - canvas.height / 2)
            g.restore()
        }
    }
    
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