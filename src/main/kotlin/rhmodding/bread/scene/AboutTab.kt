package rhmodding.bread.scene

import javafx.geometry.Pos
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import rhmodding.bread.Bread
import rhmodding.bread.util.BreadIcon
import rhmodding.bread.util.Credits
import rhmodding.bread.util.LibrariesUsed
import rhmodding.bread.util.em


class AboutTab(val app: Bread) : Tab("About") {
    
    init {
        content = BorderPane().apply {
            stylesheets += "style/about.css"
            
            val gridPane = GridPane()
            gridPane.alignment = Pos.CENTER
            gridPane.hgap = 1.0.em
            gridPane.vgap = 0.2.em
            
            gridPane.add(ImageView(BreadIcon.icon128), 0, 0)
            gridPane.add(VBox().apply {
                alignment = Pos.CENTER_LEFT
                children += Label("Bread").apply {
                    styleClass += "title"
                }
                children += Label("A BRCAD and BCCAD format editor").apply {
                    styleClass += "subtitle"
                }
            }, 1, 0)
            var row = 1
            gridPane.add(Label(Bread.VERSION.toString()).apply {
                styleClass += "version"
            }, 1, row++)
            // TODO add github link + license info
//            gridPane.add(Hyperlink(Bread.GITHUB).apply {
//                setOnAction {
//                    app.hostServices.showDocument(Bread.GITHUB)
//                }
//            }, 1, row++)
//            gridPane.add(TextFlow().apply {
//                children += Label("Licensed under ")
//                children += Hyperlink(Bread.LICENSE_NAME).apply {
//                    padding = Insets(0.0)
//                    setOnAction { _ ->
//                        app.hostServices.showDocument("${Bread.GITHUB}/blob/master/LICENSE")
//                    }
//                }
//            }, 1, row++)
            row++
            gridPane.add(Label("Open-Source Software used: ").apply { styleClass += "oss" }, 1, row++)
            val librariesGridPane = GridPane().apply {
                id = "libraries-gp"
            }
            LibrariesUsed.libraries.forEachIndexed { i, lib ->
                librariesGridPane.add(Hyperlink(lib.name).apply {
                    this.setOnAction {
                        app.hostServices.showDocument(lib.website)
                    }
                }, i % 2, i / 2)
            }
            gridPane.add(librariesGridPane, 1, row++, 2, 1)
            val creditsGridPane = GridPane().apply {
                id = "credits-gp"
            }
            Credits.generateList().forEachIndexed { index, credit ->
                creditsGridPane.add(if (credit.url != null) {
                    Hyperlink(credit.person).apply {
//                        styleClass += "oss"
                        this.setOnAction {
                            app.hostServices.showDocument(credit.url)
                        }
                    }
                } else Label(credit.person).apply {
//                    styleClass += "oss"
                }, index % 2, index / 2)
            }
            gridPane.add(Label("Credits").apply { styleClass += "oss" }, 1, row++)
            gridPane.add(creditsGridPane, 1, row++, 2, 1)
            
            center = gridPane
        }
    }
    
}