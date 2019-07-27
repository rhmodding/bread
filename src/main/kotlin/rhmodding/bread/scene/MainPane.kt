package rhmodding.bread.scene

import javafx.beans.binding.Bindings
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import rhmodding.bread.Bread
import rhmodding.bread.editor.BCCADEditor
import rhmodding.bread.editor.BRCADEditor
import rhmodding.bread.editor.Editor
import rhmodding.bread.model.bccad.BCCAD
import rhmodding.bread.model.brcad.BRCAD
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.imageio.ImageIO


@ExperimentalUnsignedTypes
class MainPane(val app: Bread) : BorderPane() {
    
    val toolbar: MenuBar = MenuBar()
    val centrePane: StackPane = StackPane()
    val tabPane: TabPane = TabPane()
    val bottomPane: VBox = VBox()
    
    val noTabsLabel: Label = Label("Open a file in\nFile > Open...").apply {
        id = "no-tabs-label"
    }
    
    init {
        stylesheets += "style/mainPane.css"
        
        top = toolbar
        center = centrePane
        bottom = bottomPane
        
        centrePane.children += tabPane
        centrePane.children += noTabsLabel
        val noTabsLabelVisible = Bindings.isEmpty(tabPane.tabs)
        noTabsLabel.visibleProperty().bind(noTabsLabelVisible)
        noTabsLabel.managedProperty().bind(noTabsLabelVisible)
        
        toolbar.menus += Menu("File").apply {
            items += MenuItem("Open...").apply {
                accelerator = KeyCombination.keyCombination("Shortcut+O")
                setOnAction {
                    val fc = FileChooser().apply {
                        title = "Choose a data file"
                        extensionFilters.add(FileChooser.ExtensionFilter("BRCAD and BCCAD files", "*.brcad", "*.bccad"))
                        initialDirectory = File(app.settings.dataFileDirectory.value)
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
                setOnAction {
                    if (tabPane.tabs.isNotEmpty()) {
                        val currentTab = tabPane.selectionModel.selectedItem
                        if (currentTab is EditorTab<*>) {
                            val dataFile = currentTab.editor.dataFile
                            val fc = FileChooser().apply {
                                title = "Choose a save location"
                                extensionFilters.add(FileChooser.ExtensionFilter(dataFile.extension.toUpperCase(), "*.${dataFile.extension}"))
                                initialDirectory = dataFile.parentFile
                                initialFileName = dataFile.name
                            }
                            val file = fc.showSaveDialog(null)
                            if (file != null) {
                                currentTab.editor.saveData(file)
                            }
                        }
                    }
                }
            }
        }
        toolbar.menus += Menu("View").apply {
            items += CheckMenuItem("Dark Mode").apply {
                selectedProperty().bindBidirectional(app.settings.nightModeProperty)
            }
        }
        toolbar.menus += Menu("About").apply {
            items += MenuItem("About the program").apply {
                setOnAction {
                    val newTab = AboutTab(app)
                    tabPane.tabs += newTab
                    tabPane.selectionModel.select(newTab)
                }
            }
        }
        
        tabPane.side = Side.TOP
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
                    val buttonTypeUse = ButtonType("Yes, use this file")
                    val buttonTypePickAnother = ButtonType("No, pick a different one")
                    val buttonTypeCancel = ButtonType("Cancel everything", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        app.addBaseStyleToDialog(this.dialogPane)
                        title = "Use suggested file?"
                        headerText = "Use the suggested texture file?"
                        contentText = "We found a texture file (.png) in this directory,\n${suggested.name}.\nDo you want to select it?"
                        
                        buttonTypes.setAll(buttonTypeUse, buttonTypePickAnother, buttonTypeCancel)
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
                    val buttonTypeUse = ButtonType("Yes, use this file")
                    val buttonTypePickAnother = ButtonType("No, pick a different one")
                    val buttonTypeCancel = ButtonType("Cancel everything", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        app.addBaseStyleToDialog(this.dialogPane)
                        title = "Use suggested file?"
                        headerText = "Use the suggested header file?"
                        contentText = "We found a header file (.h) in this directory,\n${suggested.name}.\nDo you want to select it?"
                        
                        buttonTypes.setAll(buttonTypeUse, buttonTypePickAnother, buttonTypeCancel)
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
                val editor = BRCADEditor(app, file, BRCAD.read(ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)), ImageIO.read(textureFile), headerFile)
                val newTab = EditorTab(file.name, editor)
                tabPane.tabs += newTab
                tabPane.selectionModel.select(newTab)
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
                    val buttonTypeUse = ButtonType("Yes, use this file")
                    val buttonTypePickAnother = ButtonType("No, pick a different one")
                    val buttonTypeCancel = ButtonType("Cancel everything", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        app.addBaseStyleToDialog(this.dialogPane)
                        title = "Use suggested file?"
                        headerText = "Use the suggested texture file?"
                        contentText = "We found a texture file (.png) in this directory,\n${suggested.name}.\nDo you want to select it?"
                        
                        buttonTypes.setAll(buttonTypeUse, buttonTypePickAnother, buttonTypeCancel)
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
                val rawIm = ImageIO.read(textureFile)
                // Rotate the image
                val sheetImg = BufferedImage(rawIm.height, rawIm.width, rawIm.type)
                val transform = AffineTransform()
                transform.translate(0.5 * rawIm.height, 0.5 * rawIm.width)
                transform.rotate(-Math.PI / 2)
                transform.translate(-0.5 * rawIm.width, -0.5 * rawIm.height)
                val g = sheetImg.createGraphics() as Graphics2D
                g.drawImage(rawIm, transform, null)
                g.dispose()
                
                val editor = BCCADEditor(app, file, BCCAD.read(ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)), sheetImg)
                val newTab = EditorTab(file.name, editor)
                tabPane.tabs += newTab
                tabPane.selectionModel.select(newTab)
                return true
            }
            else -> return false
        }
    }
    
    class EditorTab<E : Editor<*>>(title: String, val editor: E) : Tab(title, editor)
    
}