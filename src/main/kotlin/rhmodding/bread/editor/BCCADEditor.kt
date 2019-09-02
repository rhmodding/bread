package rhmodding.bread.editor

import javafx.application.Platform
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Affine
import rhmodding.bread.Bread
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.model.bccad.*
import rhmodding.bread.scene.MainPane
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.intSpinnerFactory
import rhmodding.bread.util.spinnerArrowKeys
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.absoluteValue


class BCCADEditor(app: Bread, mainPane: MainPane, dataFile: File, data: BCCAD, image: BufferedImage)
    : Editor<BCCAD>(app, mainPane, dataFile, data, image) {

    class BCCADSpritesTab(editor: BCCADEditor) : SpritesTab<BCCAD>(editor) {

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

                    children += GridPane().apply {
                        styleClass += "grid-pane"
                        alignment = Pos.CENTER_LEFT

                        add(Label("Designation:"), 0, 0)
                        add(designationSpinner, 1, 0)

                        add(Label("Multiply Color:"), 0, 1)
                        add(multColorPicker, 1, 1)

                        add(Label("Screen Color:"), 0, 2)
                        add(screenColorPicker, 1, 2)

                        add(Label("Top-left Depth:"), 0, 3)
                        add(tlDepthSpinner, 1, 3)
                        add(Label("Top-right Depth:"), 0, 4)
                        add(trDepthSpinner, 1, 4)
                        add(Label("Bottom-left Depth:"), 0, 5)
                        add(blDepthSpinner, 1, 5)
                        add(Label("Bottom-right Depth:"), 0, 6)
                        add(brDepthSpinner, 1, 6)
                    }
                }).apply {
                    styleClass += "titled-pane"
                }
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
                (currentPart as SpritePart).screenColor = n
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

    class BCCADAnimationsTab(editor: BCCADEditor) : AnimationsTab<BCCAD>(editor) {

        val animationNameLabel: Label = Label((currentAnimation as Animation).name).apply {
            id = "name-label"
        }

        val depthSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeys()
        val colorPicker: ColorPicker = ColorPicker(Color.WHITE)
        val translateXSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeys()
        val translateYSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeys()
        var interpolationCheckbox: CheckBox = CheckBox()

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
                            editor.app.addBaseStyleToDialog(this.dialogPane)
                        }.showAndWait().ifPresent { newName ->
                            if (newName.isNotBlank()) {
                                val n = newName.take(127)
                                animation.name = n
                                animationNameLabel.text = n
                                editor.updateContextMenu()
                            }
                        }
                    }
                }
            })
            sectionAnimation.children.add(2, HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Is interpolated?:").apply {
                    tooltip = Tooltip("Please note that animation previews and GIF exports don't support rendering with interpolation.")
                }
                children += interpolationCheckbox
            })

            stepPropertiesVBox.apply {
                children += TitledPane("BCCAD-specific", VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT

                    children += GridPane().apply {
                        styleClass += "grid-pane"

                        add(Label("Depth:"), 0, 0)
                        add(depthSpinner, 1, 0)

                        add(Label("Color:"), 0, 2)
                        add(colorPicker, 1, 2)

                        add(Label("Translation X:"), 0, 3)
                        add(translateXSpinner, 1, 3)
                        add(Label("Y:").apply {
                            textAlignment = TextAlignment.RIGHT
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 3)
                        add(translateYSpinner, 3, 3)
                    }
                }).apply {
                    styleClass += "titled-pane"
                }
            }

            depthSpinner.valueProperty().addListener { _, _, n ->
                (currentAnimationStep as AnimationStep).depth = n.toFloat()
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
            interpolationCheckbox.selectedProperty().addListener { _, _, n ->
                (currentAnimation as Animation).interpolated = n
                this@BCCADAnimationsTab.editor.repaintCanvas()
            }
        }

        override fun getAnimationNameForGifExport(): String {
            return (currentAnimation as? Animation)?.name ?: super.getAnimationNameForGifExport()
        }

        override fun updateFieldsForStep() {
            super.updateFieldsForStep()
            interpolationCheckbox.isSelected = (currentAnimation as Animation).interpolated
            if (currentAnimation.steps.isNotEmpty()) {
                val step = currentAnimationStep as AnimationStep
                depthSpinner.valueFactoryProperty().get().value = step.depth.toDouble()
                colorPicker.value = step.color
                translateXSpinner.valueFactoryProperty().get().value = step.translateX.toInt()
                translateYSpinner.valueFactoryProperty().get().value = step.translateY.toInt()
            }
        }
    }

    class BCCADAdvPropsTab(editor: Editor<BCCAD>) : AdvancedPropertiesTab<BCCAD>(editor)

    override val spritesTab: SpritesTab<BCCAD> = BCCADSpritesTab(this)
    override val animationsTab: AnimationsTab<BCCAD> = BCCADAnimationsTab(this)
    override val advPropsTab: AdvancedPropertiesTab<BCCAD> = BCCADAdvPropsTab(this)

    private val animationsMenuCM: Menu = Menu("Animations")

    init {
        stylesheets += "style/bccadEditor.css"
        this.applyCss()

        contextMenu.items += animationsMenuCM
        updateContextMenu()
    }

    fun updateContextMenu() {
        animationsMenuCM.items.clear()
        data.animations.forEachIndexed { i, a ->
            animationsMenuCM.items += MenuItem("($i): ${a.name}").apply {
                setOnAction {
                    // Select animations tab
                    sidebar.selectionModel.select(animationsTab)
                    animationsTab.animationSpinner.valueFactory.value = i
                }
            }
        }
    }

    override fun drawAnimationStep(step: IAnimationStep) {
        val g = canvas.graphicsContext2D
        val sprite = data.sprites[step.spriteIndex.toInt()]
        for (part in sprite.parts) {
            g.save()
            g.transform(getZoomTransformation())
            g.globalAlpha = step.opacity.toInt() / 255.0
            
            val subimage: Image = part.prepareForRendering(getCachedSubimage(part), (step as? AnimationStep)?.color ?: Color.WHITE, g)

            // BCCAD animation step stuff
            if (step is AnimationStep) {
                g.transform(Affine().apply {
                    appendTranslation(step.translateX.toDouble(), step.translateY.toDouble())
                    appendScale(step.stretchX * 1.0, step.stretchY * 1.0, canvas.width / 2, canvas.height / 2)
                    appendRotation(step.rotation * 1.0, canvas.width / 2, canvas.height / 2)
                })
            }

            part.transform(canvas, g)
            g.drawImage(subimage, part.posX - canvas.width / 2, part.posY - canvas.height / 2, (part.regionW.toInt() * part.stretchX).absoluteValue * 1.0, (part.regionH.toInt() * part.stretchY).absoluteValue * 1.0)
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