package rhmodding.bread.editor

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import rhmodding.bread.Bread
import rhmodding.bread.model.IAnimation
import rhmodding.bread.model.IAnimationStep
import rhmodding.bread.model.ISprite
import rhmodding.bread.model.ISpritePart
import rhmodding.bread.model.brcad.*
import rhmodding.bread.scene.MainPane
import rhmodding.bread.util.ExceptionAlert
import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.Charset
import java.util.logging.Level


class BRCADEditor(app: Bread, mainPane: MainPane, dataFile: File, data: BRCAD, image: BufferedImage, textureFile: File,
                  val headerFile: File)
    : Editor<BRCAD>(app, mainPane, dataFile, data, image, textureFile) {
    
    data class HeaderDefine(val string: String, val number: Int, val comment: String)
    
    class BRCADSpritesTab(editor: BRCADEditor) : SpritesTab<BRCAD>(editor) {
        
        init {
            body.children.add(0, TitledPane("BRCAD Information", GridPane().apply {
                styleClass += "grid-pane"
                alignment = Pos.CENTER_LEFT
                
                add(Label("Spritesheet Num.: "), 0, 0)
                add(Label("${data.spritesheetNumber}").apply {
                    styleClass += "spritesheet-number"
                }, 1, 0)
            }).apply {
                styleClass += "titled-pane"
                this.isExpanded = false
            })
        }
        
    }
    
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
    
    override val spritesTab: SpritesTab<BRCAD> = BRCADSpritesTab(this)
    override val animationsTab: AnimationsTab<BRCAD> = BRCADAnimationsTab(this)
    override val advPropsTab: AdvancedPropertiesTab<BRCAD> = AdvancedPropertiesTab(this)
    override val debugTab: DebugTab<BRCAD> = DebugTab(this)
    
    init {
        stylesheets += "style/brcadEditor.css"
        this.applyCss()
        
        contextMenu.items += Menu("Animations").apply {
            data.animations.forEachIndexed { i, _ ->
                val defines = headerDefinitions[i]
                items += MenuItem("($i)${if (defines == null || defines.isEmpty()) "" else ": ${defines.joinToString(separator = ", ") { it.string }}"}").apply {
                    setOnAction {
                        // Select animations tab
                        sidebar.selectionModel.select(animationsTab)
                        animationsTab.animationSpinner.valueFactory.value = i
                    }
                }
            }
        }
    }

    override fun loadTexture(textureFile: File): Triple<BufferedImage, BufferedImage, Boolean> {
        val (rawIm, sheetImg) = mainPane.loadBRCADImage(textureFile, data)
        return Triple(rawIm, sheetImg, rawIm.width != data.sheetW.toInt() || rawIm.height != data.sheetH.toInt())
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
            //TODO: check if there's no other steps in the animation, if so it can't delete it or has to delete the anim
            val index = data.sprites.indexOf(sprite).toUShort()
            for (anim in data.animations) {
                for (step in anim.steps) {
                    if (step.spriteIndex == index) {
                        //TODO: prompt
                        step.spriteIndex = 0.toUShort()
                    } else if (step.spriteIndex > index) {
                        step.spriteIndex = (step.spriteIndex - 1.toUShort()).toUShort()
                    }
                }
            }
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