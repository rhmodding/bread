package rhmodding.bread.scene

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.layout.VBox
import rhmodding.bread.Bread


class AboutTab(val app: Bread) : Tab("About") {
    
    init {
        content = VBox().apply {
            styleClass += "vbox"
            alignment = Pos.CENTER
            children += Label("${Bread.TITLE} - A BRCAD editor\n${Bread.VERSION}\n\nDeveloped by chrislo27 and Malalaika\nIcon by garbo111")
            // TODO put Apache license
        }
    }
    
}