package rhmodding.bread.editor

import com.madgag.gif.fmsware.AnimatedGifEncoder
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.image.WritableImage
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.util.Duration
import org.jcodec.api.awt.AWTSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Rational
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.IDataModel
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.em
import rhmodding.bread.util.intSpinnerFactory
import rhmodding.bread.util.spinnerArrowKeysAndScroll
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt


open class AnimationsTab<F : IDataModel>(editor: Editor<F>) : EditorSubTab<F>(editor, "Animations") {
    
    val body: VBox = VBox().apply {
        isFillWidth = true
    }
    val disableStepControls: BooleanProperty = SimpleBooleanProperty(false)
    val stepPropertiesVBox: VBox = VBox().apply {
        disableProperty().bind(disableStepControls)
    }
    
    val animationSpinner: Spinner<Int> = intSpinnerFactory(0, data.animations.size - 1, 0).spinnerArrowKeysAndScroll()
    val aniStepSpinner: Spinner<Int> = intSpinnerFactory(0, currentAnimation.steps.size - 1, 0).spinnerArrowKeysAndScroll()
    val playStopButton: Button = Button("Play")
    val playbackSlider: Slider = Slider(0.0, 1.0, 0.1)
    val framerateSpinner: Spinner<Int> = intSpinnerFactory(1, 60, 30, 5).spinnerArrowKeysAndScroll().apply {
        styleClass += "short-spinner"
    }
    val stepSpriteSpinner: Spinner<Int> = intSpinnerFactory(0, (data.sprites.size - 1).coerceAtLeast(0), 1).spinnerArrowKeysAndScroll()
    val stepDelaySpinner: Spinner<Int> = intSpinnerFactory(0, 65535, 0).spinnerArrowKeysAndScroll()
    val stepStretchXSpinner: Spinner<Double> = doubleSpinnerFactory(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble(), 1.0, 0.1).spinnerArrowKeysAndScroll()
    val stepStretchYSpinner: Spinner<Double> = doubleSpinnerFactory(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble(), 1.0, 0.1).spinnerArrowKeysAndScroll()
    val stepRotationSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 0.1).spinnerArrowKeysAndScroll()
    val stepOpacitySpinner: Spinner<Int> = intSpinnerFactory(0, 255, 255).spinnerArrowKeysAndScroll()
    
    val numAnimationsLabel: Label = Label("")
    val numAniStepsLabel: Label = Label("")
    
    val currentAnimation: IAnimation
        get() = data.animations[animationSpinner.value]
    val currentAnimationStep: IAnimationStep
        get() = currentAnimation.steps[aniStepSpinner.value]
    
    var currentTimeline: ObjectProperty<Timeline?> = SimpleObjectProperty(null as Timeline?).apply {
        addListener { _, old, new ->
            old?.stop()
            playStopButton.text = if (new == null) "Play" else "Stop"
        }
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
            currentTimeline.value = null
        }
        aniStepSpinner.valueProperty().addListener { _, _, _ ->
            this@AnimationsTab.editor.repaintCanvas()
            updateFieldsForStep()
            currentTimeline.value = null
        }
        playbackSlider.apply {
            prefWidth = 18.0.em
            this.valueProperty().addListener { _, _, new ->
                if (!this.isDisabled) {
                    aniStepSpinner.valueFactory.value = new.toInt()
                }
            }
            this.disableProperty().bind(Bindings.isNotNull(currentTimeline))
        }
        sectionAnimation = VBox().apply {
            styleClass += "vbox"
            alignment = Pos.CENTER_LEFT
            
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Index:").apply {
                    tooltip = Tooltip("Indices start at 0.")
                }
                children += animationSpinner
                children += numAnimationsLabel
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Step Index:").apply {
                    tooltip = Tooltip("Indices start at 0.")
                }
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
                    disableProperty().bind(disableStepControls)
                    setOnAction {
                        if (currentAnimation.steps.isNotEmpty()) {
                            editor.addAnimationStep(currentAnimation, currentAnimationStep.copy())
                            updateStepSpinners(true)
                        }
                    }
                }
                children += Button("Remove").apply {
                    disableProperty().bind(disableStepControls)
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
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Button("Export Step").apply {
                    disableProperty().bind(disableStepControls)
                    setOnAction {
                        val ani = currentAnimation
                        val curStep = aniStepSpinner.value

                        val padCount: Int = ani.steps.size.toString().length.coerceAtLeast(3)
                        val stepNum: String = curStep.toString().padStart(padCount, padChar = '0')

                        val fileChooser = FileChooser()
                        fileChooser.title = "Export the current step of this animation as a PNG image"
                        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG", "*.png"))
                        fileChooser.initialDirectory = editor.dataFile.parentFile
                        fileChooser.initialFileName = "${getAnimationNameForGifExport()}.$stepNum.png"

                        val file = fileChooser.showSaveDialog(null)
                        if (file != null) {
                            val canvas = editor.canvas
                            val showGrid = editor.showGridCheckbox.isSelected

                            val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())

                            editor.drawCheckerBackground(canvas, showGrid = showGrid, darkGrid = false)
                            editor.drawAnimationStep(ani.steps[curStep])

                            var sp: SnapshotParameters = SnapshotParameters()
                            sp.setFill(Color.TRANSPARENT)
                            canvas.snapshot(sp, writableImage)

                            ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);

                            editor.repaintCanvas()
                        }
                    }
                }
                children += Button("Export All Steps").apply {
                    disableProperty().bind(disableStepControls)
                    setOnAction {
                        val ani = currentAnimation
                        val directoryChooser = DirectoryChooser()
                        directoryChooser.title = "Export every step of this animation as PNG images"
                        directoryChooser.initialDirectory = editor.dataFile.parentFile

                        val dir = directoryChooser.showDialog(null)
                        if (dir != null) {
                            var file: File
                            val padCount: Int = ani.steps.size.toString().length.coerceAtLeast(3)
                            ani.steps.forEachIndexed { curStep, step ->
                                val stepNum: String = curStep.toString().padStart(padCount, padChar = '0')

                                val canvas = editor.canvas
                                val showGrid = editor.showGridCheckbox.isSelected

                                val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())

                                editor.drawCheckerBackground(canvas, showGrid = showGrid, darkGrid = false)
                                editor.drawAnimationStep(step)

                                var sp: SnapshotParameters = SnapshotParameters()
                                sp.setFill(Color.TRANSPARENT)
                                canvas.snapshot(sp, writableImage)

                                file = dir.resolve("${getAnimationNameForGifExport()}.$stepNum.png")
                                ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
                            }
                            editor.repaintCanvas()
                        }
                    }
                }
            }
        }
        body.children += TitledPane("Animation", sectionAnimation).apply {
            styleClass += "titled-pane"
        }
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
        stepRotationSpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.rotation = n.toFloat()
            this@AnimationsTab.editor.repaintCanvas()
        }
        stepOpacitySpinner.valueProperty().addListener { _, _, n ->
            currentAnimationStep.opacity = n.toUByte()
            this@AnimationsTab.editor.repaintCanvas()
        }
        playStopButton.setOnAction {
            val timeline = currentTimeline.value
            if (timeline != null) {
                currentTimeline.value = null
            } else {
                val ani = currentAnimation
                if (ani.steps.isNotEmpty()) {
                    val msPerFrame: Double = (1000.0 / framerateSpinner.value)
                    currentTimeline.value = Timeline().apply {
                        var currentTime = 0.0
                        ani.steps.forEachIndexed { index, step ->
                            keyFrames += KeyFrame(Duration(currentTime), EventHandler {
                                playbackStepProperty.value = index
                                editor.repaintCanvas()
                                playbackSlider.value = index.toDouble()
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
            
            children += TitledPane("Playback", VBox().apply {
                styleClass += "vbox"
                alignment = Pos.CENTER_LEFT
                
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Label("Framerate:")
                    children += framerateSpinner
                    children += Label("frames/sec")
                }
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += playStopButton
                    children += Label("Step:")
                    children += playbackSlider
                }
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Button("Export as GIF").apply {
                        disableProperty().bind(disableStepControls)
                        setOnAction {
                            val ani = currentAnimation
                            val fileChooser = FileChooser()
                            fileChooser.title = "Export this animation as an animated GIF"
                            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("GIF", "*.gif"))
                            fileChooser.initialDirectory = editor.dataFile.parentFile
                            fileChooser.initialFileName = "${getAnimationNameForGifExport()}.gif"

                            val file = fileChooser.showSaveDialog(null)
                            if (file != null) {
                                val encoder = AnimatedGifEncoder()
                                encoder.also { e ->
                                    val canvas = editor.canvas
                                    e.start(file.absolutePath)
                                    e.setSize(canvas.width.toInt(), canvas.height.toInt())
                                    val showGrid = editor.showGridCheckbox.isSelected
                                    if (showGrid) {
                                        e.setBackground(java.awt.Color(1f, 1f, 1f, 0f))
                                    } else {
                                        e.setTransparent(java.awt.Color(1f, 1f, 1f, 1f), true)
                                    }
                                    e.setRepeat(0)
                                    val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())
                                    val framerate = framerateSpinner.value
                                    ani.steps.forEach { step ->
                                        e.setDelay((step.delay.toInt() * (1000.0 / framerate).roundToInt()))
                                        editor.drawCheckerBackground(canvas, showGrid = showGrid, darkGrid = false)
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
                    children += Button("Export as MP4").apply {
                        disableProperty().bind(disableStepControls)
                        setOnAction {
                            val ani = currentAnimation
                            val fileChooser = FileChooser()
                            fileChooser.title = "Export this animation as an MP4 video"
                            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("MP4", "*.mp4"))
                            fileChooser.initialDirectory = editor.dataFile.parentFile
                            fileChooser.initialFileName = "${getAnimationNameForGifExport()}.mp4"

                            val file = fileChooser.showSaveDialog(null)
                            if (file != null) {
                                val framerate = framerateSpinner.value
                                val encoder = AWTSequenceEncoder(NIOUtils.writableChannel(file), Rational(framerate, 1))
                                encoder.also { e ->
                                    val canvas = editor.canvas
                                    val showGrid = editor.showGridCheckbox.isSelected
                                    val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())
                                    ani.steps.forEach { step ->
                                        editor.drawCheckerBackground(canvas, showGrid = showGrid, darkGrid = false)
                                        editor.drawAnimationStep(step)
                                        canvas.snapshot(SnapshotParameters(), writableImage)
                                        val buf = SwingFXUtils.fromFXImage(writableImage, null)
                                        repeat(step.delay.toInt()) {
                                            e.encodeImage(buf)
                                        }
                                    }
                                    e.finish()
                                }
                                editor.repaintCanvas()
                            }
                        }
                    }
                    children += Button("Export as PNG sequence").apply {
                        disableProperty().bind(disableStepControls)
                        setOnAction {
                            val ani = currentAnimation
                            val directoryChooser = DirectoryChooser()
                            directoryChooser.title = "Export this animation as a sequence of PNG images"
                            directoryChooser.initialDirectory = editor.dataFile.parentFile

                            val dir = directoryChooser.showDialog(null)
                            if (dir != null) {
                                var file: File
                                val padCount: Int = ani.steps.size.toString().length.coerceAtLeast(3)
                                ani.steps.forEachIndexed { curStep, step ->
                                    val stepNum: String = curStep.toString().padStart(padCount, padChar = '0')

                                    val canvas = editor.canvas
                                    val showGrid = editor.showGridCheckbox.isSelected

                                    val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())

                                    editor.drawCheckerBackground(canvas, showGrid = showGrid, darkGrid = false)
                                    editor.drawAnimationStep(step)

                                    var sp: SnapshotParameters = SnapshotParameters()
                                    sp.setFill(Color.TRANSPARENT)
                                    canvas.snapshot(sp, writableImage)

                                    repeat(step.delay.toInt()) { i ->
                                        file = dir.resolve("${getAnimationNameForGifExport()}.$stepNum.${i}.png")
                                        ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
                                    }
                                }
                                editor.repaintCanvas()
                            }
                        }
                    }
                }
            }).apply {
                styleClass += "titled-pane"
            }
            
            children += TitledPane("Step Properties", VBox().apply {
                styleClass += "vbox"
                alignment = Pos.CENTER_LEFT
                
                children += GridPane().apply {
                    styleClass += "grid-pane"
                    alignment = Pos.CENTER_LEFT
                    
                    add(Label("Sprite Index:"), 0, 0)
                    add(stepSpriteSpinner, 1, 0)
                    
                    add(Label("Delay:"), 0, 1)
                    add(stepDelaySpinner, 1, 1)
                    add(Label("frames"), 2, 1)
                    
                    add(Label("Scale X:"), 0, 2)
                    add(stepStretchXSpinner, 1, 2)
                    add(Label("Y:").apply {
                        GridPane.setHalignment(this, HPos.RIGHT)
                    }, 2, 2)
                    add(stepStretchYSpinner, 3, 2)
                    
                    add(Label("Rotation:"), 0, 3)
                    add(stepRotationSpinner, 1, 3)
                    add(Label("${Typography.degree}"), 2, 3)
                    
                    add(Label("Opacity:"), 0, 4)
                    add(stepOpacitySpinner, 1, 4)
                }
            }).apply {
                styleClass += "titled-pane"
            }
        }
        
        Platform.runLater {
            updateFieldsForStep()
        }
    }
    
    open fun updateFieldsForStep() {
        numAnimationsLabel.text = "(${data.animations.size} total animation${if (data.animations.size == 1) "" else "s"})"
        numAniStepsLabel.text = "(${currentAnimation.steps.size} total step${if (currentAnimation.steps.size == 1) "" else "s"})"
        if (currentAnimation.steps.isEmpty()) {
            disableStepControls.value = true
            return
        }
        val step = currentAnimationStep
        disableStepControls.value = false
        
        stepSpriteSpinner.valueFactoryProperty().get().value = step.spriteIndex.toInt()
        stepDelaySpinner.valueFactoryProperty().get().value = step.delay.toInt()
        stepStretchXSpinner.valueFactoryProperty().get().value = step.stretchX.toDouble()
        stepStretchYSpinner.valueFactoryProperty().get().value = step.stretchY.toDouble()
        stepRotationSpinner.valueFactoryProperty().get().value = step.rotation.toDouble()
        stepOpacitySpinner.valueFactoryProperty().get().value = step.opacity.toInt()
        playbackSlider.apply {
            this.min = 0.0
            this.max = (currentAnimation.steps.size - 1).toDouble()
            this.blockIncrement = 1.0
            this.majorTickUnit = if (max <= 5.0) 1.0 else if (max < 8.0) 2.0 else 4.0
            this.minorTickCount = (majorTickUnit.toInt() - 1).coerceAtMost(max.toInt() - 1)
            this.isShowTickMarks = true
            this.isShowTickLabels = true
            this.isSnapToTicks = true
            this.value = aniStepSpinner.value.toDouble()
        }
    }
    
    protected open fun getAnimationNameForGifExport(): String {
        return "animation_${animationSpinner.value}"
    }
    
}
