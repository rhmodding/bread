package rhmodding.bread.editor

import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import rhmodding.bread.model.IDataModel
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.intSpinnerFactory
import java.util.*


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
    
    val currentSprite: ISprite
        get() = data.sprites[spriteSpinner.value]
    val currentPart: ISpritePart
        get() = currentSprite.parts[spritePartSpinner.value]
    
    init {
        this.content = ScrollPane(body)
        
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
                        disableProperty().value = true
                        setOnAction {
                            editor.addSpritePart(currentSprite, editor.createSpritePart())
                            updateSpritePartSpinners(true)
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
    }
    
    open fun updateFieldsForPart() {
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
