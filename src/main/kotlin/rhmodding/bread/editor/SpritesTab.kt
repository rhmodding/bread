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


open class SpritesTab<F : IDataModel>(val editor: Editor<F>) : Tab("Sprites") {
    
    protected val data: F get() = editor.data
    
    val body: VBox = VBox().apply {
        styleClass += "vbox"
    }
    val partVBox: VBox = VBox().apply {
        styleClass += "vbox"
    }
    
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
                children += Button("Add New Sprite").apply {
                    disableProperty().value = true
                }
                children += Button("Duplicate").apply {
                    disableProperty().value = true
                }
            }
        }
        body.children += Separator(Orientation.HORIZONTAL)
        body.children += partVBox.apply {
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
                    children += Button("Add New Part").apply {
                        disableProperty().value = true
                    }
                    children += Button("Duplicate").apply {
                        disableProperty().value = true
                    }
                    children += Button("Remove").apply {
                        disableProperty().value = true
                    }
                }
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Button("Move Up").apply {
                        disableProperty().value = true
                    }
                    children += Button("Move Down").apply {
                        disableProperty().value = true
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
    
    open fun updateFieldsForPart() {
        if (currentSprite.parts.isEmpty()) {
            partVBox.disableProperty().value = true
            return
        }
        partVBox.disableProperty().value = false
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
