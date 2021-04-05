// Copyright 2019-2021 rhmodding project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// '-----------------------------------------------------------------'
//                              ||
//                         _____YY_____
//                       .'@@@@@@@@@@@@'.
//                      ///     ||     \\\
//                     ///      ||      \\\
//                     ||  ___  ||  _O_  ||
//           .-_-.     || |   | || || ||     .-_-.
//         .'d(x)b'.   |A'._Y_|_||_|_Y_.'A|   .'d(x)b'.
//         |(x)O(x)|---|@@@@@@@@@@@@@@@@@@|---|(x)O(x)|
//         |(x)O(x)|===|@@@@@@@@xxx@@@@@@@|===|(x)O(x)|
//         '.g(x)P.'   '|g@@@@@xx%xx@@@@p|'   '.g(x)P.'
//           '---'       '.g@@@@xxx@@@@p'       '---'
//                      ==='.g@@@@@@@p.'===
//                     //     \X_o_X/     \\
//                    (_)                 (_)

package rhmodding.bread

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.DialogPane
import javafx.scene.image.Image
import javafx.stage.Stage
import rhmodding.bread.scene.MainPane
import rhmodding.bread.util.*
import java.io.File
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess


class Bread : Application() {
    
    companion object {
        const val TITLE: String = "Bread"
        val LOGGER: Logger = Logger.getLogger("Bread").apply {
            level = Level.FINE
        }
        const val GITHUB: String = "https://github.com/rhmodding/bread"
        const val LICENSE_NAME: String = "Apache License 2.0"
        val VERSION: Version = Version(1, 3, 0, "")
        val rootFolder: File = File(System.getProperty("user.home")).resolve(".rhmodding/bread/").apply { mkdirs() }
        val windowIcons: List<Image> by lazy { listOf(BreadIcon.icon32, BreadIcon.icon64) }
        
        @JvmStatic
        fun main(args: Array<String>) {
            LOGGER.info("Launching $TITLE $VERSION...")
            Application.launch(Bread::class.java, *args)
        }
    }
    
    val settings: Settings = Settings(this)
    
    lateinit var primaryStage: Stage
        private set
    
    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        primaryStage.title = "$TITLE $VERSION"
        primaryStage.icons.addAll(windowIcons)
        
        val scene = Scene(MainPane(this), 1000.0, 720.0).apply {
            addDebugAccelerators()
            addBaseStyleToScene(this)
        }
        primaryStage.scene = scene
        primaryStage.setMinimumBoundsToSized()
        primaryStage.show()
        
        settings.loadFromStorage()
        
        Thread.currentThread().setUncaughtExceptionHandler { t, e ->
            e.printStackTrace()
            Platform.runLater {
                val exitButton = ButtonType("Exit Program")
                val buttonType: Optional<ButtonType> = ExceptionAlert(null, e, "An uncaught exception occurred in thread ${t.name}\n${e::class.java.simpleName}", "An uncaught exception occurred").apply {
                    this.buttonTypes += exitButton
                }.showAndWait()
                if (buttonType.isPresent) {
                    if (buttonType.get() == exitButton) {
                        exitProcess(0)
                    }
                }
            }
        }
    }
    
    /**
     * Adds the base style + night mode listener
     */
    fun addBaseStyleToScene(scene: Scene) {
        (scene.window as? Stage?)?.icons?.setAll(windowIcons)
        scene.stylesheets += "style/main.css"
        val nightStyle = "style/nightMode.css"
        settings.nightModeProperty.addListener { _, _, newValue ->
            if (newValue) {
                if (nightStyle !in scene.stylesheets) scene.stylesheets += nightStyle
            } else {
                scene.stylesheets -= nightStyle
            }
        }
        if (settings.nightMode) scene.stylesheets += nightStyle
    }
    
    fun addBaseStyleToDialog(dialogPane: DialogPane) {
        (dialogPane.scene.window as? Stage?)?.icons?.setAll(windowIcons)
        dialogPane.stylesheets += "style/main.css"
        val nightStyle = "style/nightMode.css"
        settings.nightModeProperty.addListener { _, _, newValue ->
            if (newValue) {
                if (nightStyle !in dialogPane.stylesheets) dialogPane.stylesheets += nightStyle
            } else {
                dialogPane.stylesheets -= nightStyle
            }
        }
        if (settings.nightMode) dialogPane.stylesheets += nightStyle
    }
    
    fun addBaseStyleToAlert(alert: Alert) {
        addBaseStyleToDialog(alert.dialogPane)
    }
    
    override fun stop() {
        super.stop()
        settings.persistToStorage()
        exitProcess(0)
    }
    
}