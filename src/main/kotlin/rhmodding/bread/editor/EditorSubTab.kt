package rhmodding.bread.editor

import javafx.scene.control.Tab
import rhmodding.bread.model.IDataModel


abstract class EditorSubTab<F : IDataModel>(val editor: Editor<F>, val title: String) : Tab(title) {
    protected val data: F get() = editor.data
}