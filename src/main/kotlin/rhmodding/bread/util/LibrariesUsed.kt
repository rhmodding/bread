package rhmodding.bread.util

object LibrariesUsed {
    data class Library(val name: String, val website: String)

    val libraries: List<Library> = listOf(
        Library("Kotlin", "https://kotlinlang.org/"),
        Library("bccad-editor", "https://github.com/rhmodding/bccad-editor"),
        Library("RHRE SFX Database Editor", "https://github.com/chrislo27/RSDE"),
        Library("Jackson", "https://github.com/FasterXML/jackson"),
        Library("animated-gif-lib", "https://github.com/rtyley/animated-gif-lib-for-java")
    ).sortedBy { it.name }
}