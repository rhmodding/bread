package rhmodding.bread.util

object LibrariesUsed {
    data class Library(val name: String, val website: String)

    val libraries: List<Library> = listOf(
        Library("Kotlin", "https://kotlinlang.org/"),
        Library("bccad-editor", "https://github.com/rhmodding/bccad-editor"),
        Library("RHRE SFX Database Editor", "https://github.com/chrislo27/RSDE"),
        Library("javafx-themes", "https://github.com/joffrey-bion/javafx-themes")
    ).sortedBy { it.name }
}