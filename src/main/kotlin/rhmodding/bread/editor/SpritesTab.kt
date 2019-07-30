package rhmodding.bread.editor

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.stage.Modality
import javafx.stage.Stage
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
        isFillWidth = true
    }
    val partPropertiesVBox: VBox = VBox()
    
    val addNewSpritePartButton: Button
    
    val spriteSpinner: Spinner<Int> = intSpinnerFactory(0, data.sprites.size - 1, 0).spinnerArrowKeys()
    val spritePartSpinner: Spinner<Int> = intSpinnerFactory(0, 0, 0).spinnerArrowKeys()
    val posXSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeys()
    val posYSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeys()
    val scaleXSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 0.1).spinnerArrowKeys()
    val scaleYSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 0.1).spinnerArrowKeys()
    val rotationSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 1.0).spinnerArrowKeys()
    val flipXCheckbox: CheckBox = CheckBox()
    val flipYCheckbox: CheckBox = CheckBox()
    val opacitySpinner: Spinner<Int> = intSpinnerFactory(0, 255, 255).spinnerArrowKeys()
    
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
        body.children += VBox().apply {
            alignment = Pos.CENTER_LEFT
            
            children += TitledPane("Sprite", VBox().apply {
                styleClass += "vbox"
                alignment = Pos.CENTER_LEFT
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
            }).apply {
                styleClass += "titled-pane"
            }
        }
        body.apply {
            children += TitledPane("Sprite Part", VBox().apply {
                styleClass += "vbox"
                alignment = Pos.CENTER_LEFT
                
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
            }).apply {
                styleClass += "titled-pane"
            }
            
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
            children += partPropertiesVBox.apply {
                children += TitledPane("Position and Scaling", VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT
                    
                    children += GridPane().apply {
                        styleClass += "grid-pane"
                        add(Label("Position X:"), 0, 0)
                        add(posXSpinner, 1, 0)
                        add(Label("Y:").apply {
                            textAlignment = TextAlignment.RIGHT
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 0)
                        add(posYSpinner, 3, 0)
                        
                        add(Label("Scale X:"), 0, 1)
                        add(scaleXSpinner, 1, 1)
                        add(Label("Y:").apply {
                            textAlignment = TextAlignment.RIGHT
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 1)
                        add(scaleYSpinner, 3, 1)
                        
                        add(Label("Flip on X-axis:"), 0, 2)
                        add(flipXCheckbox, 1, 2)
                        add(Label("Y-axis:").apply {
                            textAlignment = TextAlignment.RIGHT
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 2)
                        add(flipYCheckbox, 3, 2)
                        
                        add(Label("Rotation:"), 0, 3)
                        add(rotationSpinner, 1, 3)
                        add(Label(Typography.degree.toString()), 2, 3)
                    }
                }).apply {
                    styleClass += "titled-pane"
                }
                opacitySpinner.valueProperty().addListener { _, _, n ->
                    currentPart.opacity = n.toUByte()
                    this@SpritesTab.editor.repaintCanvas()
                }
                children += TitledPane("Graphics", VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.CENTER_LEFT
                    
                    
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Opacity:")
                        children += opacitySpinner
                    }
                }).apply {
                    styleClass += "titled-pane"
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
                                style = "-fx-base: -fx-default-button"
                            }
                            children += Button("Cancel").apply {
                                isCancelButton = true
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
                stylesheets += "style/editor.css"
                stylesheets += "style/regionPicker.css"
            }
        }
        val window = editor.scene.window
        if (window != null)
            regionPicker.initOwner(window)
        editor.app.addBaseStyleToScene(regionPicker.scene)
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
