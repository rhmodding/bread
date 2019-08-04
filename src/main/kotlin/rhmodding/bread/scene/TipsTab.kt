package rhmodding.bread.scene

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import rhmodding.bread.Bread
import rhmodding.bread.util.em


class TipsTab(val app: Bread) : Tab("Tips & Tricks") {

    init {
        content = BorderPane().apply {
            stylesheets += "style/tips.css"
            padding = Insets(1.5.em, 3.0.em, 1.5.em, 3.0.em)

            top = Label("Tips and Tricks").apply {
                id = "title"
            }
            center = ScrollPane().apply {
                content = VBox().apply {
                    styleClass += "vbox"
                    alignment = Pos.TOP_LEFT
                    
                    children += Label("Stay organized! Put your bccad/brcad files in a folder with their other related files, like the texture .png file.\nThe program can auto-detect these files and save you time when opening them.")
                    children += Label("Pressing the UP or DOWN arrow keys while editing a spinner to change it quickly.\nHold CTRL while doing so to change it by increments of 10.")
                    children += Label("When using the Region Picker in the Sprites tab, find your region area in an image editor first\nso you can set the X, Y, width, and height values.")
                    children += Label("When using the Region Picker in the Sprites tab, right clicking when you've started a selection\nwill cancel it.")
                }
            }
        }
    }

}