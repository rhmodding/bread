package rhmodding.bread.util

import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.Stage
import rhmodding.bread.Bread
import java.io.PrintWriter
import java.io.StringWriter


class ExceptionAlert(app: Bread?, val exception: Throwable,
                     contentText: String = exception.message ?: "",
                     windowTitle: String = "An exception occurred",
                     headerText: String = windowTitle,
                     textBoxLabel: Label = Label("Exception stacktrace:"))
    : Alert(AlertType.ERROR) {

    init {
        this.title = windowTitle
        this.contentText = contentText
        this.headerText = headerText
        this.addWindowIcons()

        this.dialogPane?.let {
            app?.addBaseStyleToDialog(it)
        }

        val exceptionText = StringWriter().let {
            exception.printStackTrace(PrintWriter(it))
            it.toString()
        }
        val textArea = TextArea(exceptionText).apply {
            isEditable = false
            isWrapText = false
            maxWidth = Double.MAX_VALUE
            maxHeight = Double.MAX_VALUE
            GridPane.setVgrow(this, Priority.ALWAYS)
            GridPane.setHgrow(this, Priority.ALWAYS)
        }
        val gridPane = GridPane().apply {
            maxWidth = Double.MAX_VALUE
            add(textBoxLabel, 0, 0)
            add(textArea, 0, 1)
        }

        this.dialogPane.expandableContent = gridPane
    }

}

fun Alert.addWindowIcons(): Alert = this.apply {
    (dialogPane.scene.window as? Stage?)?.icons?.addAll(Bread.windowIcons)
}