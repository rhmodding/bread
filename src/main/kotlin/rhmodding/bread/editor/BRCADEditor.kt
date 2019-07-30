package rhmodding.bread.editor

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import rhmodding.bread.Bread
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.model.brcad.*
import rhmodding.bread.util.ExceptionAlert
import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.Charset
import java.util.logging.Level


class BRCADEditor(app: Bread, dataFile: File, data: BRCAD, image: BufferedImage, val headerFile: File)
    : Editor<BRCAD>(app, dataFile, data, image) {
    
    class HeaderDefine(val string: String, val number: Int, val comment: String)
    
    class BRCADAnimationsTab(editor: BRCADEditor) : AnimationsTab<BRCAD>(editor) {
        val animationNameLabels: HBox = HBox().apply {
            styleClass += "hbox"
            styleClass += "define-name-label"
            alignment = Pos.CENTER_LEFT
        }
        
        init {
            sectionAnimation.children.add(1, HBox().apply {
                styleClass += "hbox"
                alignment = Pos.CENTER_LEFT
                children += Label("#define'd as: ").apply {
                    tooltip = Tooltip("Hover over each #define name to see the comment, if any")
                }
                children += animationNameLabels
            })
            Platform.runLater {
                updateFieldsForStep()
            }
        }
        
        override fun updateFieldsForStep() {
            super.updateFieldsForStep()
            // Update name label
            val names = (editor as BRCADEditor).headerDefinitions[animationSpinner.value]?.takeUnless { it.isEmpty() }
            animationNameLabels.children.clear()
            if (names == null) {
                animationNameLabels.children += Label("<no names>").apply {
                    tooltip = Tooltip("This animation has no #define'd names")
                }
            } else {
                names.forEach { name ->
                    animationNameLabels.children += Label(name.string).apply {
                        id = "name-label"
                        tooltip = Tooltip(if (name.comment.isBlank()) "(no commment was found for this name)" else name.comment)
                    }
                }
            }
        }
    }
    
    val headerDefinitions: Map<Int, List<HeaderDefine>> = try {
        val lines = headerFile.readLines(Charset.forName("Shift-JIS")).map { it.trim() }.withIndex()
        // Assuming each line is a #define
        val badLines = mutableListOf<IndexedValue<String>>()
        val regex = """#define(?:[ \t]+)([A-Za-z0-9_]+)(?:[ \t]+)(\d+)(?:[ \t]+)(?://(.*))?""".toRegex()
        val headers = mutableListOf<HeaderDefine>()
        for ((i, s) in lines) {
            if (s.isEmpty()) continue
            val match = regex.matchEntire(s)
            if (match != null) {
                headers += HeaderDefine(match.groupValues[1], match.groupValues[2].toInt(), match.groupValues[3])
            } else {
                if (!s.startsWith("//")) {
                    badLines.add(IndexedValue(i + 1, s))
                    Bread.LOGGER.log(Level.INFO, "Line ${i + 1} failed to be parsed in header file: $s")
                }
            }
        }
        
        if (badLines.isNotEmpty()) {
            Alert(Alert.AlertType.WARNING).apply {
                title = "Errors when parsing header"
                headerText = "Errors when parsing header"
                contentText = "There were errors when parsing the header file. Report this to the developer!\nLines: [${badLines.joinToString(separator = ", ") { it.value }}]"
            }.showAndWait()
        }
        headers.groupBy { it.number }
    } catch (e: Exception) {
        e.printStackTrace()
        ExceptionAlert(app, e).showAndWait()
        mapOf()
    }
    
    override val spritesTab: SpritesTab<BRCAD> = SpritesTab(this)
    override val animationsTab: AnimationsTab<BRCAD> = BRCADAnimationsTab(this)
    override val advPropsTab: AdvancedPropertiesTab<BRCAD> = AdvancedPropertiesTab(this)
    
    init {
        stylesheets += "style/brcadEditor.css"
        this.applyCss()
    }
    
    override fun saveData(file: File) {
        file.writeBytes(data.toBytes().array())
    }
    
    override fun addSprite(sprite: ISprite) {
        if (sprite is Sprite) {
            data.sprites += sprite
        }
    }
    
    override fun removeSprite(sprite: ISprite) {
        if (sprite is Sprite) {
            data.sprites -= sprite
        }
    }
    
    override fun addSpritePart(sprite: ISprite, part: ISpritePart) {
        if (sprite is Sprite && part is SpritePart) {
            sprite.parts += part
        }
    }
    
    override fun removeSpritePart(sprite: ISprite, part: ISpritePart) {
        if (sprite is Sprite && part is SpritePart) {
            sprite.parts -= part
        }
    }
    
    override fun addAnimation(animation: IAnimation) {
        if (animation is Animation) {
            data.animations += animation
        }
    }
    
    override fun removeAnimation(animation: IAnimation) {
        if (animation is Animation) {
            data.animations -= animation
        }
    }
    
    override fun addAnimationStep(animation: IAnimation, animationStep: IAnimationStep) {
        if (animation is Animation && animationStep is AnimationStep) {
            animation.steps += animationStep
        }
    }
    
    override fun removeAnimationStep(animation: IAnimation, animationStep: IAnimationStep) {
        if (animation is Animation && animationStep is AnimationStep) {
            animation.steps -= animationStep
        }
    }
    
    override fun createSprite(): ISprite {
        return Sprite()
    }
    
    override fun createSpritePart(): ISpritePart {
        return SpritePart()
    }
    
    override fun createAnimation(): IAnimation {
        return Animation()
    }
    
    override fun createAnimationStep(): IAnimationStep {
        return AnimationStep()
    }
    
}