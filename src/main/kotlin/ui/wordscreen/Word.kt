package ui.wordscreen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import tts.AzureTTS
import data.Word
import player.AudioButton
import state.GlobalState
import state.getResourcesFile
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.sound.sampled.*
import kotlin.concurrent.schedule

/** Kelime bileşeni
 * @param word Kelime
 * @param global Genel durum
 * @param wordVisible Kelime görünürlüğü
 * @param pronunciation Kelime telaffuzu
 * @param playTimes Kelime çalma sayısı
 * @param isPlaying Kelime telaffuzu çalınıyor mu
 * @param setIsPlaying Kelime telaffuzu çalma durumunu ayarla
 * @param isDictation Dikte modu mu
 * @param correctTime Kelimenin doğru sayısı
 * @param wrongTime Kelimenin yanlış sayısı
 * @param textFieldValue Kullanıcının girdiği dize
 * @param typingResult Kullanıcının girdiği karakterlerin sonucu
 * @param checkTyping Kullanıcının girdisinin doğru olup olmadığını kontrol etme geri çağrısı
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun Word(
    word: Word,
    global: GlobalState,
    isDictation:Boolean,
    wordVisible:Boolean,
    pronunciation: String,
    azureTTS: AzureTTS,
    playTimes: Int,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    fontFamily: FontFamily,
    audioSet:Set<String>,
    addToAudioSet:(String) -> Unit,
    correctTime: Int,
    wrongTime: Int,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (String) -> Unit,
    focusRequester: FocusRequester,
    textFieldKeyEvent: (KeyEvent) -> Boolean,
    showMenu: () -> Unit,
) {


    val wordValue = word.value
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 48.dp).height(IntrinsicSize.Max)
        ) {
            var textHeight by remember { mutableStateOf(0.dp) }
            val bottom = computeBottom(
               textStyle =  global.wordTextStyle,
                textHeight = textHeight,
            )
            val smallStyleList = listOf("H5","H6","Subtitle1","Subtitle2","Body1","Body2","Button","Caption","Overline")
            Box(Modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .height(intrinsicSize = IntrinsicSize.Max)
                .padding(start = 50.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    if (!isDictation) {
                        showMenu()
                    }
                }) {
                val fontSize = global.wordFontSize
                DisableTextMenuAndClipboardProvider{
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            checkTyping(input)
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = TextStyle(
                            color = Color.Transparent,
                            fontSize = fontSize,
                            letterSpacing =  global.letterSpacing,
                            fontFamily =fontFamily
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottom)
                            .align(Alignment.Center)
                            .focusRequester(focusRequester)
                            .onKeyEvent { textFieldKeyEvent(it) }
                    )
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Text(
                    modifier = Modifier
                        .testTag("Word")
                        .padding(bottom = bottom)
                        .align(Alignment.Center)
                        .onGloballyPositioned { layoutCoordinates ->
                        textHeight = (layoutCoordinates.size.height).dp
                    },
                    text = buildAnnotatedString {
                        typingResult.forEach { (char, correct) ->
                            if (correct) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.primary,
                                        fontSize = fontSize,
                                        letterSpacing = global.letterSpacing,
                                        fontFamily = fontFamily,
                                    )
                                ) {
                                    append(char)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.Red,
                                        fontSize =fontSize,
                                        letterSpacing = global.letterSpacing,
                                        fontFamily =fontFamily,
                                    )
                                ) {
                                    if (char == ' ') {
                                        append("_")
                                    } else {
                                        append(char)
                                    }
                                }
                            }
                        }
                        val remainChars = wordValue.substring(typingResult.size)
                        if (isDictation) {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.onBackground,
                                    fontSize = fontSize,
                                    letterSpacing =  global.letterSpacing,
                                    fontFamily = fontFamily,
                                )
                            ) {
                                repeat(remainChars.length) {
                                    append(" ")
                                }

                            }
                        } else {
                            if (wordVisible) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = fontSize,
                                        letterSpacing =  global.letterSpacing,
                                        fontFamily =fontFamily,
                                    )
                                ) {
                                    append(remainChars)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = fontSize,
                                        letterSpacing =  global.letterSpacing,
                                        fontFamily = fontFamily,
                                    )
                                ) {
                                    repeat(remainChars.length) {
                                        append("_")
                                    }
                                }

                            }

                        }
                    }
                )
            }


            Column {
                val top = (textHeight - 36.dp).div(2)
                var numberFontSize = LocalTextStyle.current.fontSize
                if(smallStyleList.contains(global.wordTextStyle)) numberFontSize = MaterialTheme.typography.overline.fontSize
                Spacer(modifier = Modifier.height(top))
                Text(text = "${if (correctTime > 0) correctTime else ""}",
                    color = MaterialTheme.colors.primary,
                    fontSize =  numberFontSize)
                Spacer(modifier = Modifier.height(top))
                Text(text = "${if (wrongTime > 0) wrongTime else ""}",
                    color = Color.Red,
                    fontSize =  numberFontSize
                )
            }
            var paddingTop = textHeight.div(2) - 20.dp
            if(paddingTop<0.dp) paddingTop =  0.dp
            if(global.wordTextStyle == "H1") paddingTop = 23.dp

            AudioButton(
                audioSet = audioSet,
                addToAudioSet = addToAudioSet,
                word = wordValue,
                volume = global.audioVolume,
                isPlaying = isPlaying,
                setIsPlaying = setIsPlaying,
                pronunciation = pronunciation,
                azureTTS = azureTTS,
                playTimes = playTimes,
                paddingTop = paddingTop,
            )
        }
}

/**
 * Fonetik bileşeni
 */
@Composable
fun Phonetic(
    word: Word,
    phoneticVisible: Boolean,
    fontSize: TextUnit
) {
    if (phoneticVisible) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (word.usphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "ABD:${word.usphone}",
                        fontSize = fontSize,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            if (word.ukphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "İng:${word.ukphone}",
                        fontSize = fontSize,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }

        }
    }
}

/**
 * Metin menüsünü ve panoyu devre dışı bırak
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DisableTextMenuAndClipboardProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextContextMenu provides object : TextContextMenu {
            @Composable
            override fun Area(
                textManager: TextContextMenu.TextManager,
                state: ContextMenuState,
                content: @Composable () -> Unit
            )  {

                val items = {listOf<ContextMenuItem>()}
                ContextMenuArea(items, state, content = content)
            }
        },
        LocalClipboardManager provides object :  ClipboardManager {
            override fun getText(): AnnotatedString {
                return AnnotatedString("")
            }

            override fun setText(text: AnnotatedString) {}
        },
        content = content
    )
}

/**
 * Özel metin menüsü ve pano, metin menüsü yalnızca kopyalamayı korur
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTextMenuProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextContextMenu provides object : TextContextMenu {
            @Composable
            override fun Area(
                textManager: TextContextMenu.TextManager,
                state: ContextMenuState,
                content: @Composable () -> Unit
            )  {
                val localization = LocalLocalization.current
                val items = {
                    listOfNotNull(
                        textManager.copy?.let {
                            ContextMenuItem(localization.copy, it)
                        }
                    )
                }

                ContextMenuArea(items, state, content = content)
            }
        },
        LocalClipboardManager provides object :  ClipboardManager {
            // paste
            override fun getText(): AnnotatedString {
                return AnnotatedString("")
            }
            // copy
            override fun setText(text: AnnotatedString) {
                 Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text.text), null)
            }
        },
        content = content
    )
}

/**
 * Ses efektini çal
 * @param path Yol
 * @param volume Ses seviyesi
 */
fun playSound(path: String, volume: Float) {
    try {
        val file = getResourcesFile(path)
        AudioSystem.getAudioInputStream(file).use { audioStream ->
            val format = audioStream.format
            val info: DataLine.Info = DataLine.Info(Clip::class.java, format)
            val clip: Clip = AudioSystem.getLine(info) as Clip
            clip.addLineListener{event ->
                if (event.type == LineEvent.Type.STOP) {
                    Timer("clip close", false).schedule(500) {
                        clip.close()
                    }
                }
            }
            clip.open(audioStream)
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val range = gainControl.maximum - gainControl.minimum
            val value = (range * volume) + gainControl.minimum
            gainControl.value = value
            clip.start()
            clip.drain()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/** Kelimenin alt iç boşluğunu hesapla */
fun computeBottom(
    textStyle:String,
    textHeight:Dp,
): Dp {
    var bottom = 0.dp
    val smallStyleList = listOf("H5","H6","Subtitle1","Subtitle2","Body1","Body2","Button","Caption","Overline")
    if(smallStyleList.contains(textStyle)) bottom = (36.dp - textHeight).div(2)
    if(bottom<0.dp) bottom = 0.dp
    if(bottom>7.5.dp) bottom = 5.dp
    return bottom
}