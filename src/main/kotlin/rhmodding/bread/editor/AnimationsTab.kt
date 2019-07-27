package rhmodding.bread.editor

import com.madgag.gif.fmsware.AnimatedGifEncoder
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.util.Duration
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.IDataModel
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.intSpinnerFactory
import rhmodding.bread.util.spinnerArrowKeys
import kotlin.math.roundToInt


open class AnimationsTab<F : IDataModel>(val editor: Editor<F>) : Tab("Animations") {
    
    protected val data: F get() = editor.data
    
    val body: VBox = VBox().apply {
        styleClass += "vbox"
    }
    val stepPropertiesVBox: VBox = VBox().apply {
        styleClass += "vbox"
    }
    
    val animationSpinner: Spinner<Int> = intSpinnerFactory(0, data.animations.size - 1, 0).spinnerArrowKeys()
    val aniStepSpinner: Spinner<Int> = intSpinnerFactory(0, currentAnimation.steps.size - 1, 0).spinnerArrowKeys()
    val playStopButton: Button = Button("Play")
    val framerateSpinner: Spinner<Int> = intSpinnerFactory(1, 60, 30).spinnerArrowKeys().apply {
        styleClass += "short-spinner"
    }
    val stepSpriteSpinner: Spinner<Int> = intSpinnerFactory(0, (data.sprites.size - 1).coerceAtLeast(0), 1).spinnerArrowKeys()
    val stepDelaySpinner: Spinner<Int> = intSpinnerFactory(0, 65535, 0).spinnerArrowKeys()
    val stepStretchXSpinner: Spinner<Double> = doubleSpinnerFactory(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble(), 1.0, 0.1).spinnerArrowKeys()
    val stepStretchYSpinner: Spinner<Double> = doubleSpinnerFactory(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble(), 1.0, 0.1).spinnerArrowKeys()
    val stepOpacitySpinner: Spinner<Int> = intSpinnerFactory(0, 255, 255).spinnerArrowKeys()
    
    val numAnimationsLabel: Label = Label("out of")
    val numAniStepsLabel: Label = Label("out of")
    
    val currentAnimation: IAnimation
        get() = data.animations[animationSpinner.value]
    val currentAnimationStep: IAnimationStep
        get() = currentAnimation.steps[aniStepSpinner.value]
    
    var currentTimeline: Timeline? = null
        set(value) {
            field?.stop()
            field = value
            playStopButton.text = if (value == null) "Play" else "Stop"
        }
    val playbackStepProperty: SimpleIntegerProperty = SimpleIntegerProperty(0)
    
    val sectionAnimation: VBox
    
    init {
        content = ScrollPane(body).apply {
            this.hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            this.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        }
        
        animationSpinner.valueProperty().addListener { _, _, _ ->
            this@AnimationsTab.editor.repaintCanvas()
            (aniStepSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                it.max = currentAnimation.steps.size - 1
                it.value = it.value.coerceAtMost(it.max)
            }
            updateFieldsForStep()
            currentTimeline = null
        }
        aniStepSpinner.valueProperty().addListener { _, _, _ ->
            this@AnimationsTab.editor.repaintCanvas()
            updateFieldsForStep()
            currentTimeline = null
        }
        sectionAnimation = VBox().apply {
            styleClass += "vbox"
            alignment = Pos.CENTER_LEFT
            children += Label("Animation:").apply {
                styleClass += "header"
            }
            
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Index:")
                children += animationSpinner
                children += numAnimationsLabel
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Step Index:")
                children += aniStepSpinner
                children += numAniStepsLabel
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                
                fun updateStepSpinners(goToMax: Boolean) {
                    (aniStepSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                        it.max = (currentAnimation.steps.size - 1).coerceAtLeast(0)
                        it.value = if (goToMax) it.max else it.value.coerceAtMost(it.max)
                    }
                    updateFieldsForStep()
                    editor.repaintCanvas()
                }
                
                children += Button("Add New Step").apply {
                    setOnAction {
                        editor.addAnimationStep(currentAnimation, editor.createAnimationStep())
                        updateStepSpinners(true)
                    }
                }
                children += Button("Duplicate").apply {
                    setOnAction {
                        if (currentAnimation.steps.isNotEmpty()) {
                            editor.addAnimationStep(currentAnimation, currentAnimationStep.copy())
                            updateStepSpinners(true)
                        }
                    }
                }
                children += Button("Remove").apply {
                    setOnAction {
                        if (currentAnimation.steps.isNotEmpty()) {
                            val alert = Alert(Alert.AlertType.CONFIRMATION)
                            editor.app.addBaseStyleToDialog(alert.dialogPane)
                            alert.title = "Remove this animation step?"
                            alert.headerText = "Remove this animation step?"
                            alert.contentText = "Are you sure you want to remove this animation step?\nYou won't be able to undo this action."
                            if (alert.showAndWait().get() == ButtonType.OK) {
                                editor.removeAnimationStep(currentAnimation, currentAnimationStep)
                                updateStepSpinners(false)
                            }
                        }
                    }
                }
            }
        }
        body.children += sectionAnimation
        stepSpriteSpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.spriteIndex = n.toUShort()
            this@AnimationsTab.editor.repaintCanvas()
        }
        stepDelaySpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.delay = n.toUShort()
            this@AnimationsTab.editor.repaintCanvas()
        }
        stepStretchXSpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.stretchX = n.toFloat()
            this@AnimationsTab.editor.repaintCanvas()
        }
        stepStretchYSpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.stretchY = n.toFloat()
            this@AnimationsTab.editor.repaintCanvas()
        }
        stepOpacitySpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.opacity = n.toUByte()
            this@AnimationsTab.editor.repaintCanvas()
        }
        playStopButton.setOnAction {
            val timeline = currentTimeline
            if (timeline != null) {
                currentTimeline = null
            } else {
                val ani = currentAnimation
                if (ani.steps.isNotEmpty()) {
                    val msPerFrame: Double = (1000.0 / framerateSpinner.value)
                    currentTimeline = Timeline().apply {
                        var currentTime = 0.0
                        ani.steps.forEachIndexed { index, step ->
                            keyFrames += KeyFrame(Duration(currentTime), EventHandler {
                                playbackStepProperty.value = index
                                editor.repaintCanvas()
                            })
                            currentTime += msPerFrame * step.delay.toInt()
                        }
                        // last frame's delay
                        keyFrames += KeyFrame(Duration(currentTime), EventHandler {})
                        cycleCount = Animation.INDEFINITE
                        play()
                    }
                }
            }
        }
        body.children += stepPropertiesVBox.apply {
            alignment = Pos.CENTER_LEFT
            children += Separator(Orientation.HORIZONTAL)
            children += Label("Playback:").apply {
                styleClass += "header"
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += playStopButton
                children += Label("Framerate:")
                children += framerateSpinner
                children += Label("frames/sec")
            }
            children += Button("Export as GIF").apply {
                setOnAction {
                    val fileChooser = FileChooser()
                    fileChooser.title = "Export this animation as an animated GIF"
                    fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("GIF", "*.gif"))
                    fileChooser.initialDirectory = editor.dataFile.parentFile
                    
                    val file = fileChooser.showSaveDialog(null)
                    if (file != null) {
                        val encoder = AnimatedGifEncoder()
                        encoder.also { e ->
                            val canvas = editor.canvas
                            e.start(file.absolutePath)
                            e.setBackground(java.awt.Color(1f, 1f, 1f, 0f))
                            e.setSize(canvas.width.toInt(), canvas.height.toInt())
                            e.setRepeat(0)
                            val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())
                            val ani = currentAnimation
                            val framerate = framerateSpinner.value
                            ani.steps.forEach { step ->
                                e.setDelay((step.delay.toInt() * (1000.0 / framerate).roundToInt()))
                                editor.drawCheckerBackground(canvas)
                                editor.drawAnimationStep(step)
                                canvas.snapshot(SnapshotParameters(), writableImage)
                                val buf = SwingFXUtils.fromFXImage(writableImage, null)
                                e.addFrame(buf)
                            }
                            e.finish()
                        }
                        editor.repaintCanvas()
                    }
                }
            }
            children += Separator(Orientation.HORIZONTAL)
            children += Label("Step Properties:").apply {
                styleClass += "header"
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Sprite Index:")
                children += stepSpriteSpinner
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Delay:")
                children += stepDelaySpinner
                children += Label("frames")
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Scale X:")
                children += stepStretchXSpinner
                children += Label("Y:")
                children += stepStretchYSpinner
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Opacity:")
                children += stepOpacitySpinner
            }
        }
        
        Platform.runLater {
            updateFieldsForStep()
        }
    }
    
    open fun updateFieldsForStep() {
        numAnimationsLabel.text = "out of ${data.animations.size} animation${if (data.animations.size == 1) "" else "s"}"
        numAniStepsLabel.text = "out of ${currentAnimation.steps.size} step${if (currentAnimation.steps.size == 1) "" else "s"}"
        if (currentAnimation.steps.isEmpty()) {
            stepPropertiesVBox.disableProperty().value = true
            return
        }
        val step = currentAnimationStep
        stepPropertiesVBox.disableProperty().value = false
        
        stepSpriteSpinner.valueFactoryProperty().get().value = step.spriteIndex.toInt()
        stepDelaySpinner.valueFactoryProperty().get().value = step.delay.toInt()
        stepStretchXSpinner.valueFactoryProperty().get().value = step.stretchX.toDouble()
        stepStretchYSpinner.valueFactoryProperty().get().value = step.stretchY.toDouble()
        stepOpacitySpinner.valueFactoryProperty().get().value = step.opacity.toInt()
    }
    
}
