package rhmodding.bread.editor

import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.SplitPane
import javafx.scene.control.TabPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import javafx.scene.transform.Scale
import rhmodding.bread.Bread
import rhmodding.bread.model.IDataModel
import rhmodding.bread.model.ISprite
import java.awt.image.BufferedImage
import kotlin.math.absoluteValue


abstract class Editor<F : IDataModel>(val app: Bread, val data: F, val texture: BufferedImage)
    : BorderPane() {
    
    val splitPane: SplitPane = SplitPane()
    
    val sidebar: TabPane = TabPane().apply {
        this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
    }
    val spritesTab: SpritesTab<F> = SpritesTab(this)
    val animationsTab: AnimationsTab<F> = AnimationsTab(this)
    
    val canvasPane: VBox = VBox().apply {
        styleClass += "vbox"
    }
    val canvas: Canvas = Canvas(512.0, 512.0)
    var zoomFactor: Double = 1.0
    
    init {
        stylesheets += "style/editor.css"
        center = splitPane
        
        canvasPane.children += canvas
        
        sidebar.tabs.addAll(spritesTab, animationsTab)
        sidebar.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            repaintCanvas()
        }
        
        splitPane.items.addAll(sidebar, canvasPane)
        
        Platform.runLater {
            repaintCanvas()
        }
    }
    
    fun repaintCanvas() {
        drawCheckerBackground()
        when (sidebar.selectionModel.selectedItem) {
            spritesTab -> {
                drawSprite(data.sprites[spritesTab.spriteSpinner.value], spritesTab.spritePartSpinner.value)
            }
            animationsTab -> {
            
            }
        }
    }
    
    fun drawCheckerBackground() {
        val g = canvas.graphicsContext2D
        g.save()
        g.transform(getZoomTransformation())
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
        val blockSize = 16.0
        for (x in 0..(canvas.width / blockSize).toInt()) {
            for (y in 0..(canvas.height / blockSize).toInt()) {
                if ((x + y) % 2 == 1) {
                    g.fill = Color.LIGHTGRAY
                } else {
                    g.fill = Color.WHITE
                }
                g.fillRect(x * blockSize, y * blockSize, blockSize, blockSize)
            }
        }
        
        // Origin lines
        val originLineWidth = 1.0
        g.fill = Color(1.0, 0.0, 0.0, 0.5)
        g.fillRect(canvas.width / 2 - originLineWidth / 2, 0.0, originLineWidth, canvas.height)
        g.fill = Color(0.0, 0.0, 1.0, 0.5)
        g.fillRect(0.0, canvas.height / 2 - originLineWidth / 2, canvas.width, originLineWidth)
        g.restore()
    }
    
    fun getZoomTransformation(): Affine = Affine(Scale(zoomFactor, zoomFactor, canvas.width / 2, canvas.height / 2))
    
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
    
}