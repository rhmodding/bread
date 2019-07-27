package rhmodding.bread.editor

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Affine
import javafx.scene.transform.Scale
import rhmodding.bread.Bread
import rhmodding.bread.model.*
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt


abstract class Editor<F : IDataModel>(val app: Bread, val dataFile: File, val data: F, val texture: BufferedImage)
    : BorderPane() {
    
    val canvasPane: VBox = VBox().apply {
        styleClass += "vbox"
    }
    val zoomLabel: Label = Label("Zoom: 100%").apply {
        textAlignment = TextAlignment.RIGHT
    }
    val canvas: Canvas = Canvas(512.0, 512.0)
    var zoomFactor: Double = 1.0
        set(value) {
            field = value.coerceIn(0.25, 4.0)
            zoomLabel.text = "Zoom: ${(field * 100).roundToInt()}%"
        }
    
    val splitPane: SplitPane = SplitPane()
    
    val sidebar: TabPane = TabPane().apply {
        this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
    }
    val spritesTab: SpritesTab<F> = SpritesTab(this)
    val animationsTab: AnimationsTab<F> = AnimationsTab(this)
    
    
    init {
        stylesheets += "style/editor.css"
        center = splitPane
        
        canvasPane.children += canvas
        canvasPane.children += HBox().apply {
            styleClass += "hbox"
            alignment = Pos.CENTER_RIGHT
            children += Button("Reset").apply {
                setOnAction {
                    zoomFactor = 1.0
                    repaintCanvas()
                }
            }
            children += zoomLabel
        }
        
        canvas.onScroll = EventHandler {
            if (it.deltaX > 0 || it.deltaY > 0) {
                zoomFactor *= 2.0.pow(1 / 8.0)
            } else {
                zoomFactor /= 2.0.pow(1 / 8.0)
            }
            repaintCanvas()
        }
        
        sidebar.tabs.addAll(spritesTab, animationsTab)
        sidebar.selectionModel.selectedItemProperty().addListener { _, _, t ->
            repaintCanvas()
            (animationsTab.stepSpriteSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                it.max = data.sprites.size - 1
                it.value = it.value.coerceAtMost(it.max)
            }
            if (t != animationsTab) {
                animationsTab.currentTimeline = null
            }
        }
        
        splitPane.items.addAll(sidebar, canvasPane)
        
        Platform.runLater {
            repaintCanvas()
        }
    }
    
    abstract fun saveData(file: File)
    
    fun repaintCanvas() {
        drawCheckerBackground()
        when (sidebar.selectionModel.selectedItem) {
            spritesTab -> {
                drawSprite(data.sprites[spritesTab.spriteSpinner.value], spritesTab.spritePartSpinner.value)
            }
            animationsTab -> {
                val stepIndex = if (animationsTab.currentTimeline != null) animationsTab.playbackStepProperty.value else animationsTab.aniStepSpinner.value
                val step = animationsTab.currentAnimation.steps.getOrNull(stepIndex)
                if (step != null) {
                    drawAnimationStep(step)
                }
            }
        }
    }
    
    fun drawCheckerBackground(canvas: Canvas = this.canvas, originLines: Boolean = true) {
        val g = canvas.graphicsContext2D
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
        g.save()
        g.transform(getZoomTransformation())
        val blockSize = 16.0
        for (x in ((canvas.width / 2 - canvas.width / zoomFactor / 2.0) / blockSize - 2).toInt()..((canvas.width / 2 + canvas.width / zoomFactor / 2.0) / blockSize + 2).toInt()) {
            for (y in ((canvas.height / 2 - canvas.height / zoomFactor / 2.0) / blockSize - 2).toInt()..((canvas.height / 2 + canvas.height / zoomFactor / 2.0) / blockSize + 2).toInt()) {
                if ((x + y) % 2 != 0) {
                    g.fill = Color.LIGHTGRAY
                } else {
                    g.fill = Color.WHITE
                }
                g.fillRect(x * blockSize, y * blockSize, blockSize, blockSize)
            }
        }
        g.restore()
        
        // Origin lines
        if (originLines) {
            val originLineWidth = 1.0
            g.fill = Color(1.0, 0.0, 0.0, 0.5)
            g.fillRect(canvas.width / 2 - originLineWidth / 2, 0.0, originLineWidth, canvas.height)
            g.fill = Color(0.0, 0.0, 1.0, 0.5)
            g.fillRect(0.0, canvas.height / 2 - originLineWidth / 2, canvas.width, originLineWidth)
        }
    }
    
    fun getZoomTransformation(zoomFactor: Double = this.zoomFactor, canvas: Canvas = this.canvas): Affine = Affine(Scale(zoomFactor, zoomFactor, canvas.width / 2, canvas.height / 2))
    
    open fun drawSprite(sprite: ISprite, selectedPart: Int = -1) {
        val g = canvas.graphicsContext2D
        val img = texture
        for (part in sprite.parts) {
            val subImg = part.createFXSubimage(img)
            g.save()
            g.transform(getZoomTransformation())
            part.transform(canvas, g)
            g.drawImage(subImg, part.posX - canvas.width / 2, part.posY - canvas.height / 2)
            g.restore()
        }
        val part = sprite.parts.getOrNull(selectedPart)
        if (part != null) {
            g.save()
            g.transform(getZoomTransformation())
            part.transform(canvas, g)
            g.globalAlpha = 1.0
            g.stroke = Color.RED
            g.strokeRect(part.posX - canvas.width / 2, part.posY - canvas.height / 2, (part.regionW.toInt() * part.stretchX).absoluteValue * 1.0, (part.regionH.toInt() * part.stretchY).absoluteValue * 1.0)
            g.restore()
        }
    }
    
    open fun drawAnimationStep(step: IAnimationStep) {
        val g = canvas.graphicsContext2D
        val img = texture
        val sprite = data.sprites[step.spriteIndex.toInt()]
        for (part in sprite.parts) {
            val subImg = part.createFXSubimage(img)
            g.save()
            g.transform(getZoomTransformation())
            g.globalAlpha = step.opacity.toInt() / 255.0
            g.transform(Affine(Scale(step.stretchX.toDouble(), step.stretchY.toDouble(), canvas.width / 2, canvas.height / 2)))
            part.transform(canvas, g)
            g.drawImage(subImg, part.posX - canvas.width / 2, part.posY - canvas.height / 2)
            g.restore()
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