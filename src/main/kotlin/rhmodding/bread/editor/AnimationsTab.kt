package rhmodding.bread.editor

import javafx.scene.control.Tab
import rhmodding.bread.model.IDataModel


open class AnimationsTab<F : IDataModel>(val editor: Editor<F>) : Tab("Animations") {

    protected val data: F get() = editor.data

    init {
    }

}
