package rhmodding.bread.scene

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.event.Event
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.stage.FileChooser
import rhmodding.bread.Bread
import rhmodding.bread.editor.BCCADEditor
import rhmodding.bread.editor.BRCADEditor
import rhmodding.bread.editor.Editor
import rhmodding.bread.model.bccad.BCCAD
import rhmodding.bread.model.brcad.BRCAD
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.Callable
import javax.imageio.ImageIO


class MainPane(val app: Bread) : BorderPane() {
    
    val toolbar: MenuBar = MenuBar()
    val centrePane: StackPane = StackPane()
    val tabPane: TabPane = TabPane()
    val bottomPane: VBox = VBox()
    
    val noTabsLabel: Label
    
    init {
        stylesheets += "style/mainPane.css"
        
        top = toolbar
        center = centrePane
        bottom = bottomPane
        
        val openKeyCombo = KeyCombination.keyCombination("Shortcut+O")
        noTabsLabel = Label("Open a file using File > Open... (${openKeyCombo.displayText}),\nor drag-and-drop a .brcad or .bccad file here").apply {
            id = "no-tabs-label"
            textAlignment = TextAlignment.CENTER
            maxWidth = Double.MAX_VALUE
            maxHeight = Double.MAX_VALUE
            alignment = Pos.CENTER
        }
        
        fun attemptCloseCurrentTab() {
            with(tabPane) {
                if (!this.selectionModel.isEmpty) {
                    this.selectionModel.selectedItem?.also { tab ->
                        val reqEvt = Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT)
                        Event.fireEvent(tab, reqEvt)
                        if (!reqEvt.isConsumed) {
                            tabs.remove(tab)
                            if (tab.onClosed != null) {
                                Event.fireEvent(tab, Event(Tab.CLOSED_EVENT))
                            }
                        }
                    }
                }
            }
        }
        centrePane.children += tabPane.apply {
            tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS
        }
        centrePane.children += noTabsLabel.apply {
            setOnDragOver { evt ->
                if (evt.gestureSource != noTabsLabel && evt.dragboard.hasFiles()) {
                    evt.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                }
                evt.consume()
            }
            setOnDragDropped { evt ->
                val db = evt.dragboard
                var success = false
                
                if (db.hasFiles()) {
                    // Find first file that matches
                    val firstThatMatches = db.files.firstOrNull { it.extension == "brcad" || it.extension == "bccad" }
                    if (firstThatMatches != null) {
                        Platform.runLater {
                            handleDataFileChoosing(firstThatMatches)
                        }
                        success = true
                    }
                }
                
                evt.isDropCompleted = success
                evt.consume()
            }
        }
        val noTabsLabelVisible = Bindings.isEmpty(tabPane.tabs)
        noTabsLabel.visibleProperty().bind(noTabsLabelVisible)
        noTabsLabel.managedProperty().bind(noTabsLabelVisible)
        
        toolbar.menus += Menu("File").apply {
            items += MenuItem("Open...").apply {
                accelerator = openKeyCombo
                setOnAction {
                    val fc = FileChooser().apply {
                        title = "Choose a data file"
                        extensionFilters.add(FileChooser.ExtensionFilter("BRCAD and BCCAD files", "*.brcad", "*.bccad"))
                        initialDirectory = File(app.settings.dataFileDirectory.value).takeIf { it.exists() && it.isDirectory } ?: File(app.settings.defaultDataFileDirectory)
                    }
                    
                    val file = fc.showOpenDialog(null)
                    if (file != null) {
                        if (handleDataFileChoosing(file)) {
                            app.settings.dataFileDirectory.value = file.parentFile.absolutePath
                            app.settings.persistToStorage()
                        }
                    }
                }
            }
            items += MenuItem("Save").apply {
                accelerator = KeyCombination.keyCombination("Shortcut+S")
                disableProperty().bind(Bindings.createBooleanBinding(Callable { tabPane.selectionModel.selectedItem !is EditorTab<*> }, tabPane.selectionModel.selectedItemProperty()))
                setOnAction {
                    if (tabPane.tabs.isNotEmpty()) {
                        val currentTab = tabPane.selectionModel.selectedItem
                        if (currentTab is EditorTab<*>) {
                            openSaveFileChooserAndSave(currentTab)
                        }
                    }
                }
            }
        }
        toolbar.menus += Menu("View").apply {
            items += CheckMenuItem("Dark Mode").apply {
                //                accelerator = KeyCombination.keyCombination("Shortcut+Alt+D")
                selectedProperty().bindBidirectional(app.settings.nightModeProperty)
            }
            items += SeparatorMenuItem()
            items += MenuItem("Close Current Tab").apply {
                accelerator = KeyCombination.keyCombination("Shortcut+F4")
                disableProperty().bind(Bindings.isEmpty(tabPane.tabs))
                setOnAction {
                    attemptCloseCurrentTab()
                }
            }
        }
        toolbar.menus += Menu("About").apply {
            items += MenuItem("Report a bug/problem").apply {
                setOnAction {
                    app.hostServices.showDocument("${Bread.GITHUB}/issues/new")
                }
            }
            items += MenuItem("Check for updates").apply {
                setOnAction {
                    app.hostServices.showDocument("${Bread.GITHUB}/releases")
                }
            }
            items += SeparatorMenuItem()
            items += MenuItem("About the program").apply {
                //                accelerator = KeyCombination.keyCombination("Shortcut+Alt+A")
                setOnAction {
                    val aboutTab = tabPane.tabs.find { it is AboutTab } ?: AboutTab(app).also {
                        tabPane.tabs += it
                    }
                    tabPane.selectionModel.select(aboutTab)
                }
            }
            items += MenuItem("Tips and Tricks").apply {
                setOnAction {
                    val tipsTab = tabPane.tabs.find { it is TipsTab } ?: TipsTab(app).also {
                        tabPane.tabs += it
                    }
                    tabPane.selectionModel.select(tipsTab)
                }
            }
        }
        
        tabPane.side = Side.TOP
    }
    
    fun openSaveFileChooserAndSave(editorTab: EditorTab<*>): File? {
        val dataFile = editorTab.editor.dataFile
        val fc = FileChooser().apply {
            title = "Choose a save location"
            extensionFilters.add(FileChooser.ExtensionFilter(dataFile.extension.toUpperCase(), "*.${dataFile.extension}"))
            initialDirectory = dataFile.parentFile
            initialFileName = dataFile.name
        }
        val file = fc.showSaveDialog(null)
        if (file != null) {
            editorTab.editor.saveData(file)
        }
        
        return file
    }
    
    fun handleDataFileChoosing(file: File): Boolean {
        when (file.extension) {
            "brcad" -> {
                val dir = file.parentFile
                val filesInDir = dir.listFiles()?.toList() ?: error("Data file's parent is not a directory")
                
                // Attempt to find the texture file. If there is only one PNG file then suggest to use it. Otherwise, ask where the texture file is.
                val pngFiles = filesInDir.filter { it.extension == "png" }
                var textureFile: File? = null
                if (pngFiles.size == 1) {
                    // Suggest the file
                    val suggested = pngFiles.first()
                    val buttonTypeUse = ButtonType("Yes, use this file", ButtonBar.ButtonData.YES)
                    val buttonTypePickAnother = ButtonType("No, pick a different one")
                    val buttonTypeCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        app.addBaseStyleToDialog(this.dialogPane)
                        title = "Use suggested file?"
                        headerText = "Use the suggested texture file?"
                        contentText = "We found a texture file (.png) in this directory,\n${suggested.name}.\nDo you want to select it?"
                        
                        buttonTypes.setAll(buttonTypeUse, buttonTypePickAnother, buttonTypeCancel)
                        dialogPane.buttonTypes.map(dialogPane::lookupButton).forEach { b ->
                            ButtonBar.setButtonUniformSize(b, false)
                        }
                    }
                    val alertResult: Optional<ButtonType> = alert.showAndWait()
                    when (alertResult.get()) {
                        buttonTypeUse -> {
                            textureFile = suggested
                        }
                        buttonTypePickAnother -> {
                            // Do nothing, continue to file chooser
                        }
                        else -> return false // Cancel
                    }
                }
                if (textureFile == null) {
                    // Ask
                    val fc = FileChooser().apply {
                        title = "Select the associated texture file"
                        extensionFilters.add(FileChooser.ExtensionFilter("PNG", "*.png"))
                        initialDirectory = dir
                    }
                    val f = fc.showOpenDialog(null)
                    if (f != null) {
                        textureFile = f
                    } else return false
                }
                
                // Attempt to find the C header file. If there is only one .h file then suggest to use it. Otherwise, ask where the header file is.
                val headerFiles = filesInDir.filter { it.extension == "h" }
                var headerFile: File? = null
                if (headerFiles.size == 1) {
                    // Suggest the file
                    val suggested = headerFiles.first()
                    val buttonTypeUse = ButtonType("Yes, use this file", ButtonBar.ButtonData.YES)
                    val buttonTypePickAnother = ButtonType("No, pick a different one")
                    val buttonTypeCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        app.addBaseStyleToDialog(this.dialogPane)
                        title = "Use suggested file?"
                        headerText = "Use the suggested header file?"
                        contentText = "We found a header file (.h) in this directory,\n${suggested.name}.\nDo you want to select it?"
                        
                        buttonTypes.setAll(buttonTypeUse, buttonTypePickAnother, buttonTypeCancel)
                        dialogPane.buttonTypes.map(dialogPane::lookupButton).forEach { b ->
                            ButtonBar.setButtonUniformSize(b, false)
                        }
                    }
                    val alertResult: Optional<ButtonType> = alert.showAndWait()
                    when (alertResult.get()) {
                        buttonTypeUse -> {
                            headerFile = suggested
                        }
                        buttonTypePickAnother -> {
                            // Do nothing, continue to file chooser
                        }
                        else -> return false // Cancel
                    }
                }
                if (headerFile == null) {
                    // Ask
                    val fc = FileChooser().apply {
                        title = "Select the associated header file"
                        extensionFilters.add(FileChooser.ExtensionFilter("C header file", "*.h"))
                        initialDirectory = dir
                    }
                    val f = fc.showOpenDialog(null)
                    if (f != null) {
                        headerFile = f
                    } else return false
                }
                
                // Open BRCADEditor tab
                val brcad = BRCAD.read(ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN))
                val rawIm = ImageIO.read(textureFile)
                // Resize the image
                val sheetImg = BufferedImage(brcad.sheetW.toInt(), brcad.sheetH.toInt(), rawIm.type)
                val transform = AffineTransform()
                transform.scale(1.0 * sheetImg.width / rawIm.width, 1.0 * sheetImg.height / rawIm.height)
                val g = sheetImg.createGraphics() as Graphics2D
                g.drawImage(rawIm, transform, null)
                g.dispose()
                
                val editor = BRCADEditor(app, this, file, brcad, sheetImg, headerFile)
                val newTab = EditorTab(file.name, editor)
                tabPane.tabs += newTab
                tabPane.selectionModel.select(newTab)
                
                if (rawIm.width != brcad.sheetW.toInt() || rawIm.height != brcad.sheetH.toInt()) {
                    Alert(Alert.AlertType.INFORMATION).apply {
                        app.addBaseStyleToDialog(dialogPane)
                        title = "Load Information"
                        headerText = null
                        contentText = "The loaded texture file has different dimensions than what is defined in the BRCAD file.\nBRCAD: ${brcad.sheetW} by ${brcad.sheetH}\nTexture: ${rawIm.width} by ${rawIm.height}\nPlease note that in the editor the texture will be visually scaled to fit the BRCAD's dimensions."
                    }.showAndWait()
                }
                return true
            }
            "bccad" -> {
                val dir = file.parentFile
                val filesInDir = dir.listFiles()?.toList() ?: error("Data file's parent is not a directory")
                
                // Attempt to find the texture file. If there is only one PNG file then suggest to use it. Otherwise, ask where the texture file is.
                val pngFiles = filesInDir.filter { it.extension == "png" }
                var textureFile: File? = null
                if (pngFiles.size == 1) {
                    // Suggest the file
                    val suggested = pngFiles.first()
                    val buttonTypeUse = ButtonType("Yes, use this file", ButtonBar.ButtonData.YES)
                    val buttonTypePickAnother = ButtonType("No, pick a different one")
                    val buttonTypeCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        app.addBaseStyleToDialog(this.dialogPane)
                        title = "Use suggested file?"
                        headerText = "Use the suggested texture file?"
                        contentText = "We found a texture file (.png) in this directory,\n${suggested.name}.\nDo you want to select it?"
                        
                        buttonTypes.setAll(buttonTypeUse, buttonTypePickAnother, buttonTypeCancel)
                        dialogPane.buttonTypes.map(dialogPane::lookupButton).forEach { b ->
                            ButtonBar.setButtonUniformSize(b, false)
                        }
                    }
                    val alertResult: Optional<ButtonType> = alert.showAndWait()
                    when (alertResult.get()) {
                        buttonTypeUse -> {
                            textureFile = suggested
                        }
                        buttonTypePickAnother -> {
                            // Do nothing, continue to file chooser
                        }
                        else -> return false // Cancel
                    }
                }
                if (textureFile == null) {
                    // Ask
                    val fc = FileChooser().apply {
                        title = "Select the associated texture file"
                        extensionFilters.add(FileChooser.ExtensionFilter("PNG", "*.png"))
                        initialDirectory = dir
                    }
                    val f = fc.showOpenDialog(null)
                    if (f != null) {
                        textureFile = f
                    } else return false
                }
                
                // Open BCCADEditor tab
                
                val readBytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
                val bccad = BCCAD.read(readBytes)
                
                val rawIm = ImageIO.read(textureFile)
                // Rotate (and resize) the image
                val sheetImg = BufferedImage(/*1024, 1024, */bccad.sheetW.toInt(), bccad.sheetH.toInt(), BufferedImage.TYPE_INT_ARGB)
                val transform = AffineTransform()
                // Note: width and height intentionally swapped in scale call
                transform.rotate(-Math.PI / 2)
                transform.translate(-1.0 * sheetImg.height, 0.0)
                transform.scale(1.0 * sheetImg.width / rawIm.height, 1.0 * sheetImg.height / rawIm.width)
                AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC).filter(rawIm, sheetImg)
                
                val editor = BCCADEditor(app, this, file, bccad, sheetImg)
                val newTab = EditorTab(file.name, editor)
                tabPane.tabs += newTab
                tabPane.selectionModel.select(newTab)
                
                // Width and height are swapped intentionally
                if (rawIm.height != bccad.sheetW.toInt() || rawIm.width != bccad.sheetH.toInt()) {
                    Alert(Alert.AlertType.INFORMATION).apply {
                        app.addBaseStyleToDialog(dialogPane)
                        title = "Load Information"
                        headerText = null
                        contentText = "The loaded texture file has different dimensions than what is defined in the BCCAD file.\nBCCAD: ${bccad.sheetW} by ${bccad.sheetH}\nTexture (after being rotated): ${rawIm.height} by ${rawIm.width}\nPlease note that in the editor the texture will be visually scaled to fit the BCCAD's dimensions."
                    }.showAndWait()
                }
                return true
            }
            else -> return false
        }
    }
    
    inner class EditorTab<E : Editor<*>>(title: String, val editor: E) : Tab(title, editor) {
        
        init {
            setOnCloseRequest { evt ->
                Alert(Alert.AlertType.CONFIRMATION).apply {
                    this.title = "Tab Close Confirmation"
                    this.headerText = "Tab Close Confirmation"
                    this.contentText = "Would you like to save your changes before closing this editor tab?"
                    
                    val buttonSave = ButtonType("Save", ButtonBar.ButtonData.YES)
                    val buttonDontSave = ButtonType("Don't Save")
                    val buttonCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    this.buttonTypes.setAll(buttonSave, buttonDontSave, buttonCancel)
                    app.addBaseStyleToAlert(this)
                    
                    dialogPane.buttonTypes.map(dialogPane::lookupButton).forEach { b ->
                        ButtonBar.setButtonUniformSize(b, false)
                    }
                    
                    when (this.showAndWait().orElse(buttonCancel)) {
                        buttonSave -> {
                            val file = openSaveFileChooserAndSave(this@EditorTab)
                            if (file == null) {
                                // The operation was cancelled
                                evt.consume()
                            }
                        }
                        buttonDontSave -> {
                            // Do nothing, let the event propagate
                        }
                        else -> {
                            // Cancel
                            evt.consume()
                        }
                    }
                }
            }
            
            contextMenu = editor.contextMenu.apply {
                items.add(0, MenuItem("Close").apply {
                    accelerator = KeyCombination.keyCombination("Shortcut+F4")
                    setOnAction {
                        val tabPane = this@MainPane.tabPane
                        val tab = this@EditorTab
                        val reqEvt = Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT)
                        Event.fireEvent(tab, reqEvt)
                        if (!reqEvt.isConsumed) {
                            tabPane.tabs.remove(tab)
                            if (tab.onClosed != null) {
                                Event.fireEvent(tab, Event(Tab.CLOSED_EVENT))
                            }
                        }
                    }
                })
                if (items.size > 1) {
                    items.add(1, SeparatorMenuItem())
                }
            }
        }
        
    }
    
}