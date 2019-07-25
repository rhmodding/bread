package rhmodding.bread.scene

import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import rhmodding.bread.Bread
import rhmodding.bread.brcad.BRCAD
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


@ExperimentalUnsignedTypes
class TestPane : BorderPane() {
    init {
        center = Button("Open BRCAD").apply {
            setOnAction {
                val file = FileChooser().apply {
                    title = "Choose BRCAD"
                    extensionFilters.add(FileChooser.ExtensionFilter("BRCAD", "*.brcad"))
                    initialDirectory = File(System.getProperty("user.home")).resolve("Desktop/")
                }.showOpenDialog(null)
                if (file != null) {
                    val readBytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)
                    val brcad = BRCAD.read(readBytes)
                    Bread.LOGGER.debug(brcad.toString())
                    Bread.LOGGER.debug(brcad.toBytes().equals(readBytes))
                }
            }
        }
    }
}