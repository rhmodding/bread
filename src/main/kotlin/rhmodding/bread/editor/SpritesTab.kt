package rhmodding.bread.editor

import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Affine
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate
import javafx.stage.Modality
import javafx.stage.Stage
import rhmodding.bread.model.bccad.Animation as BCCADAnimation
import rhmodding.bread.model.IDataModel
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.util.doubleSpinnerFactory
import rhmodding.bread.util.intSpinnerFactory
import rhmodding.bread.util.spinnerArrowKeysAndScroll
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.pow


open class SpritesTab<F : IDataModel>(editor: Editor<F>) : EditorSubTab<F>(editor, "Sprites") {

    val body: VBox = VBox().apply {
        isFillWidth = true
    }
    val disablePartControls: BooleanProperty = SimpleBooleanProperty(false)
    val disablePasteControls: BooleanProperty = SimpleBooleanProperty(true)
    val partPropertiesVBox: VBox = VBox().apply {
        disableProperty().bind(disablePartControls)
    }

    val addNewSpritePartButton: Button

    val spriteSpinner: Spinner<Int> = intSpinnerFactory(0, data.sprites.size - 1, 0).spinnerArrowKeysAndScroll()
    val spritePartSpinner: Spinner<Int> = intSpinnerFactory(0, 0, 0).spinnerArrowKeysAndScroll()
    val posXSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeysAndScroll()
    val posYSpinner: Spinner<Int> = intSpinnerFactory(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt(), 0).spinnerArrowKeysAndScroll()
    val scaleXSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 0.1).spinnerArrowKeysAndScroll()
    val scaleYSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 0.1).spinnerArrowKeysAndScroll()
    val rotationSpinner: Spinner<Double> = doubleSpinnerFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, 1.0).spinnerArrowKeysAndScroll()
    val flipXCheckbox: CheckBox = CheckBox()
    val flipYCheckbox: CheckBox = CheckBox()
    val opacitySpinner: Spinner<Int> = intSpinnerFactory(0, 255, 255).spinnerArrowKeysAndScroll()

    val numSpritesLabel: Label = Label("")
    val numSpritePartsLabel: Label = Label("")

    var copyPart: ISpritePart = editor.createSpritePart().apply {
        regionW = 0u
        regionH = 0u
    }
    
    protected var lastEditedRegion: ISpritePart = editor.createSpritePart().apply {
        regionW = 0u
        regionH = 0u
    }

    val currentSprite: ISprite
        get() = data.sprites[spriteSpinner.value]
    val currentPart: ISpritePart
        get() = currentSprite.parts[spritePartSpinner.value]

    val zoomLabel: Label = Label("Zoom: 100%").apply {
        textAlignment = TextAlignment.RIGHT
    }
    var zoomFactor: Double = 1.0
        set(value) {
            field = value.coerceIn(0.10, 4.0)
            zoomLabel.text = "Zoom: ${(field * 100).roundToInt()}%"
        }
    var panX: Double = 0.0
    var panY: Double = 0.0

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
                    children += Label("Index:").apply {
                        tooltip = Tooltip("Indices start at 0.")
                    }
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
                                // Check every step because at this point we don't know if the sprite is used in any anims
                                var cancel = false
                                var isbreak = false
                                val index = data.sprites.indexOf(currentSprite).toUShort()
                                for (anim in data.animations) {
                                    for (step in anim.steps) {
                                        if (step.spriteIndex == index) {
                                            val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                                                editor.app.addBaseStyleToDialog(dialogPane)
                                                if (anim is BCCADAnimation)
                                                    title = "Sprite used in animation " + anim.name
                                                else
                                                    title = "Sprite used in animation " + data.animations.indexOf(anim).toString()
                                                headerText = "Sprite is currently used in an animation"
                                                contentText = "Are you absolutely sure you want to remove this sprite?\nThe animation step(s) will be set to sprite 0 if you do."
                                            }
                                            if (alert.showAndWait().get() == ButtonType.OK) {
                                                isbreak = true
                                                break
                                            }
                                            else {
                                                cancel = true
                                                break
                                            }
                                        }
                                    }
                                    if (isbreak || cancel) break
                                }

                                if (!cancel) {
                                    if (!isbreak) {
                                        // This way, the number of alert boxes is reduced to 1 always
                                        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                                            editor.app.addBaseStyleToDialog(dialogPane)
                                            title = "Remove this sprite?"
                                            headerText = "Remove this sprite?"
                                            contentText = "Are you sure you want to remove this sprite?\nYou won't be able to undo this action."
                                        }      
                                        if (alert.showAndWait().get() == ButtonType.OK) {
                                            editor.removeSprite(currentSprite)
                                            updateSpriteSpinners(false)
                                        }                   
                                    } else {
                                        editor.removeSprite(currentSprite)
                                        updateSpriteSpinners(false)
                                    }

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
                    children += Label("Index:").apply {
                        tooltip = Tooltip("Indices start at 0.")
                    }
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
                                newPart.posX = 512
                                newPart.posY = 512
                                editor.addSpritePart(currentSprite, newPart)
                                updateSpritePartSpinners(true)
                            }
                        }
                    }
                    children += addNewSpritePartButton
                    children += Button("Duplicate").apply {
                        disableProperty().bind(disablePartControls)
                        setOnAction {
                            if (currentSprite.parts.isNotEmpty()) {
                                editor.addSpritePart(currentSprite, currentPart.copy())
                                updateSpritePartSpinners(true)
                            }
                        }
                    }
                    children += Button("Remove").apply {
                        disableProperty().bind(disablePartControls)
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
                        disableProperty().bind(disablePartControls)
                        setOnAction {
                            if (spritePartSpinner.value < currentSprite.parts.size - 1) {
                                Collections.swap(currentSprite.parts, spritePartSpinner.value, spritePartSpinner.value + 1)
                                spritePartSpinner.increment(1)
                            }
                        }
                    }
                    children += Button("Move Down").apply {
                        disableProperty().bind(disablePartControls)
                        setOnAction {
                            if (spritePartSpinner.value > 0) {
                                Collections.swap(currentSprite.parts, spritePartSpinner.value, spritePartSpinner.value - 1)
                                spritePartSpinner.decrement(1)
                            }
                        }
                    }
                }
                children += HBox().apply {
                    fun updateSpritePartSpinners(goToMax: Boolean) {
                        (spritePartSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).also {
                            it.max = (currentSprite.parts.size - 1).coerceAtLeast(0)
                            it.value = if (goToMax) it.max else it.value.coerceAtMost(it.max)
                        }
                        updateFieldsForPart()
                        editor.repaintCanvas()
                    }

                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Button("Copy").apply {
                        disableProperty().bind(disablePartControls)
                        setOnAction {
                            disablePasteControls.value = false
                            copyPart = currentPart
                        }
                    }
                    children += Button("Cut").apply {
                        disableProperty().bind(disablePartControls)
                        setOnAction {
                            disablePasteControls.value = false
                            copyPart = currentPart
                            if (currentSprite.parts.isNotEmpty()) {
                                val alert = Alert(Alert.AlertType.CONFIRMATION)
                                editor.app.addBaseStyleToDialog(alert.dialogPane)
                                alert.title = "Cut this sprite part?"
                                alert.headerText = "Cut this sprite part?"
                                alert.contentText = "Are you sure you want to cut this sprite part?\nYou won't be able to undo this action."
                                if (alert.showAndWait().get() == ButtonType.OK) {
                                    editor.removeSpritePart(currentSprite, currentPart)
                                    updateSpritePartSpinners(false)
                                }
                            }
                        }
                    }
                    children += Button("Paste").apply {
                        disableProperty().bind(disablePasteControls)
                        setOnAction {
                            if (currentSprite.parts.isNotEmpty()) {
                                editor.addSpritePart(currentSprite, copyPart.copy())
                                updateSpritePartSpinners(true)
                            }
                        }
                    }
                }
                children += HBox().apply {
                    styleClass += "hbox"
                    alignment = Pos.CENTER_LEFT
                    children += Button("Edit Region").apply {
                        disableProperty().bind(disablePartControls)
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
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 0)
                        add(posYSpinner, 3, 0)

                        add(Label("Scale X:"), 0, 1)
                        add(scaleXSpinner, 1, 1)
                        add(Label("Y:").apply {
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 1)
                        add(scaleYSpinner, 3, 1)

                        add(Label("Flip X:"), 0, 2)
                        add(flipXCheckbox, 1, 2)
                        add(Label("Y:").apply {
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

                    children += GridPane().apply {
                        styleClass += "grid-pane"

                        add(Label("Opacity:"), 0, 0)
                        add(opacitySpinner, 1, 0)
                    }
                }).apply {
                    styleClass += "titled-pane"
                }
            }
        }

        Platform.runLater {
            updateFieldsForPart()
            (spritePartSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).max = currentSprite.parts.size - 1
        }
    }

    fun openRegionEditor(spritePart: ISpritePart): Boolean {
        val copy: ISpritePart = spritePart.copy()
        val regionPicker = Stage()
        val sheet = editor.texture

        regionPicker.apply {
            title = "Edit Sprite Part Region"
            isResizable = false
            scene = Scene(BorderPane().apply {
                styleClass += "border-pane"
                stylesheets += "style/editor.css"
                stylesheets += "style/regionPicker.css"

                val scaleFactor = (512.0 / max(sheet.width, sheet.height)).coerceAtMost(1.0)
                val canvas = Canvas(sheet.width * scaleFactor, sheet.height * scaleFactor)
                val fxSheet = SwingFXUtils.toFXImage(sheet, null)
        

                val darkGrid = CheckBox("Dark grid").apply {
                    isSelected = editor.darkGridCheckbox.isSelected
                }

                fun getCanvasSheetCameraTransformation(zoomFactor: Double, canvas: Canvas): Affine {
                    return Affine().apply {
                        this.append(Translate(panX, panY))
                        this.append(Scale(zoomFactor, zoomFactor, canvas.width / 2, canvas.height / 2))
                    }
                }

                fun repaintSheetCanvas(fillRegionRect: Boolean = false,
                                       regX: Double = copy.regionX.toDouble() * scaleFactor,
                                       regY: Double = copy.regionY.toDouble() * scaleFactor,
                                       regW: Double = copy.regionW.toDouble() * scaleFactor,
                                       regH: Double = copy.regionH.toDouble() * scaleFactor) {
                    val g = canvas.graphicsContext2D
                    g.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    editor.drawCheckerBackground(canvas, showGrid = true, originLines = false, darkGrid = darkGrid.isSelected)
                    g.save()
                    g.transform(getCanvasSheetCameraTransformation(zoomFactor, canvas))
                    g.drawImage(fxSheet, 0.0, 0.0, canvas.width, canvas.height)
                    if (fillRegionRect) {
                        g.fill = Color(1.0, 0.0, 0.0, 0.35)
                        g.fillRect(regX, regY, regW, regH)
                        g.fill = Color.WHITE
                    }
                    g.stroke = Color.RED
                    g.strokeRect(regX, regY, regW, regH)
                    g.restore()
                }

                repaintSheetCanvas()

                center = ScrollPane().apply scroll@{
                    isFitToWidth = true
                    this.content = StackPane(canvas).apply {
                        alignment = Pos.CENTER
                    }
                }
                BorderPane.setAlignment(center, Pos.CENTER)

                val draggingProperty: BooleanProperty = SimpleBooleanProperty(false)
                val originalRegionLabelText = "Original region: (${spritePart.regionX}, ${spritePart.regionY}, ${spritePart.regionW}, ${spritePart.regionH})"
                val originalRegionLabel: Label = Label(originalRegionLabelText)
                val regionXSpinner: Spinner<Int> = intSpinnerFactory(0, sheet.width, copy.regionX.toInt()).apply {
                    spinnerArrowKeysAndScroll()
                    disableProperty().bind(draggingProperty)
                    valueProperty().addListener { _, _, n ->
                        copy.regionX = n.toUShort()
                        repaintSheetCanvas()
                    }
                }
                val regionYSpinner: Spinner<Int> = intSpinnerFactory(0, sheet.height, copy.regionY.toInt()).apply {
                    spinnerArrowKeysAndScroll()
                    disableProperty().bind(draggingProperty)
                    valueProperty().addListener { _, _, n ->
                        copy.regionY = n.toUShort()
                        repaintSheetCanvas()
                    }
                }
                val regionWSpinner: Spinner<Int> = intSpinnerFactory(0, sheet.width, copy.regionW.toInt()).apply {
                    spinnerArrowKeysAndScroll()
                    disableProperty().bind(draggingProperty)
                    valueProperty().addListener { _, _, n ->
                        copy.regionW = n.toUShort()
                        repaintSheetCanvas()
                    }
                }
                val regionHSpinner: Spinner<Int> = intSpinnerFactory(0, sheet.height, copy.regionH.toInt()).apply {
                    spinnerArrowKeysAndScroll()
                    disableProperty().bind(draggingProperty)
                    valueProperty().addListener { _, _, n ->
                        copy.regionH = n.toUShort()
                        repaintSheetCanvas()
                    }
                }
                
                fun setSpinnersToCopyRegion() {
                    regionXSpinner.valueFactory.value = copy.regionX.toInt()
                    regionYSpinner.valueFactory.value = copy.regionY.toInt()
                    regionWSpinner.valueFactory.value = copy.regionW.toInt()
                    regionHSpinner.valueFactory.value = copy.regionH.toInt()
                }

                //Used to move the pans while zooming out
                fun verifyPan(){
                    val maxPanX = fxSheet.getWidth()*(zoomFactor-1)/2
                    if(panX > maxPanX){
                        panX = maxPanX
                    } else if(panX < maxPanX*-1){
                        panX = maxPanX*-1
                    }

                    val maxPanY = fxSheet.getHeight()*(zoomFactor-1)/2
                    if(panY > maxPanY){
                        panY = maxPanY
                    } else if(panY < maxPanY*-1){
                        panY = maxPanY*-1
                    }
                }
                
                // Dragging support
                with(canvas) {
                    var x = -1
                    var y = -1
                    var w = 0
                    var h = 0

                    var isPanningCanvas = false
                    var prevDragX = 0.0
                    var prevDragY = 0.0

                    fun reset() {
                        x = -1
                        y = -1
                        w = 0
                        h = 0
                        draggingProperty.value = false
                        originalRegionLabel.text = originalRegionLabelText
                    }


                    setOnMousePressed { e ->
                        if (e.button == MouseButton.PRIMARY) {
                            // CALC
                            val centerImgX = fxSheet.getWidth()/2
                            val centerImgY = fxSheet.getHeight()/2
                            x = ((((e.x - centerImgX - panX) / zoomFactor) + centerImgX) / scaleFactor).toInt()
                            y = ((((e.y - centerImgY - panY) / zoomFactor) + centerImgX) / scaleFactor).toInt()
                            repaintSheetCanvas(true, 0.0, 0.0, 0.0, 0.0)
                            draggingProperty.value = true
                            originalRegionLabel.text = "Drag an area"
                        }
                    }
                    setOnMouseReleased { e ->
                        if (e.button == MouseButton.PRIMARY && x >= 0 && y >= 0) {
                            if (x >= 0 && y >= 0) {
                                // Set the copy values and update the spinners
                                val regionX = if (w < 0) x + w else x
                                val regionY = if (h < 0) y + h else y
                                val regionW = w.absoluteValue
                                val regionH = h.absoluteValue
                                
                                copy.regionX = regionX.toUShort()
                                copy.regionY = regionY.toUShort()
                                copy.regionW = regionW.toUShort()
                                copy.regionH = regionH.toUShort()
                                setSpinnersToCopyRegion()
                                
                                repaintSheetCanvas(false)
                            }
                            reset()
                        } else if (e.button == MouseButton.SECONDARY) {
                            isPanningCanvas = false
                            prevDragX = 0.0
                            prevDragY = 0.0
                        }
                    }
                    setOnMouseDragged { e ->
                        if (e.button == MouseButton.PRIMARY && x >= 0 && y >= 0) {
                            val centerImgX = fxSheet.getWidth() / 2
                            val centerImgY = fxSheet.getHeight() / 2
                            w = (e.x / scaleFactor).toInt() - x
                            h = (e.y / scaleFactor).toInt() - y
                            w = ((((e.x - centerImgX - panX) / zoomFactor) + centerImgX) / scaleFactor).toInt() - x
                            h = ((((e.y - centerImgY - panY) / zoomFactor) + centerImgX) / scaleFactor).toInt() - y

                            val regionX = if (w < 0) x + w else x
                            val regionY = if (h < 0) y + h else y
                            val regionW = w.absoluteValue
                            val regionH = h.absoluteValue
                            // Repaint canvas and update label
                            originalRegionLabel.text = "New region: ($regionX, $regionY, $regionW, $regionH)"
                            repaintSheetCanvas(true, regionX.toDouble() * scaleFactor, regionY.toDouble() * scaleFactor, regionW.toDouble() * scaleFactor, regionH.toDouble() * scaleFactor)
                        } else if (e.button == MouseButton.SECONDARY){
                            if (!isPanningCanvas) {
                                isPanningCanvas = true
                            } else {
                                val diffX = e.x - prevDragX
                                val diffY = e.y - prevDragY
                                
                                panX += diffX
                                panY += diffY

                                verifyPan()
                            }
                            prevDragX = e.x
                            prevDragY = e.y
                            repaintSheetCanvas()
                        }
                    }
                }
                
                //Zoom support?
                canvas.onScroll = EventHandler { evt ->
                    if (evt.isShiftDown) {
                        if (evt.deltaX > 0 || evt.deltaY > 0) {
                            zoomFactor += 0.01
                        } else {
                            if (zoomFactor > 1.0){
                                zoomFactor -= 0.01
                            }
                        }
                    } else {
                        if (evt.deltaX > 0 || evt.deltaY > 0) {
                            zoomFactor *= 1.190507733
                        } else {
                            if (zoomFactor > 1.0){
                                zoomFactor /= 2.0.pow(1 / 8.0)
                                if(zoomFactor < 1.0) {
                                    zoomFactor = 1.0
                                }
                            }
                        }
                    }
                    verifyPan()
                    repaintSheetCanvas()
                }
                bottom = VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.TOP_CENTER

                    children += Separator(Orientation.HORIZONTAL)
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Label("Adjust the region using the spinners below and/or by left clicking and dragging on the canvas.")  
                    }
                    children += HBox().apply {
                        styleClass += "hbox"
                        alignment = Pos.CENTER_LEFT
                        children += Button("Reset Preview").apply {
                            setOnAction {
                                zoomFactor = 1.0
                                panX = 0.0
                                panY = 0.0
                                repaintSheetCanvas()
                            }
                        }
                        children += Button("Reset Panning").apply {
                            setOnAction {
                                panX = 0.0
                                panY = 0.0
                                repaintSheetCanvas()
                            }
                        }
                        children += Button("Reset Zoom").apply {
                            setOnAction {
                                zoomFactor = 1.0
                                repaintSheetCanvas()
                            }
                        }
                        children += zoomLabel
                    }
                    children += GridPane().apply {
                        styleClass += "grid-pane"
                        add(Label("Region X:"), 0, 0)
                        add(regionXSpinner, 1, 0)
                        add(Label("Y:").apply {
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 0)
                        add(regionYSpinner, 3, 0)
                        add(Label("Region Width:"), 0, 1)
                        add(regionWSpinner, 1, 1)
                        add(Label("Height:").apply {
                            GridPane.setHalignment(this, HPos.RIGHT)
                        }, 2, 1)
                        add(regionHSpinner, 3, 1)

                        add(darkGrid.apply {
                            selectedProperty().addListener { _, _, _ ->
                                repaintSheetCanvas()
                            }
                        }, 4, 0)
                        add(Button("Set to Last Edited Region").apply {
                            val last = lastEditedRegion
                            val shouldDisable = last.regionW.toUInt() == 0u || last.regionH.toUInt() == 0u
                            
                            if (!shouldDisable) {
                                disableProperty().bind(disablePartControls)
                                tooltip = Tooltip("Last edited region: (${last.regionX}, ${last.regionY}, ${last.regionW}, ${last.regionH})")
                            } else {
                                disableProperty().value = shouldDisable
                            }
                            setOnAction {
                                copy.regionX = last.regionX
                                copy.regionY = last.regionY
                                copy.regionW = last.regionW
                                copy.regionH = last.regionH
                                setSpinnersToCopyRegion()
                                repaintSheetCanvas()
                            }
                        }, 4, 1)
                    }
                    children += Separator(Orientation.HORIZONTAL)
                    children += BorderPane().apply {
                        styleClass += "border-pane"
                        alignment = Pos.CENTER
                        left = HBox().apply {
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
                        right = HBox().apply {
                            styleClass += "hbox"
                            alignment = Pos.CENTER_RIGHT
                            children += originalRegionLabel
                            children += Button("Reset to Original").apply {
                                disableProperty().bind(draggingProperty)
                                setOnAction {
                                    copy.regionX = spritePart.regionX
                                    copy.regionY = spritePart.regionY
                                    copy.regionW = spritePart.regionW
                                    copy.regionH = spritePart.regionH
                                    repaintSheetCanvas()
                                }
                            }
                        }
                    }
                }
            })
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
            lastEditedRegion = copy.copy()
        }
        editor.repaintCanvas()
        return success
    }

    open fun updateFieldsForPart() {
        numSpritesLabel.text = "(${data.sprites.size} total sprite${if (data.sprites.size == 1) "" else "s"})"
        numSpritePartsLabel.text = "(${currentSprite.parts.size} total part${if (currentSprite.parts.size == 1) "" else "s"})"
        if (currentSprite.parts.isEmpty()) {
            disablePartControls.value = true
            return
        }
        disablePartControls.value = false
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
