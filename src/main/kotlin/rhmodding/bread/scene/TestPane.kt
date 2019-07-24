package rhmodding.bread.scene

import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import rhmodding.bread.Bread
import rhmodding.bread.brcad.BRCAD
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


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
                    Bread.LOGGER.debug(BRCAD.read(ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)).toString())
                }
            }
        }
    }
}