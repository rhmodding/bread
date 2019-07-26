package rhmodding.bread.editor

import rhmodding.bread.Bread
import rhmodding.bread.model.brcad.BRCAD
import java.awt.image.BufferedImage
import java.io.File


@ExperimentalUnsignedTypes
class BRCADEditor(app: Bread, dataFile: File, data: BRCAD, image: BufferedImage, val headerFile: File /* TODO actually use this file */)
    : Editor<BRCAD>(app, dataFile, data, image) {
    
    override fun saveData(file: File) {
        file.writeBytes(data.toBytes().array())
    }
}