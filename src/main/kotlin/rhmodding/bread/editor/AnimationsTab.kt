package rhmodding.bread.editor

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.layout.VBox
import rhmodding.bread.model.IDataModel


open class AnimationsTab<F : IDataModel>(val editor: Editor<F>) : Tab("Animations") {

    protected val data: F get() = editor.data

    init {
        content = VBox().apply {
            styleClass += "vbox"
            alignment = Pos.CENTER
            children += Label("Animation editing not yet supported, sorry")
        }
    }

}
