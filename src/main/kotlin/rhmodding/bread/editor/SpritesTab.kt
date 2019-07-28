package rhmodding.bread.editor

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import rhmodding.bread.Bread
import rhmodding.bread.model.IDataModel
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.intSpinnerFactory
import rhmodding.bread.util.spinnerArrowKeys
import java.util.*
import kotlin.math.max


open class SpritesTab<F : IDataModel>(val editor: Editor<F>) : Tab("Sprites") {
    
    protected val data: F get() = editor.data
    
    val body: VBox = VBox().apply {
        styleClass += "vbox"
    }
    val partPropertiesVBox: VBox = VBox().apply {
        styleClass += "vbox"
    }
    
    val addNewSpritePartButton: Button
    
    val spriteSpinner: Spinner<Int> = intSpinnerFactory(0, data.sprites.size - 1, 0)
    val spritePartSpinner: Spinner<Int> = intSpinnerFactory(0, 0, 0)
    val posXSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0)
    val posYSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0)
    val scaleXSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 0.1)
    val scaleYSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 0.1)
    val rotationSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 1.0).apply {
        styleClass += "long-spinner"
    }
    val flipXCheckbox: CheckBox = CheckBox()
    val flipYCheckbox: CheckBox = CheckBox()
    val opacitySpinner: Spinner<Int> = intSpinnerFactory(0, 255, 255)
    
    val numSpritesLabel: Label = Label("")
    val numSpritePartsLabel: Label = Label("")
    
    val currentSprite: ISprite
        get() = data.sprites[spriteSpinner.value]
    val currentPart: ISpritePart
        get() = currentSprite.parts[spritePartSpinner.value]
    
    init {
        this.content = ScrollPane(body).apply {
            this.hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            this.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        }
        
        spriteSpinner.valueProperty().addListener { _, _, _ ->
            this@SpritesTab.editor.repaintCanvas()
            (spritePartSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                it.max = currentSprite.parts.size - 1
                it.value = it.value.coerceAtMost(it.max)
            }
            updateFieldsForPart()
        }
        spritePartSpinner.valueProperty().addListener { _, _, _ ->
            this@SpritesTab.editor.repaintCanvas()
            updateFieldsForPart()
        }
        spriteSpinner.spinnerArrowKeys()
        spritePartSpinner.spinnerArrowKeys()
        body.children += VBox().apply {
            styleClass += "vbox"
            alignment = Pos.CENTER_LEFT
            children += Label("Sprite:").apply {
                styleClass += "header"
            }
            
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Index:")
                children += spriteSpinner
                children += numSpritesLabel
            }
            children += HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                fun updateSpriteSpinners(goToMax: Boolean) {
                    (spriteSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                        it.max = data.sprites.size - 1
                        it.value = if (goToMax) it.max else it.value.coerceAtMost(it.max)
                    }
                    spritePartSpinner.valueFactory.value = 0
                    editor.repaintCanvas()
                }
                children += Button("Add New Sprite").apply {
                    setOnAction {
                        editor.addSprite(editor.createSprite())
                        updateSpriteSpinners(true)
                    }
                }
                children += Button("Duplicate").apply {
                    setOnAction {
                        editor.addSprite(currentSprite.copy())
                        updateSpriteSpinners(true)
                    }
                }
                children += Button("Remove").apply {
                    setOnAction {
                        if (data.sprites.size > 1) {
                            val alert = Alert(Alert.AlertType.CONFIRMATION)
                            editor.app.addBaseStyleToDialog(alert.dialogPane)
                            alert.title = "Remove this sprite?"
                            alert.headerText = "Remove this sprite?"
                            alert.contentText = "Are you sure you want to remove this sprite?\nYou won't be able to undo this action."
                            if (alert.showAndWait().get() == ButtonType.OK) {
                                editor.removeSprite(currentSprite)
                                updateSpriteSpinners(false)
                            }
                        }
                    }
                }
            }
        }
        body.children += Separator(Orientation.HORIZONTAL)
        body.apply {
            children += VBox().apply {
                styleClass += "vbox"
                alignment = Pos.CENTER_LEFT
                children += Label("Sprite Part:").apply {
                    styleClass += "header"
                }
                
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Label("Index:")
                    children += spritePartSpinner
                    children += numSpritePartsLabel
                }
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    fun updateSpritePartSpinners(goToMax: Boolean) {
                        (spritePartSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                            it.max = (currentSprite.parts.size - 1).coerceAtLeast(0)
                            it.value = if (goToMax) it.max else it.value.coerceAtMost(it.max)
                        }
                        updateFieldsForPart()
                        editor.repaintCanvas()
                    }
                    addNewSpritePartButton = Button("Add New Part").apply {
                        setOnAction {
                            val newPart = editor.createSpritePart().apply {
                                regionW = 0u
                                regionH = 0u
                            }
                            val success = openRegionEditor(newPart)
                            if (success) {
                                editor.addSpritePart(currentSprite, newPart)
                                updateSpritePartSpinners(true)
                            }
                        }
                    }
                    children += addNewSpritePartButton
                    children += Button("Duplicate").apply {
                        setOnAction {
                            if (currentSprite.parts.isNotEmpty()) {
                                editor.addSpritePart(currentSprite, currentPart.copy())
                                updateSpritePartSpinners(true)
                            }
                        }
                    }
                    children += Button("Remove").apply {
                        setOnAction {
                            if (currentSprite.parts.isNotEmpty()) {
                                val alert = Alert(Alert.AlertType.CONFIRMATION)
                                editor.app.addBaseStyleToDialog(alert.dialogPane)
                                alert.title = "Remove this sprite part?"
                                alert.headerText = "Remove this sprite part?"
                                alert.contentText = "Are you sure you want to remove this sprite part?\nYou won't be able to undo this action."
                                if (alert.showAndWait().get() == ButtonType.OK) {
                                    editor.removeSpritePart(currentSprite, currentPart)
                                    updateSpritePartSpinners(false)
                                }
                            }
                        }
                    }
                }
                
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Button("Move Up").apply {
                        setOnAction {
                            if (spritePartSpinner.value < currentSprite.parts.size - 1) {
                                Collections.swap(currentSprite.parts, spritePartSpinner.value, spritePartSpinner.value + 1)
                                spritePartSpinner.increment(1)
                            }
                        }
                    }
                    children += Button("Move Down").apply {
                        setOnAction {
                            if (spritePartSpinner.value > 0) {
                                Collections.swap(currentSprite.parts, spritePartSpinner.value, spritePartSpinner.value - 1)
                                spritePartSpinner.decrement(1)
                            }
                        }
                    }
                }
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Button("Edit Region").apply {
                        setOnAction {
                            if (currentSprite.parts.isNotEmpty()) {
                                openRegionEditor(currentPart)
                            }
                        }
                    }
                }
            }
            children += Separator(Orientation.HORIZONTAL)
            
            posXSpinner.valueProperty().addListener { _, _, n ->
                currentPart.posX = n.toShort()
                this@SpritesTab.editor.repaintCanvas()
            }
            posYSpinner.valueProperty().addListener { _, _, n ->
                currentPart.posY = n.toShort()
                this@SpritesTab.editor.repaintCanvas()
            }
            scaleXSpinner.valueProperty().addListener { _, _, n ->
                currentPart.stretchX = n.toFloat()
                this@SpritesTab.editor.repaintCanvas()
            }
            scaleYSpinner.valueProperty().addListener { _, _, n ->
                currentPart.stretchY = n.toFloat()
                this@SpritesTab.editor.repaintCanvas()
            }
            flipXCheckbox.selectedProperty().addListener { _, _, n ->
                currentPart.flipX = n
                this@SpritesTab.editor.repaintCanvas()
            }
            flipYCheckbox.selectedProperty().addListener { _, _, n ->
                currentPart.flipY = n
                this@SpritesTab.editor.repaintCanvas()
            }
            rotationSpinner.valueProperty().addListener { _, _, n ->
                currentPart.rotation = n.toFloat()
                this@SpritesTab.editor.repaintCanvas()
            }
            posXSpinner.spinnerArrowKeys()
            posYSpinner.spinnerArrowKeys()
            scaleXSpinner.spinnerArrowKeys()
            scaleYSpinner.spinnerArrowKeys()
            rotationSpinner.spinnerArrowKeys()
            children += partPropertiesVBox.apply {
                children += VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT
                    children += Label("Position and Scaling:").apply {
                        styleClass += "header"
                    }
                    
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Position X:")
                        children += posXSpinner
                        children += Label("Y:")
                        children += posYSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Scale X:")
                        children += scaleXSpinner
                        children += Label("Y:")
                        children += scaleYSpinner
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Flip on X-axis:")
                        children += flipXCheckbox
                        children += Label("on Y-axis:")
                        children += flipYCheckbox
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Rotation:")
                        children += rotationSpinner
                        children += Label(Typography.degree.toString())
                    }
                }
                children += Separator(Orientation.HORIZONTAL)
                opacitySpinner.spinnerArrowKeys()
                opacitySpinner.valueProperty().addListener { _, _, n ->
                    currentPart.opacity = n.toUByte()
                    this@SpritesTab.editor.repaintCanvas()
                }
                children += VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT
                    children += Label("Graphics:").apply {
                        styleClass += "header"
                    }
                    
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Opacity:")
                        children += opacitySpinner
                    }
                }
            }
        }
        
        Platform.runLater {
            updateFieldsForPart()
        }
    }
    
    fun openRegionEditor(spritePart: ISpritePart): Boolean {
        val copy: ISpritePart = spritePart.copy()
        val regionPicker = Stage()
        val sheet = editor.texture
        regionPicker.apply {
            scene = Scene(Group().apply {
                title = "Edit Sprite Part Region"
                
                val scaleFactor = (640.0 / max(sheet.width, sheet.height)).coerceAtMost(1.0)
                val canvas = Canvas(sheet.width * scaleFactor, sheet.height * scaleFactor)
                val fxSheet = SwingFXUtils.toFXImage(sheet, null)
                
                fun repaintSheetCanvas() {
                    val g = canvas.graphicsContext2D
                    g.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    editor.drawCheckerBackground(canvas, false)
                    g.drawImage(fxSheet, 0.0, 0.0, canvas.width, canvas.height)
                    g.stroke = Color.RED
                    g.strokeRect(copy.regionX.toDouble() * scaleFactor, copy.regionY.toDouble() * scaleFactor, copy.regionW.toDouble() * scaleFactor, copy.regionH.toDouble() * scaleFactor)
                }
                
                repaintSheetCanvas()
                children += ScrollPane().apply {
                    content = VBox().apply {
                        styleClass += "vbox"
                        
                        children += canvas
                        children += Separator(Orientation.HORIZONTAL)
                        children += HBox().apply {
                            styleClass += "hbox"
                            alignment = Pos.CENTER_LEFT
                            children += Label("Adjust the region using the spinners below.")
                            children += Label("Original region: (${spritePart.regionX}, ${spritePart.regionY}, ${spritePart.regionW}, ${spritePart.regionH})")
                            children += Button("Reset to Original").apply {
                                setOnAction {
                                    copy.regionX = spritePart.regionX
                                    copy.regionY = spritePart.regionY
                                    copy.regionW = spritePart.regionW
                                    copy.regionH = spritePart.regionH
                                    repaintSheetCanvas()
                                }
                            }
                        }
                        children += HBox().apply {
                            styleClass += "hbox"
                            alignment = Pos.CENTER_LEFT
                            children += Label("Region X:")
                            children += intSpinnerFactory(0, sheet.width, copy.regionX.toInt()).apply {
                                spinnerArrowKeys()
                                valueProperty().addListener { _, _, n ->
                                    copy.regionX = n.toUShort()
                                    repaintSheetCanvas()
                                }
                            }
                            children += Label("Y:")
                            children += intSpinnerFactory(0, sheet.width, copy.regionY.toInt()).apply {
                                spinnerArrowKeys()
                                valueProperty().addListener { _, _, n ->
                                    copy.regionY = n.toUShort()
                                    repaintSheetCanvas()
                                }
                            }
                        }
                        children += HBox().apply {
                            styleClass += "hbox"
                            alignment = Pos.CENTER_LEFT
                            children += Label("Region Width:")
                            children += intSpinnerFactory(0, sheet.width, copy.regionW.toInt()).apply {
                                spinnerArrowKeys()
                                valueProperty().addListener { _, _, n ->
                                    copy.regionW = n.toUShort()
                                    repaintSheetCanvas()
                                }
                            }
                            children += Label("Height:")
                            children += intSpinnerFactory(0, sheet.width, copy.regionH.toInt()).apply {
                                spinnerArrowKeys()
                                valueProperty().addListener { _, _, n ->
                                    copy.regionH = n.toUShort()
                                    repaintSheetCanvas()
                                }
                            }
                        }
                        children += Separator(Orientation.HORIZONTAL)
                        children += HBox().apply {
                            styleClass += "hbox"
                            alignment = Pos.CENTER_LEFT
                            children += Button("Confirm").apply {
                                setOnAction {
                                    regionPicker.close()
                                }
                            }
                            children += Button("Cancel").apply {
                                setOnAction {
                                    copy.regionW = 0u
                                    copy.regionH = 0u
                                    regionPicker.close()
                                }
                            }
                        }
                    }
                }
            }).apply {
                editor.app.addBaseStyleToScene(this)
                stylesheets += "style/editor.css"
                stylesheets += "style/regionPicker.css"
                icons.setAll(Bread.windowIcons)
            }
        }
        val window = editor.scene.window
        if (window != null)
            regionPicker.initOwner(window)
        regionPicker.initModality(Modality.APPLICATION_MODAL)
        regionPicker.onCloseRequest = EventHandler {
            copy.regionW = 0u
            copy.regionH = 0u
        }
        regionPicker.showAndWait()
        
        var success = false
        // Check that the copy area is valid
        if (copy.regionX.toInt() + copy.regionW.toInt() <= sheet.width &&
                copy.regionY.toInt() + copy.regionH.toInt() <= sheet.height &&
                (copy.regionW > 0u && copy.regionH > 0u)) {
            spritePart.regionX = copy.regionX
            spritePart.regionY = copy.regionY
            spritePart.regionW = copy.regionW
            spritePart.regionH = copy.regionH
            success = true
        }
        editor.repaintCanvas()
        return success
    }
    
    open fun updateFieldsForPart() {
        numSpritesLabel.text = "(${data.sprites.size} total sprite${if (data.sprites.size == 1) "" else "s"})"
        numSpritePartsLabel.text = "(${currentSprite.parts.size} total part${if (currentSprite.parts.size == 1) "" else "s"})"
        if (currentSprite.parts.isEmpty()) {
            partPropertiesVBox.disableProperty().value = true
            return
        }
        partPropertiesVBox.disableProperty().value = false
        val part = currentPart
        posXSpinner.valueFactoryProperty().get().value = part.posX.toInt()
        posYSpinner.valueFactoryProperty().get().value = part.posY.toInt()
        scaleXSpinner.valueFactoryProperty().get().value = part.stretchX.toDouble()
        scaleYSpinner.valueFactoryProperty().get().value = part.stretchY.toDouble()
        rotationSpinner.valueFactoryProperty().get().value = part.rotation.toDouble()
        flipXCheckbox.isSelected = part.flipX
        flipYCheckbox.isSelected = part.flipY
        opacitySpinner.valueFactoryProperty().get().value = part.opacity.toInt()
    }
    
}
