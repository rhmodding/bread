package rhmodding.bread

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.ButtonType
import javafx.scene.control.DialogPane
import javafx.scene.image.Image
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhmodding.bread.scene.TestPane
import rhmodding.bread.util.ExceptionAlert
import rhmodding.bread.util.Version
import rhmodding.bread.util.addDebugAccelerators
import rhmodding.bread.util.setMinimumBoundsToSized
import java.io.File
import java.util.*
import kotlin.system.exitProcess


class Bread : Application() {

    companion object {
        const val TITLE: String = "Bread"
        val LOGGER: Logger = LogManager.getContext(Bread::class.java.classLoader, false).getLogger("Bread")
        val VERSION: Version = Version(0, 1, 0, "DEVELOPMENT")
        val rootFolder: File = File(System.getProperty("user.home")).resolve(".bread/").apply { mkdirs() }
        val windowIcons: List<Image> by lazy { listOf(Image("icon/16.png"), Image("icon/32.png"), Image("icon/48.png")) }

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

        val scene = Scene(TestPane(), 800.0, 600.0).apply {
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

    override fun stop() {
        super.stop()
        settings.persistToStorage()
        exitProcess(0)
    }

}