package rhmodding.bread.editor

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Affine
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate
import rhmodding.bread.Bread
import rhmodding.bread.model.*
import rhmodding.bread.scene.MainPane
import rhmodding.bread.util.em
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.io.File
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt


abstract class Editor<F : IDataModel>(val app: Bread, val mainPane: MainPane, val dataFile: File, val data: F,
                                      val texture: BufferedImage, val textureFile: File)
    : BorderPane() {

    private val canvasColors: Array<Color> = arrayOf(Color.WHITE, Color.LIGHTGREY, Color.BLACK, Color.web("#353535FF"))
    val canvasPane: VBox = VBox().apply {
        styleClass += "vbox"
    }
    val zoomLabel: Label = Label("Zoom: 100%").apply {
        textAlignment = TextAlignment.RIGHT
    }
    val canvas: Canvas = Canvas(512.0, 512.0)
    var zoomFactor: Double = 1.0
        set(value) {
            field = value.coerceIn(0.10, 4.0)
            zoomLabel.text = "Zoom: ${(field * 100).roundToInt()}%"
        }
    var panX: Double = 0.0
    var panY: Double = 0.0
    val originLinesCheckbox: CheckBox = CheckBox("Show origin lines").apply {
        isSelected = true
    }
    val showGridCheckbox: CheckBox = CheckBox("Show grid").apply {
        isSelected = true
    }
    val darkGridCheckbox: CheckBox = CheckBox("Dark grid").apply {
        isSelected = false
        disableProperty().bind(Bindings.not(showGridCheckbox.selectedProperty()))
    }

    val splitPane: SplitPane = SplitPane()
    val contextMenu: ContextMenu = ContextMenu()

    val sidebar: TabPane = TabPane().apply {
        this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
    }
    abstract val spritesTab: SpritesTab<F>
    abstract val animationsTab: AnimationsTab<F>
    abstract val debugTab: DebugTab<F>
    abstract val advPropsTab: AdvancedPropertiesTab<F>

    protected val subimageCache: WeakHashMap<Long, Image> = WeakHashMap()

    init {
        stylesheets += "style/editor.css"
        center = splitPane
        minWidth = 15.0.em

        canvasPane.children += canvas
        canvasPane.children += VBox().apply {
            styleClass += "vbox"
            alignment = Pos.CENTER_LEFT
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Button("Reset Preview").apply {
                    setOnAction {
                        zoomFactor = 1.0
                        panX = 0.0
                        panY = 0.0
                        repaintCanvas()
                    }
                }
                children += Button("Reset Panning").apply {
                    setOnAction {
                        panX = 0.0
                        panY = 0.0
                        repaintCanvas()
                    }
                }
                children += Button("Reset Zoom").apply {
                    setOnAction {
                        zoomFactor = 1.0
                        repaintCanvas()
                    }
                }
                children += zoomLabel
            }
            children += Label("Click and drag to pan. Scroll to zoom in/out (hold SHIFT and scroll for finer control).").apply {
                textAlignment = TextAlignment.LEFT
                isWrapText = true
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += showGridCheckbox.apply {
                    selectedProperty().addListener { _, _, _ ->
                        repaintCanvas()
                    }
                }
                children += darkGridCheckbox.apply {
                    selectedProperty().addListener { _, _, _ ->
                        repaintCanvas()
                    }
                }
                children += originLinesCheckbox.apply {
                    selectedProperty().addListener { _, _, _ ->
                        repaintCanvas()
                    }
                }
            }
        }

        canvas.onScroll = EventHandler { evt ->
            if (evt.isShiftDown) {
                if (evt.deltaX > 0 || evt.deltaY > 0) {
                    zoomFactor += 0.01
                } else {
                    zoomFactor -= 0.01
                }
            } else {
                if (evt.deltaX > 0 || evt.deltaY > 0) {
                    zoomFactor *= 1.190507733
                } else {
                    zoomFactor /= 2.0.pow(1 / 8.0)
                }
            }
            repaintCanvas()
        }

        var isPanningCanvas = false
        var prevDragX = 0.0
        var prevDragY = 0.0
        canvas.onMouseReleased = EventHandler {
            isPanningCanvas = false
            prevDragX = 0.0
            prevDragY = 0.0
        }
        canvas.onMouseDragged = EventHandler { evt ->
            if (!isPanningCanvas) {
                isPanningCanvas = true
            } else {
                val diffX = evt.x - prevDragX
                val diffY = evt.y - prevDragY
                
                panX += diffX
                panY += diffY
            }
            prevDragX = evt.x
            prevDragY = evt.y
            repaintCanvas()
        }

        Platform.runLater {
            sidebar.tabs.addAll(spritesTab, animationsTab, debugTab/*, advPropsTab*/)
        }
        sidebar.selectionModel.selectedItemProperty().addListener { _, _, t ->
            repaintCanvas()
            (animationsTab.stepSpriteSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                it.max = data.sprites.size - 1
                it.value = it.value.coerceAtMost(it.max)
            }
            if (t != animationsTab) {
                animationsTab.currentTimeline.value = null
            }
        }

        splitPane.items.addAll(sidebar, canvasPane)

        contextMenu.items.add(MenuItem("Reload Texture").apply {
            setOnAction {
                if (textureFile.exists()) {
                    val (_, sheetImg, wrongDimensions) = loadTexture(textureFile)
                    val g = texture.createGraphics()
                    g.composite = AlphaComposite.Clear
                    g.fillRect(0, 0, texture.width, texture.height)
                    g.composite = AlphaComposite.SrcOver
                    g.drawImage(sheetImg, 0, 0, texture.width, texture.height, null as ImageObserver?)
                    g.dispose()
                    subimageCache.clear()
                    Platform.runLater {
                        repaintCanvas()
                    }
                    Alert(Alert.AlertType.INFORMATION).apply {
                        app.addBaseStyleToDialog(dialogPane)
                        title = "Texture File Loaded"
                        headerText = null
                        contentText = "The texture has been reloaded. (${textureFile.name})${if (wrongDimensions) "\nThe reloaded texture file has different dimensions than what is defined in the data file.\nPlease note that in the editor the texture will be visually scaled to fit the data file's dimensions." else ""}"
                    }.showAndWait()
                } else {
                    Alert(Alert.AlertType.ERROR).apply {
                        app.addBaseStyleToDialog(dialogPane)
                        title = "Missing Texture File"
                        headerText = null
                        contentText = "The texture file that was loaded is missing. Please reload the entire data file."
                    }.showAndWait()
                }
            }
        })

        Platform.runLater {
            repaintCanvas()
        }
    }

    /**
     * @return Triple of raw image, scaled image, boolean indicating WRONG dimensions if true
     */
    protected abstract fun loadTexture(textureFile: File): Triple<BufferedImage, BufferedImage, Boolean>

    abstract fun saveData(file: File)

    fun repaintCanvas() {
        drawCheckerBackground()
        when (sidebar.selectionModel.selectedItem) {
            spritesTab -> {
                drawSprite(data.sprites[spritesTab.spriteSpinner.value], spritesTab.spritePartSpinner.value)
            }
            animationsTab -> {
                val stepIndex = if (animationsTab.currentTimeline.value != null) animationsTab.playbackStepProperty.value else animationsTab.aniStepSpinner.value
                val step = animationsTab.currentAnimation.steps.getOrNull(stepIndex)
                if (step != null) {
                    drawAnimationStep(step)
                }
            }
        }
    }

    fun drawCheckerBackground(canvas: Canvas = this.canvas,
                              showGrid: Boolean = showGridCheckbox.isSelected,
                              originLines: Boolean = originLinesCheckbox.isSelected,
                              darkGrid: Boolean = darkGridCheckbox.isSelected) {
        val g = canvas.graphicsContext2D
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)

        if (showGrid) {
            g.save()
            g.transform(getCanvasCameraTransformation())
            val blockSize: Double = if (zoomFactor < 1.0) {
                when (zoomFactor) {
                    in 0.50..1.00 -> 16.0
                    in 0.25..0.50 -> 32.0
                    in 0.125..0.25 -> 64.0
                    in 0.0625..0.125 -> 128.0
                    else -> 128.0
                }
            } else (16.0)
            val blockColorEven = canvasColors[if (!darkGrid) 0 else 2]
            val blockColorOdd = canvasColors[if (!darkGrid) 1 else 3]
            
//            val blockStartX = ((canvas.width / 2 - canvas.width / zoomFactor / 2.0) / blockSize).toInt() - 2
            
            val blocksInViewableAreaX = (canvas.width / blockSize / zoomFactor).toInt()
            val blocksInViewableAreaY = (canvas.height / blockSize / zoomFactor).toInt()
            val blockStartX = ((-(blocksInViewableAreaX / 2.0))).toInt() - (panX / zoomFactor / blockSize).toInt() - 2 // ((canvas.width / 2 - panX / zoomFactor - canvas.width / zoomFactor / 2) / blockSize).toInt() / 2 - 2
            val blockStartY = ((-(blocksInViewableAreaY / 2.0))).toInt() - (panY / zoomFactor / blockSize).toInt() - 2 // ((canvas.height / 2 - panY / zoomFactor - canvas.height / zoomFactor / 2) / blockSize).toInt() / 2 - 2
            val blockEndX = blockStartX + blocksInViewableAreaX + 4 // ((canvas.width / 2 + canvas.width / zoomFactor / 2.0) / blockSize).toInt() + 2
            val blockEndY = blockStartY + blocksInViewableAreaY + 4 //(canvas.height / 2 + canvas.height / zoomFactor / 2.0) / blockSize).toInt() + 2
            for (x in blockStartX..blockEndX) {
                for (y in blockStartY..blockEndY) {
                    if ((x + y) % 2 != 0) {
                        g.fill = blockColorOdd
                    } else {
                        g.fill = blockColorEven
                    }
                    g.fillRect((canvas.width / 2) + x * blockSize, (canvas.height / 2) + y * blockSize, blockSize, blockSize)
                }
            }
            g.restore()
        }

        // Origin lines
        if (originLines) {
            g.save()
//            g.transform(getCanvasCameraTransformation())
            g.transform(Affine(Translate(panX, panY)))
            val originLineWidth = 1.0
            val xAxis = if (darkGrid && showGrid) Color(0.5, 0.5, 1.0, 0.75) else Color(0.0, 0.0, 1.0, 0.75)
            val yAxis = if (darkGrid && showGrid) Color(1.0, 0.5, 0.5, 0.75) else Color(1.0, 0.0, 0.0, 0.75)
            g.fill = xAxis
            g.fillRect(0.0 - panX, canvas.height / 2 - originLineWidth / 2, canvas.width, originLineWidth)
            g.fill = yAxis
            g.fillRect(canvas.width / 2 - originLineWidth / 2, 0.0 - panY, originLineWidth, canvas.height)
            g.restore()
        }
    }

    fun getCanvasCameraTransformation(zoomFactor: Double = this.zoomFactor, canvas: Canvas = this.canvas): Affine {
        return Affine().apply {
            this.append(Translate(panX, panY))
            this.append(Scale(zoomFactor, zoomFactor, canvas.width / 2, canvas.height / 2))
        }
    }

    open fun drawSprite(sprite: ISprite, selectedPart: Int = -1) {
        val g = canvas.graphicsContext2D
        for (part in sprite.parts) {
            g.save()
            g.transform(getCanvasCameraTransformation())
            val subimage: Image = part.prepareForRendering(getCachedSubimage(part), Color.WHITE, g)
            part.transform(canvas, g)
            g.drawImage(subimage, part.posX - canvas.width / 2, part.posY - canvas.height / 2, (part.regionW.toInt() * part.stretchX).absoluteValue * 1.0, (part.regionH.toInt() * part.stretchY).absoluteValue * 1.0)
            g.restore()
        }
        val part = sprite.parts.getOrNull(selectedPart)
        if (part != null) {
            g.save()
            g.transform(getCanvasCameraTransformation())
            part.transform(canvas, g)
            g.globalAlpha = 1.0
            g.stroke = Color.RED
            g.strokeRect(part.posX - canvas.width / 2, part.posY - canvas.height / 2, (part.regionW.toInt() * part.stretchX).absoluteValue * 1.0, (part.regionH.toInt() * part.stretchY).absoluteValue * 1.0)
            g.restore()
        }
    }

    open fun drawAnimationStep(step: IAnimationStep) {
        val g = canvas.graphicsContext2D
        val sprite = data.sprites[step.spriteIndex.toInt()]
        for (part in sprite.parts) {
            g.save()
            g.transform(getCanvasCameraTransformation())
            g.globalAlpha = step.opacity.toInt() / 255.0

            val subimage: Image = part.prepareForRendering(getCachedSubimage(part), Color.WHITE, g)

            g.transform(Affine().apply {
                appendScale(step.stretchX * 1.0, step.stretchY * 1.0, canvas.width / 2, canvas.height / 2)
                appendRotation(step.rotation * 1.0, canvas.width / 2, canvas.height / 2)
            })
            part.transform(canvas, g)
            g.drawImage(subimage, part.posX - canvas.width / 2, part.posY - canvas.height / 2, (part.regionW.toInt() * part.stretchX).absoluteValue * 1.0, (part.regionH.toInt() * part.stretchY).absoluteValue * 1.0)
            g.restore()
        }
    }

    protected open fun getCachedSubimage(part: ISpritePart): Image {
        val key: Long = (part.regionX.toLong() shl 48) or (part.regionY.toLong() shl 32) or (part.regionW.toLong() shl 16) or (part.regionH.toLong())
        return subimageCache.getOrPut(key) {
            val outOfBounds = part.regionX.toInt() !in 0..texture.width || part.regionY.toInt() !in 0..texture.height || ((part.regionX + part.regionW).toInt() !in 0..texture.width) || ((part.regionY + part.regionH).toInt() !in 0..texture.height)
            val subimage = if (outOfBounds)
                BufferedImage(part.regionW.toInt(), part.regionH.toInt(), BufferedImage.TYPE_INT_ARGB)
            else texture.getSubimage(part.regionX.toInt(), part.regionY.toInt(), part.regionW.toInt(), part.regionH.toInt())
            SwingFXUtils.toFXImage(subimage, null)
        }
    }

    abstract fun addSprite(sprite: ISprite)
    abstract fun removeSprite(sprite: ISprite)
    abstract fun addSpritePart(sprite: ISprite, part: ISpritePart)
    abstract fun removeSpritePart(sprite: ISprite, part: ISpritePart)
    abstract fun addAnimation(animation: IAnimation)
    abstract fun removeAnimation(animation: IAnimation)
    abstract fun addAnimationStep(animation: IAnimation, animationStep: IAnimationStep)
    abstract fun removeAnimationStep(animation: IAnimation, animationStep: IAnimationStep)
    abstract fun createSprite(): ISprite
    abstract fun createSpritePart(): ISpritePart
    abstract fun createAnimation(): IAnimation
    abstract fun createAnimationStep(): IAnimationStep

}