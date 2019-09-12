package rhmodding.bread.editor

import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import rhmodding.bread.model.IDataModel


open class DebugTab<F : IDataModel>(editor: Editor<F>) : EditorSubTab<F>(editor, "Debug") {
    
    val body: VBox = VBox().apply {
        isFillWidth = true
    }
    val infoBox: TextArea
    
    init {
        content = ScrollPane(body).apply {
            this.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            this.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        }
        
        infoBox = TextArea().apply {
            editableProperty().set(false)
            prefWidthProperty().bind(body.prefWidthProperty())
            prefHeightProperty().bind(body.prefHeightProperty())
        }
        body.children += infoBox
        populate()
    }
    
    open fun populate() {
        infoBox.text = "$data"
    }
    
}