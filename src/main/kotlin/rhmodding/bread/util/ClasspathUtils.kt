package rhmodding.bread.util


import rhmodding.bread.Bread
import java.io.InputStream
import java.net.URL


fun classpathResource(path: String): URL = Bread::class.java.classLoader.getResource(path) ?: error("Could not find classpath resource $path")

fun classpathResourceStream(path: String): InputStream = Bread::class.java.classLoader.getResourceAsStream(path) ?: error("Could not find classpath resource $path")