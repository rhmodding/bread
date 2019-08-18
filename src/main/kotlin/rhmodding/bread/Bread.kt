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
        val VERSION: Version = Version(1, 0, 3, "DEVELOPMENT")
        val rootFolder: File = File(System.getProperty("user.home")).resolve(".bread/").apply { mkdirs() }
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