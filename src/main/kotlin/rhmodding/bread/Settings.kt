package rhmodding.bread

import javafx.beans.property.SimpleBooleanProperty
import rhmodding.bread.util.JsonHandler
import java.io.File

class Settings(val app: Bread) {

    val prefsFolder: File = Bread.rootFolder.resolve("prefs/").apply {
        mkdirs()
    }
    val prefsFile: File = prefsFolder.resolve("prefs.json")

    val nightModeProperty = SimpleBooleanProperty(false)
    var nightMode: Boolean
        get() = nightModeProperty.value
        set(value) = nightModeProperty.set(value)
//    val richPresenceProperty = SimpleBooleanProperty(true)
//    var richPresence: Boolean
//        get() = richPresenceProperty.value
//        set(value) = richPresenceProperty.set(value)

    init {
//        richPresenceProperty.addListener { _, _, newValue ->
//            DiscordHelper.enabled = newValue
//        }
    }

    fun loadFromStorage() {
        if (!prefsFile.exists()) return
        try {
            val obj = JsonHandler.OBJECT_MAPPER.readTree(prefsFile)

            nightMode = obj["nightMode"]?.asBoolean(false) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun persistToStorage() {
        prefsFile.createNewFile()
        val json = JsonHandler.OBJECT_MAPPER.createObjectNode()

        json.put("nightMode", nightMode)

        prefsFile.writeText(JsonHandler.OBJECT_MAPPER.writeValueAsString(json))
    }

}