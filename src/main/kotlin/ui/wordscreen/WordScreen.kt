package ui.wordscreen

import LocalCtrl
import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.*
import ui.wordscreen.MemoryStrategy.*
import tts.AzureTTS
import tts.rememberAzureTTS
import ui.Toolbar
import ui.components.MacOSTitle
import ui.components.RemoveButton
import ui.dialog.*
import util.createTransferHandler
import util.rememberMonospace
import java.awt.Component
import java.awt.Rectangle
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.schedule

/**
 * Uygulamanın çekirdek bileşeni, kelime ezberleme arayüzü
 * @param appState uygulamanın genel durumu
 * @param wordScreenState kelime ezberleme arayüzünün durum kapsayıcısı
 * @param videoBounds video oynatma penceresinin konumu ve boyutu
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class, ExperimentalFoundationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WordScreen(
    window: ComposeWindow,
    title: String,
    appState: AppState,
    wordScreenState: WordScreenState,
    videoBounds: Rectangle,
    resetVideoBounds :() -> Rectangle,
    showPlayer :(Boolean) -> Unit,
    setVideoPath:(String) -> Unit,
    setVideoVocabulary:(String) -> Unit
) {


    //设置窗口的拖放处理函数
    LaunchedEffect(Unit){
        setWindowTransferHandler(
            window = window,
            appState = appState,
            wordScreenState = wordScreenState,
            showVideoPlayer = showPlayer,
            setVideoPath = setVideoPath,
            setVideoVocabulary = setVideoVocabulary
        )
    }

    Box(Modifier.background(MaterialTheme.colors.background)) {
        ->
        /** Kelime giriş kutusu odak isteyicisi*/
        val wordFocusRequester = remember { FocusRequester() }
        /** Mevcut ezberlenen kelime */
        val currentWord = if(wordScreenState.vocabulary.wordList.isNotEmpty()){
            wordScreenState.getCurrentWord()
        }else  null

        val  wordRequestFocus: () -> Unit = {
            if(currentWord != null){
                wordFocusRequester.requestFocus()
            }
        }
        var showFilePicker by remember {mutableStateOf(false)}
        var showBuiltInVocabulary by remember{mutableStateOf(false)}
        var documentWindowVisible by remember { mutableStateOf(false) }
        var generateVocabularyListVisible by remember { mutableStateOf(false) }
        val openChooseVocabulary:(String) ->Unit = { path ->
            val file = File(path)
            val index = appState.findVocabularyIndex(file)
            val changed = appState.changeVocabulary(
                vocabularyFile = file,
                wordScreenState,
                index
            )
            if(changed){
                appState.global.type = ScreenType.WORD
                appState.saveGlobalState()
            }
        }

        Row {
            val dictationState = rememberDictationState()
            val azureTTS = rememberAzureTTS()
            WordScreenSidebar(
                appState = appState,
                wordScreenState = wordScreenState,
                dictationState = dictationState,
                wordRequestFocus = wordRequestFocus,
                resetVideoBounds = resetVideoBounds,
                azureTTS = azureTTS
                )

            Box(Modifier.fillMaxSize()) {
                if (currentWord != null) {
                    MainContent(
                        appState =appState,
                        wordScreenState = wordScreenState,
                        dictationState = dictationState,
                        azureTTS = azureTTS,
                        currentWord = currentWord,
                        videoBounds = videoBounds,
                        resetVideoBounds = resetVideoBounds,
                        wordFocusRequester = wordFocusRequester,
                        window = window
                    )
                } else {
                    VocabularyEmpty(
                        openVocabulary = { showFilePicker = true },
                        openBuiltInVocabulary = {showBuiltInVocabulary = true},
                        generateVocabulary = {generateVocabularyListVisible = true},
                        openDocument = {documentWindowVisible = true},
                        parentWindow = window,
                        futureFileChooser = appState.futureFileChooser,
                        openChooseVocabulary = openChooseVocabulary
                    )
                }

                Header(
                    wordScreenState = wordScreenState,
                    title = title,
                    window = window,
                    wordRequestFocus = wordRequestFocus,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        Row( modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = appState.openSettings,
                setIsOpen = {
                    appState.openSettings = it
                    if(!it && currentWord != null){
                        wordFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier,
                globalState = appState.global,
                saveGlobalState = {appState.saveGlobalState()},
                showPlayer = showPlayer,
                openSearch = appState.openSearch,
            )
            val ctrl = LocalCtrl.current

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Text(text = "Kelime Dağarcığı Dosyasını Aç $ctrl + O", modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 50,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset.Zero
                )
            ) {

                IconButton(
                    onClick = { showFilePicker = true },
                    modifier = Modifier.padding(top = if (isMacOS()) 30.dp else 0.dp)
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            RemoveButton(onClick = {
                wordScreenState.index = 0
                wordScreenState.vocabulary.size = 0
                wordScreenState.vocabulary.name = ""
                wordScreenState.vocabulary.relateVideoPath = ""
                wordScreenState.vocabulary.wordList.clear()
                wordScreenState.vocabularyName = ""
                wordScreenState.vocabularyPath = ""
                wordScreenState.saveWordScreenState()
            }, toolTip = "Mevcut Kelime Dağarcığını Kapat")
            val extensions = if(isMacOS()) listOf("public.json") else listOf("json")

            FilePicker(
                show = showFilePicker,
                fileExtensions = extensions,
                initialDirectory = ""){pfile ->
                if(pfile != null){
                    if(pfile.path.isNotEmpty()){
                        openChooseVocabulary(pfile.path)
                    }
                }

                showFilePicker = false
            }
        }


        BuiltInVocabularyDialog(
            show = showBuiltInVocabulary,
            close = {showBuiltInVocabulary = false},
            openChooseVocabulary = openChooseVocabulary,
            futureFileChooser = appState.futureFileChooser
        )

        GenerateVocabularyListDialog(
            appState = appState,
            show = generateVocabularyListVisible,
            close = {generateVocabularyListVisible = false}
        )

        var currentPage by remember { mutableStateOf("features") }
        if(documentWindowVisible){
            DocumentWindow(
                close = {documentWindowVisible = false},
                currentPage = currentPage,
                setCurrentPage = {currentPage = it}
            )
        }
    }

}


@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun Header(
    wordScreenState: WordScreenState,
    title:String,
    window: ComposeWindow,
    wordRequestFocus: () -> Unit,
    modifier: Modifier
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ){
        // macOS başlık çubuğu Windows'tan farklıdır, özel işlem gerekir
        if (isMacOS()) {
            MacOSTitle(
                title = title,
                window = window,
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center){
            // 记忆单词时的状态信息
            val text = when(wordScreenState.memoryStrategy){
                Normal -> { if(wordScreenState.vocabulary.size>0) "${wordScreenState.index + 1}/${wordScreenState.vocabulary.size}" else ""}
                Dictation -> { "Kelime Dikte Etme   ${wordScreenState.dictationIndex + 1}/${wordScreenState.dictationWords.size}"}
                DictationTest -> {"Dikte Testi   ${wordScreenState.dictationIndex + 1}/${wordScreenState.reviewWords.size}"}
                NormalReviewWrong -> { "Yanlış Kelimeleri Gözden Geçir   ${wordScreenState.dictationIndex + 1}/${wordScreenState.wrongWords.size}"}
                DictationTestReviewWrong -> { "Dikte Testi - Yanlış Kelimeleri Gözden Geçir   ${wordScreenState.dictationIndex + 1}/${wordScreenState.wrongWords.size}"}
            }

            val top = if(wordScreenState.memoryStrategy != Normal) 0.dp else 12.dp
            Text(
                text = text,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .testTag("Header")
                    .padding(top = top )
            )
            if(wordScreenState.memoryStrategy != Normal){
                Spacer(Modifier.width(20.dp))
                val tooltip = when (wordScreenState.memoryStrategy) {
                    DictationTest, DictationTestReviewWrong -> {
                        "Dikte Testinden Çık"
                    }
                    Dictation -> {
                        "Dikte Etmeden Çık"
                    }
                    else -> {
                        "Gözden Geçirmeden Çık"
                    }
                }
                ExitButton(
                    tooltip = tooltip,
                    onClick = {
                    wordScreenState.showInfo()
                    wordScreenState.clearInputtedState()
                    wordScreenState.memoryStrategy = Normal
                    if( wordScreenState.wrongWords.isNotEmpty()){
                        wordScreenState.wrongWords.clear()
                    }
                    if(wordScreenState.reviewWords.isNotEmpty()){
                        wordScreenState.reviewWords.clear()
                    }
                    wordRequestFocus()
                })
            }
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainContent(
    appState: AppState,
    wordScreenState: WordScreenState,
    dictationState: DictationState,
    azureTTS: AzureTTS,
    currentWord:Word,
    videoBounds: Rectangle,
    resetVideoBounds :() -> Rectangle,
    wordFocusRequester:FocusRequester,
    window: ComposeWindow,
){
    var nextButtonVisible by remember{ mutableStateOf(false) }
        /** Coroutine oluşturucu */
        val scope = rememberCoroutineScope()

        /** Kelime giriş hatası*/
        var isWrong by remember { mutableStateOf(false) }

        /** Video oynatılıyor mu */
        var isPlaying by remember { mutableStateOf(false) }

        /** Altyazı oynatma kısayol tuşu dizini */
        var plyingIndex by remember { mutableStateOf(0) }

        /** Doldurulmuş yer imi simgesini göster */
        var showBookmark by remember { mutableStateOf(false) }

        /** Silme iletişim kutusunu göster */
        var showDeleteDialog by remember { mutableStateOf(false) }

        /** Mevcut kelimeyi bildiklerine ekleme onay iletişim kutusunu göster */
        var showFamiliarDialog by remember { mutableStateOf(false) }

        /** Altyazı giriş kutusu odak isteyicisi*/
        val (focusRequester1,focusRequester2,focusRequester3) = remember { FocusRequester.createRefs() }

        /** Sabit genişlikli yazı tipi*/
        val monospace  = rememberMonospace()

        val audioPlayerComponent = LocalAudioPlayerComponent.current

        val clipboardManager = LocalClipboardManager.current

        /** Kelime telaffuzu oynatılıyor mu */
        var isPlayingAudio by remember { mutableStateOf(false) }

    val onVideoBoundsChanged :(Boolean) -> Unit= {
        wordScreenState.isChangeVideoBounds = it
        if(it){
            wordScreenState.changePlayerBounds(videoBounds)
        }
    }
        /**
         * 用快捷键播放视频时被调用的函数，
         * Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
         */
        @OptIn(ExperimentalSerializationApi::class)
        val shortcutPlay: (playTriple: Triple<Caption, String, Int>?) -> Unit = { playTriple ->
            if (playTriple != null && !isPlaying) {
                scope.launch {
                        play(
                            window = appState.videoPlayerWindow,
                            setIsPlaying = { isPlaying = it },
                            appState.global.videoVolume,
                            playTriple,
                            appState.videoPlayerComponent,
                            videoBounds,
                            wordScreenState.externalSubtitlesVisible,
                            vocabularyDir = wordScreenState.getVocabularyDir(),
                            resetVideoBounds = resetVideoBounds,
                            isVideoBoundsChanged = wordScreenState.isChangeVideoBounds,
                            setIsVideoBoundsChanged = onVideoBoundsChanged
                        )
                }
            }
        }


        /** Mevcut kelimeyi sil */
        val deleteWord:() -> Unit = {
            val index = wordScreenState.index
            wordScreenState.vocabulary.wordList.removeAt(index)
            wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
            if(wordScreenState.vocabulary.name == "HardVocabulary"){
                appState.hardVocabulary.wordList.remove(currentWord)
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }
            try{
                wordScreenState.saveCurrentVocabulary()
                wordScreenState.clearInputtedState()
            }catch (e:Exception){
                // 回滚
                wordScreenState.vocabulary.wordList.add(index,currentWord)
                wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
                if(wordScreenState.vocabulary.name == "HardVocabulary"){
                    appState.hardVocabulary.wordList.add(currentWord)
                    appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "Kelime silinemedi, hata mesajı:\n${e.message}")
            }
        }

        /** Mevcut kelimeyi bildiklerine ekle */
        val addToFamiliar:() -> Unit = {
            val file = getFamiliarVocabularyFile()
            val familiar =  loadVocabulary(file.absolutePath)
            val familiarWord = currentWord.deepCopy()
            // Mevcut kelime dağarcığı MKV veya SUBTITLES türündeyse, dahili kelime dağarcığının harici bir kelime dağarcığına dönüştürülmesi gerekir.
            if (wordScreenState.vocabulary.type == VocabularyType.MKV ||
                wordScreenState.vocabulary.type == VocabularyType.SUBTITLES
            ) {
                familiarWord.captions.forEach{ caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = wordScreenState.vocabulary.relateVideoPath,
                        subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId,
                        subtitlesName = wordScreenState.vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    familiarWord.externalCaptions.add(externalCaption)
                }
                familiarWord.captions.clear()

            }
            if(familiar.name.isEmpty()){
                familiar.name = "FamiliarVocabulary"
            }
            if(!familiar.wordList.contains(familiarWord)){
                familiar.wordList.add(familiarWord)
                familiar.size = familiar.wordList.size
            }
            try{
                saveVocabulary(familiar,file.absolutePath)
                deleteWord()
            }catch(e:Exception){
                // 回滚
                if(familiar.wordList.contains(familiarWord)){
                    familiar.wordList.remove(familiarWord)
                    familiar.size = familiar.wordList.size
                }

                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "Bildikleriniz kaydedilemedi, hata mesajı:\n${e.message}")
            }
            showFamiliarDialog = false
        }

        /** Zor kelimeler listesine ekleme işlevi */
        val bookmarkClick :() -> Unit = {
            val hardWord = currentWord.deepCopy()
            val contains = appState.hardVocabulary.wordList.contains(currentWord)
            val index = appState.hardVocabulary.wordList.indexOf(currentWord)
            if(contains){
                appState.hardVocabulary.wordList.removeAt(index)
                // Mevcut kelime dağarcığı zor kelimeler listesi ise, kullanıcı kelimeyi zor kelimeler listesinden (mevcut kelime dağarcığı) silmek istiyor demektir
                if(wordScreenState.vocabulary.name == "HardVocabulary"){
                    wordScreenState.vocabulary.wordList.remove(currentWord)
                    wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
                    try{
                        wordScreenState.saveCurrentVocabulary()
                    }catch (e:Exception){
                        // 回滚
                        appState.hardVocabulary.wordList.add(index,currentWord)
                        appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
                        wordScreenState.vocabulary.wordList.add(wordScreenState.index,currentWord)
                        wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size

                        e.printStackTrace()
                        JOptionPane.showMessageDialog(window, "Mevcut kelime dağarcığı kaydedilemedi, hata mesajı:\n${e.message}")
                    }

                }
            }else{
                val relateVideoPath = wordScreenState.vocabulary.relateVideoPath
                val subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId
                val subtitlesName =
                    if (wordScreenState.vocabulary.type == VocabularyType.SUBTITLES) wordScreenState.vocabulary.name else ""

                currentWord.captions.forEach { caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath,
                        subtitlesTrackId,
                        subtitlesName,
                        caption.start,
                        caption.end,
                        caption.content
                    )
                    hardWord.externalCaptions.add(externalCaption)
                }
                hardWord.captions.clear()
                appState.hardVocabulary.wordList.add(hardWord)
            }
            try{
                appState.saveHardVocabulary()
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }catch(e:Exception){
                // 回滚
                if(contains){
                    appState.hardVocabulary.wordList.add(index,hardWord)
                }else{

                    appState.hardVocabulary.wordList.remove(hardWord)
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "Zor kelimeler kaydedilemedi, hata mesajı:\n${e.message}")
            }

        }

        /** Genel kısayol tuşu geri arama işlevi */
        val globalKeyEvent: (KeyEvent) -> Boolean = {
            when {
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.A && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordFocusRequester.requestFocus()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        appState.openSearch()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.P && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.phoneticVisible = !wordScreenState.phoneticVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.phoneticVisible = wordScreenState.phoneticVisible
                            dictationState.saveDictationState()
                        }

                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.L && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.morphologyVisible = !wordScreenState.morphologyVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.morphologyVisible = wordScreenState.morphologyVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.E && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.definitionVisible = !wordScreenState.definitionVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.definitionVisible = wordScreenState.definitionVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.H && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.sentencesVisible = !wordScreenState.sentencesVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.sentencesVisible = wordScreenState.sentencesVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.translationVisible = !wordScreenState.translationVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.translationVisible = wordScreenState.translationVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.wordVisible = !wordScreenState.wordVisible
                        wordScreenState.saveWordScreenState()
                    }
                    true
                }

                (it.isCtrlPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) -> {
                    if (!isPlayingAudio) {
                        scope.launch (Dispatchers.IO){
                            val audioPath =  getAudioPath(
                                word = currentWord.value,
                                audioSet = appState.localAudioSet,
                                addToAudioSet = { appState.localAudioSet.add(it) },
                                pronunciation = wordScreenState.pronunciation,
                                azureTTS = azureTTS
                            )
                            playAudio(
                                word = currentWord.value,
                                audioPath = audioPath,
                                pronunciation =  wordScreenState.pronunciation,
                                volume = appState.global.audioVolume,
                                audioPlayerComponent = audioPlayerComponent,
                                changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                            )
                        }

                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.subtitlesVisible = !wordScreenState.subtitlesVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.subtitlesVisible = wordScreenState.subtitlesVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.One && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        appState.openSettings = !appState.openSettings
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                    if(!it.isShiftPressed){
                        scope.launch {
                            bookmarkClick()
                        }
                        showBookmark = true
                        true
                    }else false
                }
                (it.isCtrlPressed && it.key == Key.Y && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.vocabulary.name == "FamiliarVocabulary"){
                        JOptionPane.showMessageDialog(window, "Bildiklerinize eklenmiş bir kelime tekrar eklenemez.")
                    }else{
                        showFamiliarDialog = true
                    }
                    true
                }
                (it.isShiftPressed && it.key == Key.Delete && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        showDeleteDialog = true
                    }
                    true
                }
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.Z && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest ){
                        val playTriple = if (wordScreenState.vocabulary.type == VocabularyType.DOCUMENT) {
                            getPayTriple(currentWord, 0)
                        } else {
                            val caption = wordScreenState.getCurrentWord().captions[0]
                            Triple(caption, wordScreenState.vocabulary.relateVideoPath, wordScreenState.vocabulary.subtitlesTrackId)
                        }
                        plyingIndex = 0
                        if (playTriple != null && wordScreenState.subtitlesVisible &&  wordScreenState.isWriteSubtitles ) focusRequester1.requestFocus()
                        shortcutPlay(playTriple)
                    }
                    true
                }
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.X && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest){
                        val playTriple = if (wordScreenState.getCurrentWord().externalCaptions.size >= 2) {
                            getPayTriple(currentWord, 1)
                        } else if (wordScreenState.getCurrentWord().captions.size >= 2) {
                            val caption = wordScreenState.getCurrentWord().captions[1]
                            Triple(caption, wordScreenState.vocabulary.relateVideoPath, wordScreenState.vocabulary.subtitlesTrackId)
                        }else null
                        plyingIndex = 1
                        if (playTriple != null && wordScreenState.subtitlesVisible && wordScreenState.isWriteSubtitles) focusRequester2.requestFocus()
                        shortcutPlay(playTriple)
                    }
                    true
                }
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest){
                        val playTriple = if (wordScreenState.getCurrentWord().externalCaptions.size >= 3) {
                            getPayTriple(currentWord, 2)
                        } else if (wordScreenState.getCurrentWord().captions.size >= 3) {
                            val caption = wordScreenState.getCurrentWord().captions[2]
                            Triple(caption, wordScreenState.vocabulary.relateVideoPath, wordScreenState.vocabulary.subtitlesTrackId)
                        }else null
                        plyingIndex = 2
                        if (playTriple != null && wordScreenState.subtitlesVisible && wordScreenState.isWriteSubtitles) focusRequester3.requestFocus()
                        shortcutPlay(playTriple)
                    }
                    true
                }
                else -> false
            }

        }

        /** Bu bölüm tamamlandı iletişim kutusunu göster */
        var showChapterFinishedDialog by remember { mutableStateOf(false) }

        /** Tüm kelime dağarcığı öğrenildi iletişim kutusunu göster */
        var isVocabularyFinished by remember { mutableStateOf(false) }

        /** Bölüm tamamlandığında ses efektini oynat */
        val playChapterFinished = {
            if (wordScreenState.isPlaySoundTips) {
                playSound("audio/Success!!.wav", wordScreenState.soundTipsVolume)
            }
        }

        /**
         * Dikte modunda, gözler kapalı kelime yazarken, kelimeyi yeni bitirdiğinizde bu sesi çalmak iyi hissettirmiyor.
         * Dikte olmayan modda Enter tuşuna basıldığında böyle bir his olmaz, çünkü Enter tuşuna basıldığında,
         * giriş zaten tamamlanmıştır, bir beklenti vardır ve bir ipucu sesinin çalınacağı tahmin edilir.
         */
        val delayPlaySound:() -> Unit = {
            Timer("bolumBitirmeSesiCal", false).schedule(1000) {
                playChapterFinished()
            }
            showChapterFinishedDialog = true
        }


        /** Yanlış kelimeleri gözden geçirirken dizini artır */
        val increaseWrongIndex:() -> Unit = {
            if (wordScreenState.dictationIndex + 1 == wordScreenState.wrongWords.size) {
                delayPlaySound()
            } else wordScreenState.dictationIndex++
        }


        /** Sonraki kelimeye geç */
        val toNext: () -> Unit = {
            scope.launch {
                wordScreenState.clearInputtedState()
                when (wordScreenState.memoryStrategy) {
                    Normal -> {
                        when {
                            (wordScreenState.index == wordScreenState.vocabulary.size - 1) -> {
                                isVocabularyFinished = true
                                playChapterFinished()
                                showChapterFinishedDialog = true
                            }
                            ((wordScreenState.index + 1) % 20 == 0) -> {
                                playChapterFinished()
                                showChapterFinishedDialog = true
                            }
                            else -> wordScreenState.index += 1
                        }
                        wordScreenState.saveWordScreenState()
                    }
                    Dictation -> {
                        if (wordScreenState.dictationIndex + 1 == wordScreenState.dictationWords.size) {
                            delayPlaySound()
                        } else wordScreenState.dictationIndex++
                    }
                    DictationTest -> {
                        if (wordScreenState.dictationIndex + 1 == wordScreenState.reviewWords.size) {
                            delayPlaySound()
                        } else wordScreenState.dictationIndex++
                    }
                    NormalReviewWrong -> { increaseWrongIndex() }
                    DictationTestReviewWrong -> { increaseWrongIndex() }
                }

                wordFocusRequester.requestFocus()

            }
        }

        /** Önceki kelimeye geç, dikte sırasında izin verilmez */
        val previous :() -> Unit = {
            scope.launch {
                // Normal kelime ezberleme
                if(wordScreenState.memoryStrategy == Normal){
                    wordScreenState.clearInputtedState()
                    if((wordScreenState.index) % 20 != 0 ){
                        wordScreenState.index -= 1
                        wordScreenState.saveWordScreenState()
                    }
                    // Yanlış kelimeleri gözden geçir
                }else if (wordScreenState.memoryStrategy == NormalReviewWrong || wordScreenState.memoryStrategy == DictationTestReviewWrong ){
                    wordScreenState.clearInputtedState()
                    if(wordScreenState.dictationIndex > 0 ){
                        wordScreenState.dictationIndex -= 1
                    }
                }
                wordFocusRequester.requestFocus()
            }
        }
        Box(
            modifier = Modifier.fillMaxSize()
                .onKeyEvent { globalKeyEvent(it) }
                .onPointerEvent(PointerEventType.Move){nextButtonVisible = true}
                .onPointerEvent(PointerEventType.Exit){nextButtonVisible = false}
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .background(MaterialTheme.colors.background)
                .focusable(true)
                .align(Alignment.Center)
                .padding(end = 0.dp,bottom = 58.dp)
        ) {

            /** Dikte modundaki yanlış kelimeler */
            val dictationWrongWords = remember { mutableStateMapOf<Word, Int>()}

            /** Kelime düzenleme iletişim kutusunu göster */
            var showEditWordDialog by remember { mutableStateOf(false) }

            /** Dikte modunda saklanan yanlış kelimeleri temizle */
            val resetChapterTime: () -> Unit = {
                dictationWrongWords.clear()
            }


            /** Hata ses efektini oynat */
            val playBeepSound = {
                if (wordScreenState.isPlaySoundTips) {
                    playSound("audio/beep.wav", wordScreenState.soundTipsVolume)
                }
            }

            /** Başarı ses efektini oynat */
            val playSuccessSound = {
                if (wordScreenState.isPlaySoundTips) {
                    playSound("audio/hint.wav", wordScreenState.soundTipsVolume)
                }
            }


            /** Tuş vuruşu ses efektini oynat */
            val playKeySound = {
                if (appState.global.isPlayKeystrokeSound) {
                    playSound("audio/keystroke.wav", appState.global.keystrokeVolume)
                }
            }

            /**
             * Kullanıcı dikte testinde enter'a bastığında çağrılan işlev, dikte testinde kelimeyi atlamak da hata sayılır
             */
            val dictationSkipCurrentWord: () -> Unit = {
                if (wordScreenState.wordCorrectTime == 0) {
                    val dictationWrongTime = dictationWrongWords[currentWord]
                    if (dictationWrongTime == null) {
                        dictationWrongWords[currentWord] = 1
                    }
                }
            }

            /** Odağı kelime giriş kutusuna taşı */
            val jumpToWord:() -> Unit = {
                wordFocusRequester.requestFocus()
            }

            /** Odağı altyazı kopyalamaya taşı */
            val jumpToCaptions:() -> Unit = {
                if((wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest) &&
                    wordScreenState.subtitlesVisible && (currentWord.captions.isNotEmpty() || currentWord.externalCaptions.isNotEmpty())
                ){
                    focusRequester1.requestFocus()
                }
            }

            /** Girilen kelimeyi kontrol et */
            val checkWordInput: (String) -> Unit = { input ->
                if(!isWrong){
                    wordScreenState.wordTextFieldValue = input
                    wordScreenState.wordTypingResult.clear()
                    var done = true
                    /**
                     *  Kullanıcının çok uzun içerik yapıştırmasını engelle, yapıştırılan içerik word.value uzunluğunu aşarsa BasicTextField genişliğini değiştirir ve Text genişliğiyle eşleşmez
                     */
                    if (input.length > currentWord.value.length) {
                        wordScreenState.wordTypingResult.clear()
                        wordScreenState.wordTextFieldValue = ""
                    } else {
                        val inputChars = input.toList()
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar == wordChar) {
                                wordScreenState.wordTypingResult.add(Pair(inputChar, true))
                            } else {
                                // Harf giriş hatası
                                wordScreenState.wordTypingResult.add(Pair(inputChar, false))
                                done = false
                                playBeepSound()
                                isWrong = true
                                wordScreenState.wordWrongTime++
                                // Dikte testi veya bağımsız dikte testi ise, yanlış kelimeleri toplamak gerekir
                                if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                    val dictationWrongTime = dictationWrongWords[currentWord]
                                    if (dictationWrongTime != null) {
                                        dictationWrongWords[currentWord] = dictationWrongTime + 1
                                    } else {
                                        dictationWrongWords[currentWord] = 1
                                    }
                                }
//                                // Kelime telaffuzunu tekrar oynat
                                if (!isPlayingAudio && wordScreenState.playTimes == 2) {
                                    scope.launch (Dispatchers.IO){
                                        val audioPath =  getAudioPath(
                                            word = currentWord.value,
                                            audioSet = appState.localAudioSet,
                                            addToAudioSet = { appState.localAudioSet.add(it) },
                                            pronunciation = wordScreenState.pronunciation,
                                            azureTTS = azureTTS
                                        )
                                        playAudio(
                                            word = currentWord.value,
                                            audioPath = audioPath,
                                            pronunciation =  wordScreenState.pronunciation,
                                            volume = appState.global.audioVolume,
                                            audioPlayerComponent = audioPlayerComponent,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
//                                        setIsAutoPlay = {}
                                        )
                                    }

                                }

                            }
                        }
                        // Kullanıcının girdiği kelime tamamen doğru
                        if (wordScreenState.wordTypingResult.size == currentWord.value.length && done) {
                            // Giriş tamamen doğru
                            playSuccessSound()
                            wordScreenState.wordCorrectTime++
                            if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                Timer("dogruGirisSonrakine", false).schedule(50) {
                                    toNext()
                                }
                            }else if (wordScreenState.isAuto && wordScreenState.wordCorrectTime == wordScreenState.repeatTimes ) {
                                Timer("dogruGirisSonrakine", false).schedule(50) { // Timer adı aynı kalabilir, işlevi benzer
                                    toNext()
                                }
                            } else {
                                Timer("dogruGirisTemizle", false).schedule(50){
                                    wordScreenState.wordTypingResult.clear()
                                    wordScreenState.wordTextFieldValue = ""
                                }

                                // Kelime telaffuzunu tekrar oynat
                                if (!isPlayingAudio && wordScreenState.playTimes == 2) {
                                    scope.launch (Dispatchers.IO){
                                        val audioPath =  getAudioPath(
                                            word = currentWord.value,
                                            audioSet = appState.localAudioSet,
                                            addToAudioSet = { appState.localAudioSet.add(it) },
                                            pronunciation = wordScreenState.pronunciation,
                                            azureTTS = azureTTS
                                        )
                                        playAudio(
                                            word = currentWord.value,
                                            audioPath = audioPath,
                                            pronunciation =  wordScreenState.pronunciation,
                                            volume = appState.global.audioVolume,
                                            audioPlayerComponent = audioPlayerComponent,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
//                                        setIsAutoPlay = {}
                                        )
                                    }

                                }
                            }
                        }
                    }
                }else{
                    // Hatalı girişten sonra yazmaya devam et
                    if(input.length > wordScreenState.wordTypingResult.size){
                        // Dize kesilmezse, kullanıcı bir tuşa uzun basarsa program çökebilir
                        val inputStr = input.substring(0,wordScreenState.wordTypingResult.size)
                        val inputChars = inputStr.toList()
                        isWrong = false
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar != wordChar) {
                                playBeepSound()
                                isWrong = true
                            }
                        }
                        if(!isWrong){
                            wordScreenState.wordTextFieldValue = inputStr
                        }
                    }else if(input.length == wordScreenState.wordTypingResult.size-1){
                        // Hatalı girişten sonra hatalı harfi silmek için geri al tuşuna bas
                        isWrong = false
                            wordScreenState.wordTypingResult.removeLast()
                            wordScreenState.wordTextFieldValue = input
                    }else if(input.isEmpty()){
                        // Hatalı girişten sonra Ctrl + A ile tümünü seçip tüm girişi sil
                        wordScreenState.wordTextFieldValue = ""
                        wordScreenState.wordTypingResult.clear()
                        isWrong = false
                    }

                }

            }


            /** Girilen altyazıyı kontrol et */
            val checkCaptionsInput: (Int, String, String) -> Unit = { index, input, captionContent ->
                when(index){
                    0 -> wordScreenState.captionsTextFieldValue1 = input
                    1 -> wordScreenState.captionsTextFieldValue2 = input
                    2 -> wordScreenState.captionsTextFieldValue3 = input
                }
                val typingResult = wordScreenState.captionsTypingResultMap[index]
                typingResult!!.clear()
                val inputChars = input.toMutableList()
                for (i in inputChars.indices) {
                    val inputChar = inputChars[i]
                    if(i<captionContent.length){
                        val captionChar = captionContent[i]
                        if (inputChar == captionChar) {
                            typingResult.add(Pair(captionChar, true))
                        }else if (inputChar == ' ' && (captionChar == '[' || captionChar == ']')) {
                            typingResult.add(Pair(captionChar, true))
                            // Müzik sembollerini girmek zor olduğundan boşlukla değiştirilebilir
                        }else if (inputChar == ' ' && (captionChar == '♪')) {
                            typingResult.add(Pair(captionChar, true))
                            // Müzik sembolü iki boşluk kaplar, bu yüzden ♪ ekleyip bir boşluk silin
                            inputChars.add(i,'♪')
                            inputChars.removeAt(i+1)
                            val textFieldValue = String(inputChars.toCharArray())
                            when(index){
                                0 -> wordScreenState.captionsTextFieldValue1 = textFieldValue
                                1 -> wordScreenState.captionsTextFieldValue2 = textFieldValue
                                2 -> wordScreenState.captionsTextFieldValue3 = textFieldValue
                            }
                        } else {
                            typingResult.add(Pair(inputChar, false))
                        }
                    }else{
                        typingResult.add(Pair(inputChar, false))
                    }

                }

            }

            /** Dizini azalt */
            val decreaseIndex = {
                if(wordScreenState.index == wordScreenState.vocabulary.size - 1){
                    val mod = wordScreenState.vocabulary.size % 20
                    wordScreenState.index -= (mod-1)
                }else if (wordScreenState.vocabulary.size > 19) wordScreenState.index -= 19
                else wordScreenState.index = 0
            }

            /** Doğruluk oranını hesapla */
            val correctRate: () -> Float = {
                val size = if(wordScreenState.memoryStrategy == Dictation ) wordScreenState.dictationWords.size else wordScreenState.reviewWords.size
                var rate =  (size - dictationWrongWords.size).div(size.toFloat()) .times(1000)
                rate = rate.toInt().toFloat().div(10)
                rate
            }

            /** Bu bölümü tekrar öğren */
            val learnAgain: () -> Unit = {
                decreaseIndex()
                resetChapterTime()
                wordScreenState.saveWordScreenState()
                showChapterFinishedDialog = false
                isVocabularyFinished = false
            }


            /** Yanlış kelimeleri gözden geçir */
            val reviewWrongWords: () -> Unit = {
                val reviewList = dictationWrongWords.keys.toList()
                if (reviewList.isNotEmpty()) {
                    wordScreenState.showInfo(clear = false)
                    if (wordScreenState.memoryStrategy == DictationTest ||
                        wordScreenState.memoryStrategy == DictationTestReviewWrong
                    ) {
                        wordScreenState.memoryStrategy = DictationTestReviewWrong
                    }else{
                        wordScreenState.memoryStrategy = NormalReviewWrong
                    }
                    if( wordScreenState.wrongWords.isEmpty()){
                        wordScreenState.wrongWords.addAll(reviewList)
                    }
                    wordScreenState.dictationIndex = 0
                    showChapterFinishedDialog = false
                }
            }

            /** Sonraki bölüm */
            val nextChapter: () -> Unit = {

                if (wordScreenState.memoryStrategy == NormalReviewWrong ||
                    wordScreenState.memoryStrategy == DictationTestReviewWrong
                ) {
                    wordScreenState.wrongWords.clear()
                }

                if( wordScreenState.memoryStrategy == Dictation){
                    wordScreenState.showInfo()
                }

                wordScreenState.index += 1
                wordScreenState.chapter++
                resetChapterTime()
                wordScreenState.memoryStrategy = Normal
                wordScreenState.saveWordScreenState()
                showChapterFinishedDialog = false
            }


            /** Normal kelime ezberleme, dikte testine girmek için gereken kelimeler */
            val shuffleNormal:() -> Unit = {
                val wordValue = wordScreenState.getCurrentWord().value
                val shuffledList = wordScreenState.generateDictationWords(wordValue)
                wordScreenState.dictationWords.clear()
                wordScreenState.dictationWords.addAll(shuffledList)
            }
            /** Bağımsız dikte testinden tekrar dikte testine girerken gereken kelimeler */
            val shuffleDictationReview:() -> Unit = {
                var shuffledList = wordScreenState.reviewWords.shuffled()
                // Karıştırılmış listenin ilk kelimesi mevcut bölümün son kelimesine eşitse, yeniden oluşturma tetiklenmez
                while(shuffledList.first() == currentWord){
                    shuffledList = wordScreenState.reviewWords.shuffled()
                }
                wordScreenState.reviewWords.clear()
                wordScreenState.reviewWords.addAll(shuffledList)
            }
            /** Dikte moduna gir */
            val enterDictation: () -> Unit = {
                scope.launch {
                    wordScreenState.saveWordScreenState()
                    when(wordScreenState.memoryStrategy){
                        // Normal kelime ezberlemeden ilk kez dikte testine girme
                        Normal -> {
                            shuffleNormal()
                            wordScreenState.memoryStrategy = Dictation
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // Normal kelime ezberlerken tekrar dikte etmeyi seç
                        Dictation ->{
                            shuffleNormal()
                            wordScreenState.dictationIndex = 0
                        }
                        // Yanlış kelimeleri gözden geçirmeden dikte testine girme, burada iki durum vardır:
                        // Biri normal kelime ezberlemeden yanlış kelimeleri gözden geçirmeye girmek, gözden geçirme tamamlandıktan sonra tekrar dikte etmek
                        NormalReviewWrong ->{
                            wordScreenState.memoryStrategy = Dictation
                            wordScreenState.wrongWords.clear()
                            shuffleNormal()
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // Biri bağımsız dikte testinden yanlış kelimeleri gözden geçirmeye girmek, gözden geçirme tamamlandıktan sonra tekrar dikte etmek
                        DictationTestReviewWrong ->{
                            wordScreenState.memoryStrategy = DictationTest
                            wordScreenState.wrongWords.clear()
                            shuffleDictationReview()
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // Bağımsız dikte testindeyken tekrar dikte etmeyi seç
                        DictationTest ->{
                            shuffleDictationReview()
                            wordScreenState.dictationIndex = 0
                        }
                    }
                    wordFocusRequester.requestFocus()
                    resetChapterTime()
                    showChapterFinishedDialog = false
                    isVocabularyFinished = false
                }
            }


            /**
             * Dizini sıfırla
             * Parametre isShuffle kelime dağarcığını karıştırıp karıştırmayacağı
             */
            val resetIndex: (isShuffle: Boolean) -> Unit = { isShuffle ->
                // Sırayı karıştırmak istiyorsanız
                if (isShuffle) {
                    // Dahili kelime dağarcığının adresi
                    val path = getResourcesFile("vocabulary").absolutePath
                    // Karıştırılacak kelime dağarcığı dahili bir kelime dağarcığıysa, karıştırılmış kelime dağarcığını kaydetmek için bir adres seçmeniz gerekir,
                    // Bir adres seçmezseniz, yazılım yükseltildikten sonra kelime dağarcığı sıfırlanır.
                    if(wordScreenState.vocabularyPath.startsWith(path)){
                        val fileChooser = appState.futureFileChooser.get()
                        fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                        fileChooser.dialogTitle = "Sıfırladıktan sonra kelime dağarcığını kaydet"
                        val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                        val fileName = File(wordScreenState.vocabularyPath).nameWithoutExtension
                        fileChooser.selectedFile = File("$myDocuments${File.separator}$fileName.json")
                        val userSelection = fileChooser.showSaveDialog(window)
                        if (userSelection == JFileChooser.APPROVE_OPTION) {
                            val selectedFile = fileChooser.selectedFile
                            val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                            val savePath = Paths.get(selectedFile.absolutePath)
                            if(savePath.startsWith(vocabularyDirPath)){
                                JOptionPane.showMessageDialog(null,"Kelime dağarcığı uygulama kurulum dizinine kaydedilemez, çünkü yazılım güncellendiğinde veya kaldırıldığında kelime dağarcığı sıfırlanır veya silinir.")
                            }else{
                                wordScreenState.vocabulary.wordList.shuffle()
                                val shuffledList = wordScreenState.vocabulary.wordList
                                val vocabulary = Vocabulary(
                                    name = selectedFile.nameWithoutExtension,
                                    type = VocabularyType.DOCUMENT,
                                    language = "english",
                                    size = wordScreenState.vocabulary.size,
                                    relateVideoPath = wordScreenState.vocabulary.relateVideoPath,
                                    subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId,
                                    wordList = shuffledList
                                )

                                try {
                                    saveVocabulary(vocabulary, selectedFile.absolutePath)
                                    appState.changeVocabulary(selectedFile, wordScreenState, 0)
                                    // changeVocabulary dahili kelime dağarcığını son kullanılanlar listesine kaydeder,
                                    // Kaydettikten sonra listeyi tekrar değiştirirseniz, aynı ada sahip iki kelime dağarcığı olacaktır,
                                    // Bu yüzden yeni eklenen kelime dağarcığını son kullanılanlar listesinden silmeniz gerekir
                                    for (i in 0 until appState.recentList.size) {
                                        val recentItem = appState.recentList[i]
                                        if (recentItem.name == wordScreenState.vocabulary.name) {
                                            appState.removeRecentItem(recentItem)
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    JOptionPane.showMessageDialog(window, "Kelime dağarcığı kaydedilemedi, hata mesajı:\n${e.message}")
                                }


                            }
                        }
                    }else{
                        try{
                            wordScreenState.vocabulary.wordList.shuffle()
                            wordScreenState.saveCurrentVocabulary()
                        }catch(e:Exception){
                            e.printStackTrace()
                            JOptionPane.showMessageDialog(window, "Kelime dağarcığı kaydedilemedi, hata mesajı:\n${e.message}")
                        }

                    }

                }

                wordScreenState.index = 0
                wordScreenState.chapter = 1
                wordScreenState.saveWordScreenState()
                resetChapterTime()
                showChapterFinishedDialog = false
                isVocabularyFinished = false
            }
            val wordKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                when {
                    ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown || it.key == Key.DirectionRight)
                            && it.type == KeyEventType.KeyUp) -> {
                        toNext()
                        if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                            dictationSkipCurrentWord()
                        }
                        true
                    }
                    ((it.key == Key.PageUp || it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                        previous()
                        true
                    }
                    (it.isCtrlPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                        if(!it.isShiftPressed){
                            clipboardManager.setText(AnnotatedString(currentWord.value))
                            true
                        }else false

                    }
                    (it.isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                        jumpToCaptions()
                        true
                    }

                    (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) -> {
                        jumpToCaptions()
                        true
                    }
                    (it.type == KeyEventType.KeyDown
                            && it.key != Key.ShiftRight
                            && it.key != Key.ShiftLeft
                            && it.key != Key.CtrlRight
                            && it.key != Key.CtrlLeft
                            && it.key != Key.AltLeft
                            && it.key != Key.AltRight
                            && it.key != Key.Escape
                            && it.key != Key.Enter
                            && it.key != Key.NumPadEnter
                            ) -> {
                        playKeySound()
                        true
                    }
                    else -> false
                }
            }


            LaunchedEffect(appState.vocabularyChanged){
                if(appState.vocabularyChanged){
                    wordScreenState.clearInputtedState()
                    if(wordScreenState.memoryStrategy == NormalReviewWrong ||
                        wordScreenState.memoryStrategy == DictationTestReviewWrong
                    ){
                        wordScreenState.wrongWords.clear()
                    }
                    if (wordScreenState.memoryStrategy == Dictation) {
                        wordScreenState.showInfo()
                        resetChapterTime()
                    }

                    if(wordScreenState.memoryStrategy == DictationTest) wordScreenState.memoryStrategy = Normal


                    appState.vocabularyChanged = false
                }
            }

            var activeMenu by remember { mutableStateOf(false) }
            Box(
                Modifier.onPointerEvent(PointerEventType.Exit) { activeMenu = false }
            ) {
                /** Dinamik menü, fare kelime alanına geldiğinde görüntülenir */
                if (activeMenu) {
                    Row(modifier = Modifier.align(Alignment.TopCenter)) {
                        val contains = appState.hardVocabulary.wordList.contains(currentWord)
                        DeleteButton(onClick = { showDeleteDialog = true })
                        EditButton(onClick = { showEditWordDialog = true })
                        FamiliarButton(onClick = {
                            if(wordScreenState.vocabulary.name == "FamiliarVocabulary"){
                                JOptionPane.showMessageDialog(window, "Bildiklerinize eklenmiş bir kelime tekrar eklenemez.")
                            }else{
                                showFamiliarDialog = true
                            }

                        })
                        HardButton(
                            onClick = { bookmarkClick() },
                            contains = contains,
                            fontFamily = monospace
                        )
                        CopyButton(wordValue = currentWord.value)
                    }
                }else if(showBookmark){
                    val contains = appState.hardVocabulary.wordList.contains(currentWord)
                    // Bu düğme yalnızca 0.3 saniye görüntülenir ve sonra kaybolur
                    BookmarkButton(
                        modifier = Modifier.align(Alignment.TopCenter).padding(start = 96.dp),
                        contains = contains,
                        disappear = {showBookmark = false}
                    )
                }

                Row(Modifier.align(Alignment.Center)){
                    Word(
                        word = currentWord,
                        global = appState.global,
                        wordVisible = wordScreenState.wordVisible,
                        pronunciation = wordScreenState.pronunciation,
                        azureTTS = azureTTS,
                        playTimes = wordScreenState.playTimes,
                        isPlaying = isPlayingAudio,
                        setIsPlaying = { isPlayingAudio = it },
                        isDictation = (wordScreenState.memoryStrategy == Dictation ||wordScreenState.memoryStrategy == DictationTest),
                        fontFamily = monospace,
                        audioSet = appState.localAudioSet,
                        addToAudioSet = {appState.localAudioSet.add(it) },
                        correctTime = wordScreenState.wordCorrectTime,
                        wrongTime = wordScreenState.wordWrongTime,
                        textFieldValue = wordScreenState.wordTextFieldValue,
                        typingResult = wordScreenState.wordTypingResult,
                        checkTyping = { checkWordInput(it) },
                        focusRequester = wordFocusRequester,
                        textFieldKeyEvent = {wordKeyEvent(it)},
                        showMenu = {activeMenu = true}
                    )
                }

            }


            Phonetic(
                word = currentWord,
                phoneticVisible = wordScreenState.phoneticVisible,
                fontSize = appState.global.detailFontSize
            )
            Morphology(
                word = currentWord,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                searching = false,
                morphologyVisible = wordScreenState.morphologyVisible,
                fontSize = appState.global.detailFontSize
            )
            Definition(
                word = currentWord,
                definitionVisible = wordScreenState.definitionVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Translation(
                word = currentWord,
                translationVisible = wordScreenState.translationVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Sentences(
                word = currentWord,
                sentencesVisible = wordScreenState.sentencesVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )

            val startPadding = if ( isPlaying && !wordScreenState.isChangeVideoBounds) 0.dp else 50.dp
            val captionsModifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Max)
                .padding(bottom = 0.dp, start = startPadding)
                .onKeyEvent {
                    when {
                        ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown || (it.key == Key.DirectionRight && !it.isShiftPressed))
                                && it.type == KeyEventType.KeyUp
                                ) -> {
                            toNext()
                            if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                dictationSkipCurrentWord()
                            }
                            true
                        }
                        ((it.key == Key.PageUp  ||  (it.key == Key.DirectionLeft && !it.isShiftPressed)) && it.type == KeyEventType.KeyUp) -> {
                            previous()
                            true
                        }
                        else -> globalKeyEvent(it)
                    }
                }
            Captions(
                captionsVisible = wordScreenState.subtitlesVisible,
                playTripleMap = getPlayTripleMap(wordScreenState.vocabulary.type,wordScreenState.vocabulary.subtitlesTrackId,wordScreenState.vocabulary.relateVideoPath,  currentWord),
                videoPlayerWindow = appState.videoPlayerWindow,
                videoPlayerComponent = appState.videoPlayerComponent,
                isPlaying = isPlaying,
                plyingIndex = plyingIndex,
                setPlayingIndex = {plyingIndex = it},
                volume = appState.global.videoVolume,
                setIsPlaying = { isPlaying = it },
                word = currentWord,
                bounds = videoBounds,
                textFieldValueList = listOf(wordScreenState.captionsTextFieldValue1,wordScreenState.captionsTextFieldValue2,wordScreenState.captionsTextFieldValue3),
                typingResultMap = wordScreenState.captionsTypingResultMap,
                putTypingResultMap = { index, list ->
                    wordScreenState.captionsTypingResultMap[index] = list
                },
                checkTyping = { index, input, captionContent ->
                    checkCaptionsInput(index, input, captionContent)
                },
                playKeySound = { playKeySound() },
                modifier = captionsModifier,
                focusRequesterList = listOf(focusRequester1,focusRequester2,focusRequester3),
                jumpToWord = {jumpToWord()},
                externalVisible = wordScreenState.externalSubtitlesVisible,
                openSearch = {appState.openSearch()},
                fontSize = appState.global.detailFontSize,
                resetVideoBounds = resetVideoBounds,
                isVideoBoundsChanged = wordScreenState.isChangeVideoBounds,
                setIsChangeBounds = onVideoBoundsChanged,
                isWriteSubtitles = wordScreenState.isWriteSubtitles,
                vocabularyDir = wordScreenState.getVocabularyDir()
            )

            if (showDeleteDialog) {
                ConfirmDialog(
                    message = "${currentWord.value} kelimesini silmek istediğinizden emin misiniz?",
                    confirm = {
                        scope.launch {
                            deleteWord()
                            showDeleteDialog = false
                        }
                    },
                    close = { showDeleteDialog = false }
                )
            }
            if(showFamiliarDialog){
                ConfirmDialog(
                    message = "${currentWord.value} kelimesini bildiklerinize eklemek istediğinizden emin misiniz?\n" +
                            "Bildiklerinize eklendikten sonra ${currentWord.value} mevcut kelime dağarcığından silinecektir.",
                    confirm = { scope.launch { addToFamiliar() } },
                    close = { showFamiliarDialog = false }
                )

            }
            if (showEditWordDialog) {
                EditWordDialog(
                    word = currentWord,
                    title = "Kelimeyi Düzenle",
                    appState = appState,
                    vocabulary = wordScreenState.vocabulary,
                    vocabularyDir = wordScreenState.getVocabularyDir(),
                    save = { newWord ->
                        scope.launch {
                            val index = wordScreenState.index
                            // Yeniden oluşturmayı tetikle
                            wordScreenState.vocabulary.wordList.removeAt(index)
                            wordScreenState.vocabulary.wordList.add(index, newWord)
                            try{
                                wordScreenState.saveCurrentVocabulary()
                                showEditWordDialog = false
                            }catch(e:Exception){
                                // Geri al
                                wordScreenState.vocabulary.wordList.removeAt(index)
                                wordScreenState.vocabulary.wordList.add(index, currentWord)
                                e.printStackTrace()
                                JOptionPane.showMessageDialog(window, "Mevcut kelime dağarcığı kaydedilemedi, hata mesajı:\n${e.message}")
                            }

                        }
                    },
                    close = { showEditWordDialog = false }
                )
            }

            /** Bağımsız dikte testi için bölüm seçimi iletişim kutusunu göster */
            var showChapterDialog by remember { mutableStateOf(false) }
            /** Bağımsız dikte testi için bölüm seçimi iletişim kutusunu aç */
            val openReviewDialog:() -> Unit = {
                showChapterFinishedDialog = false
                showChapterDialog = true
                resetChapterTime()
            }

            if(showChapterDialog){
                SelectChapterDialog(
                    close = {showChapterDialog = false},
                    wordScreenState = wordScreenState,
                    wordRequestFocus = {
                        wordFocusRequester.requestFocus()
                    },
                    isMultiple = true
                )
            }

            /** Mevcut bölüm bittiğinde açılan iletişim kutusunu kapat */
            val close: () -> Unit = {
                showChapterFinishedDialog = false
                if(isVocabularyFinished) isVocabularyFinished = false
            }
            if (showChapterFinishedDialog) {
                ChapterFinishedDialog(
                    close = { close() },
                    isVocabularyFinished = isVocabularyFinished,
                    correctRate = correctRate(),
                    memoryStrategy = wordScreenState.memoryStrategy,
                    openReviewDialog = {openReviewDialog()},
                    isReviewWrong = (wordScreenState.memoryStrategy == NormalReviewWrong || wordScreenState.memoryStrategy == DictationTestReviewWrong),
                    dictationWrongWords = dictationWrongWords,
                    enterDictation = { enterDictation() },
                    learnAgain = { learnAgain() },
                    reviewWrongWords = { reviewWrongWords() },
                    nextChapter = { nextChapter() },
                    resetIndex = { resetIndex(it) }
                )
            }
        }


        if (nextButtonVisible) {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "Önceki")
                        }
                    }
                },
                delayMillis = 300,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(
                    onClick = { previous() },
                    modifier = Modifier.testTag("PreviousButton")
                ) {
                    Icon(
                        Icons.Filled.ArrowBackIosNew,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "Sonraki")
                        }
                    }
                },
                delayMillis = 300,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterStart,
                    alignment = Alignment.CenterStart,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(
                    onClick = { toNext()},
                    modifier = Modifier.testTag("NextButton")
                ) {
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }


    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VocabularyEmpty(
    openVocabulary: () -> Unit,
    openBuiltInVocabulary: () -> Unit = {},
    generateVocabulary: () -> Unit = {},
    openDocument: () -> Unit = {},
    parentWindow : ComposeWindow,
    futureFileChooser: FutureTask<JFileChooser>,
    openChooseVocabulary: (String) -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
                    .width(288.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kelime Dağarcığını Aç",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = { openVocabulary() })
                            .padding(5.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "Kullanım Kılavuzu",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = { openDocument() })
                            .width(78.dp)
                            .padding(5.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "Kelime Dağarcığı Oluştur",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = {generateVocabulary()  })
                            .padding(5.dp)
                    )
                }

                var visible by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .onPointerEvent(PointerEventType.Exit) { visible = false }
                ){
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 10.dp).onPointerEvent(PointerEventType.Enter) { visible = true }
                    ) {
                        Text(
                            text = "Dahili Kelime Dağarcığı",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.clickable(onClick = {openBuiltInVocabulary()})
                                .padding(5.dp)
                        )
                    }
                    val scope = rememberCoroutineScope()
                    AnimatedVisibility(visible = visible){

                        /** Kelime dağarcığını kaydet */
                        val save:(File) -> Unit = {file ->
                            scope.launch(Dispatchers.IO) {
                                val name = file.nameWithoutExtension
                                val fileChooser = futureFileChooser.get()
                                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                                fileChooser.dialogTitle = "Kelime Dağarcığını Kaydet"
                                val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                                fileChooser.selectedFile = File("$myDocuments${File.separator}${name}.json")
                                val userSelection = fileChooser.showSaveDialog(parentWindow)
                                if (userSelection == JFileChooser.APPROVE_OPTION) {

                                    val fileToSave = fileChooser.selectedFile
                                    if (fileToSave.exists()) {
                                        // 是-0,否-1，取消-2 (Evet-0, Hayır-1, İptal-2)
                                        val answer =
                                            JOptionPane.showConfirmDialog(parentWindow, "${name}.json zaten var.\nDeğiştirilsin mi?")
                                        if (answer == 0) {
                                            try{
                                                fileToSave.writeBytes(file.readBytes())
                                                openChooseVocabulary(file.absolutePath)
                                            }catch (e:Exception){
                                                e.printStackTrace()
                                                JOptionPane.showMessageDialog(parentWindow,"Kaydetme başarısız, hata mesajı:\n${e.message}")
                                            }

                                        }
                                    } else {
                                        try{
                                            fileToSave.writeBytes(file.readBytes())
                                            openChooseVocabulary(file.absolutePath)
                                        }catch (e:Exception){
                                            e.printStackTrace()
                                            JOptionPane.showMessageDialog(parentWindow,"Kaydetme başarısız, hata mesajı:\n${e.message}")
                                        }

                                    }

                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 5.dp)
                        ) {
                            Text(
                                text = "Seviye 4",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/大学英语/四级.json") // TODO: Dosya yolu da çevrilecek
                                    save(file)
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Seviye 6",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/大学英语/六级.json") // TODO: Dosya yolu da çevrilecek
                                    save(file)
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Oxford 3000 Temel Kelime",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/牛津核心词/The_Oxford_3000.json") // TODO: Dosya yolu da çevrilecek
                                    save(file)
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Daha Fazla",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {openBuiltInVocabulary()})
                                    .padding(5.dp)
                            )
                        }
                    }
                }

            }
        }


    }
}

/**
 * Kelime biçimi bileşeni
 */
@Composable
fun Morphology(
    word: Word,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    searching: Boolean,
    morphologyVisible: Boolean,
    fontSize: TextUnit
) {
    if (morphologyVisible &&(isChangeVideoBounds || !isPlaying )) {
        val exchanges = word.exchange.split("/")
        var preterite = ""
        var pastParticiple = ""
        var presentParticiple = ""
        var third = ""
        var er = ""
        var est = ""
        var plural = ""
        var lemma = ""

        exchanges.forEach { exchange ->
            val pair = exchange.split(":")
            when (pair[0]) {
                "p" -> {
                    preterite = pair[1]
                }
                "d" -> {
                    pastParticiple = pair[1]
                }
                "i" -> {
                    presentParticiple = pair[1]
                }
                "3" -> {
                    third = pair[1]
                }
                "r" -> {
                    er = pair[1]
                }
                "t" -> {
                    est = pair[1]
                }
                "s" -> {
                    plural = pair[1]
                }
                "0" -> {
                    lemma = pair[1]
                }

            }
        }

        Column {
            SelectionContainer {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.height(IntrinsicSize.Max)
                        .width(if(searching) 600.dp else 554.dp)
                        .padding(start = if(searching) 0.dp else 50.dp)

                ) {
                    val textColor = MaterialTheme.colors.onBackground
                    val plainStyle = SpanStyle(
                        color = textColor,
                        fontSize = fontSize,
                    )


                    Text(
                        buildAnnotatedString {
                            if (lemma.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("Kök Hali ")
                                }
                                withStyle(style = plainStyle.copy(color = Color.Magenta)) {
                                    append(lemma)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (preterite.isNotEmpty()) {
                                var color = textColor
                                if (!preterite.endsWith("ed")) {
                                    color = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

                                }
                                withStyle(style = plainStyle) {
                                    append("Geçmiş Zaman Hali ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(preterite)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (pastParticiple.isNotEmpty()) {
                                var color = textColor
                                if (!pastParticiple.endsWith("ed")) {
                                    color =
                                        if (MaterialTheme.colors.isLight) MaterialTheme.colors.primary else Color.Yellow
                                }
                                withStyle(style = plainStyle) {
                                    append("Geçmiş Zaman Sıfat Fiili ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(pastParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (presentParticiple.isNotEmpty()) {
                                val color = if (presentParticiple.endsWith("ing")) textColor else Color(0xFF303F9F)
                                withStyle(style = plainStyle) {
                                    append("Şimdiki Zaman Sıfat Fiili ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(presentParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (third.isNotEmpty()) {
                                val color = if (third.endsWith("s")) textColor else Color.Cyan
                                withStyle(style = plainStyle) {
                                    append("Üçüncü Tekil Şahıs Hali ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(third)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }

                            if (er.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("Karşılaştırma Derecesi $er;")
                                }
                            }
                            if (est.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("Üstünlük Derecesi $est;")
                                }
                            }
                            if (plural.isNotEmpty()) {
                                val color = if (plural.endsWith("s")) textColor else Color(0xFFD84315)
                                withStyle(style = plainStyle) {
                                    append("Çoğul Hali ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(plural)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                        }
                    )

                }
            }
            if(!searching){
                Divider(Modifier.padding(start = 50.dp))
            }
        }


    }

}

/**
 * İngilizce tanım bileşeni
 */
@Composable
fun Definition(
    word: Word,
    definitionVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    fontSize: TextUnit
) {
    if (definitionVisible && (isChangeVideoBounds || !isPlaying )) {
        // Satır sayısını hesapla, kaydırma çubuğunun gösterilip gösterilmeyeceğine karar vermek için kullanılır
        // Orijinal dize uzunluğundan satır sonu karakterleri çıkarılmış uzunluğu çıkararak satır sonu karakterlerinin sayısını al
        val rows = word.definition.length - word.definition.replace("\n", "").length
        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 8) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.definition,
                        )
                    }
                }
                if (rows > 8) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/**
 * Çince anlam bileşeni
 */
@Composable
fun Translation(
    translationVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    word: Word,
    fontSize: TextUnit
) {
    if (translationVisible && (isChangeVideoBounds || !isPlaying )) {
        // Satır sayısını hesapla, kaydırma çubuğunun gösterilip gösterilmeyeceğine karar vermek için kullanılır
        // Orijinal dize uzunluğundan satır sonu karakterleri çıkarılmış uzunluğu çıkararak satır sonu karakterlerinin sayısını al
        val rows = word.translation.length - word.translation.replace("\n", "").length
        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 8) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.translation,
                        )
                    }
                }
                if (rows > 8) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/**
 * Örnek cümle bileşeni
 */
@Composable
fun Sentences(
    sentencesVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    word: Word,
    fontSize: TextUnit
) {
    if (sentencesVisible && word.pos.isNotEmpty() && (isChangeVideoBounds || !isPlaying )) {
        // Satır sayısını hesapla, kaydırma çubuğunun gösterilip gösterilmeyeceğine karar vermek için kullanılır
        // Orijinal dize uzunluğundan satır sonu karakterleri çıkarılmış uzunluğu çıkararak satır sonu karakterlerinin sayısını al
        val rows = word.pos.length - word.pos.replace("\n", "").length

        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(180.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 5) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.pos,
                        )
                    }
                }
                if (rows > 5) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }
            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/** Altyazı listesi bileşeni
 * @param captionsVisible altyazıların görünürlüğü
 * @param playTripleMap gösterilecek altyazılar. Map türü parametre açıklaması:
 * - Map'in Int'i      -> index, esas olarak altyazıları silmek ve zaman çizelgesini güncellemek için kullanılır
 * - Triple'ın Caption'ı  -> caption.content giriş ve okuma için, caption.start ve caption.end video oynatma için
 * - Triple'ın String'i   -> altyazının karşılık geldiği video adresi
 * - Triple'ın Int'i      -> altyazının parçası
 * @param videoPlayerWindow video oynatma penceresi
 * @param isPlaying 是否正在播放视频
 * @param volume 音量
 * @param setIsPlaying 设置是否正在播放视频播放的回调
 * @param word 单词
 * @param bounds 视频播放窗口的位置
 * @param textFieldValueList 用户输入的字幕列表
 * @param typingResultMap 用户输入字幕的结果 Map
 * @param putTypingResultMap 添加当前的字幕到结果Map
 * @param checkTyping 检查用户输入的回调
 * @param playKeySound 当用户输入字幕时播放敲击键盘音效的回调
 * @param modifier 修改器
 */
@ExperimentalComposeUiApi
@Composable
fun Captions(
    captionsVisible: Boolean,
    playTripleMap: Map<Int, Triple<Caption, String, Int>>,
    videoPlayerWindow: JFrame,
    videoPlayerComponent: Component,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    plyingIndex: Int,
    setPlayingIndex: (Int) -> Unit,
    volume: Float,
    word: Word,
    bounds: Rectangle,
    textFieldValueList: List<String>,
    typingResultMap: Map<Int, MutableList<Pair<Char, Boolean>>>,
    putTypingResultMap: (Int, MutableList<Pair<Char, Boolean>>) -> Unit,
    checkTyping: (Int, String, String) -> Unit,
    playKeySound: () -> Unit,
    modifier: Modifier,
    focusRequesterList:List<FocusRequester>,
    jumpToWord: () -> Unit,
    externalVisible:Boolean,
    openSearch: () -> Unit,
    fontSize: TextUnit,
    resetVideoBounds :() ->  Rectangle,
    isVideoBoundsChanged:Boolean,
    setIsChangeBounds:(Boolean) -> Unit = {},
    isWriteSubtitles:Boolean,
    vocabularyDir:File
) {
    if (captionsVisible) {
        val horizontalArrangement = if (isPlaying && !isVideoBoundsChanged) Arrangement.Center else Arrangement.Start
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            Column {
                val scope = rememberCoroutineScope()
                playTripleMap.forEach { (index, playTriple) ->
                    var captionContent = playTriple.first.content
                    if(!isWriteSubtitles){
                        if (captionContent.endsWith("\r\n")) {
                            captionContent = captionContent.dropLast(2)
                        } else if (captionContent.endsWith("\n")) {
                            captionContent = captionContent.dropLast(1)
                        }
                    }else{
                        if (captionContent.contains("\r\n")) {
                            captionContent = captionContent.replace("\r\n", " ")
                        } else if (captionContent.contains("\n")) {
                            captionContent = captionContent.replace("\n", " ")
                        }
                    }
                    // 当前的字幕是否获得焦点
                    var focused by remember { mutableStateOf(false) }
                    var textFieldValue = textFieldValueList[index]
                    if(!isWriteSubtitles){
                        textFieldValue = captionContent
                    }
                    var typingResult = typingResultMap[index]
                    if (typingResult == null) {
                        typingResult = mutableListOf()
                        putTypingResultMap(index, typingResult)
                    }
                    var isPlayFailed by remember { mutableStateOf(false) }
                    var failedMessage by remember { mutableStateOf("") }
                    val playCurrentCaption:()-> Unit = {
                        if (!isPlaying) {
                            scope.launch {
                                play(
                                    window = videoPlayerWindow,
                                    setIsPlaying = { setIsPlaying(it) },
                                    volume = volume,
                                    playTriple = playTriple,
                                    videoPlayerComponent = videoPlayerComponent,
                                    bounds = bounds,
                                    onFailed = { message ->
                                        isPlayFailed = true
                                        failedMessage = message
                                    },
                                    externalSubtitlesVisible = externalVisible,
                                    resetVideoBounds = resetVideoBounds,
                                    isVideoBoundsChanged = isVideoBoundsChanged,
                                    setIsVideoBoundsChanged = setIsChangeBounds,
                                    vocabularyDir = vocabularyDir,
                                    updatePlayingIndex = { setPlayingIndex(index) }
                                )
                            }

                        }
                        if(isWriteSubtitles || focused){
                            focusRequesterList[index].requestFocus()
                        }else{
                            jumpToWord()
                        }
                    }
                    var selectable by remember { mutableStateOf(false) }
                    val focusMoveUp:() -> Unit = {
                        if(index == 0){
                            jumpToWord()
                        }else{
                            focusRequesterList[index-1].requestFocus()
                        }
                    }
                    val focusMoveDown:() -> Unit = {
                        if(index<2 && index + 1 < playTripleMap.size){
                            focusRequesterList[index+1].requestFocus()
                        }
                    }
                    val captionKeyEvent:(KeyEvent) -> Boolean = {
                        when {
                            (it.type == KeyEventType.KeyDown
                                    && it.key != Key.ShiftRight
                                    && it.key != Key.ShiftLeft
                                    && it.key != Key.CtrlRight
                                    && it.key != Key.CtrlLeft
                                    ) -> {
                                scope.launch { playKeySound() }
                                true
                            }
                            (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                scope.launch { selectable = !selectable }
                                true
                            }
                            (it.key == Key.Tab && it.type == KeyEventType.KeyUp) -> {
                                scope.launch {  playCurrentCaption() }
                                true
                            }
                            (it.key == Key.DirectionDown && !it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            (it.key == Key.DirectionUp && !it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (it.isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (it.isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            else -> false
                        }
                    }

                    Caption(
                        isPlaying = isPlaying,
                        isWriteSubtitles = isWriteSubtitles,
                        captionContent = captionContent,
                        textFieldValue = textFieldValue,
                        typingResult = typingResult,
                        checkTyping = { editIndex, input, editContent ->
                            checkTyping(editIndex, input, editContent)
                        },
                        index = index,
                        playingIndex = plyingIndex,
                        focusRequester = focusRequesterList[index],
                        focused = focused,
                        focusChanged = { focused = it },
                        playCurrentCaption = {playCurrentCaption()},
                        captionKeyEvent = {captionKeyEvent(it)},
                        selectable = selectable,
                        setSelectable = {selectable = it},
                        resetPlayState = {isPlayFailed = false },
                        isPlayFailed = isPlayFailed,
                        failedMessage = failedMessage,
                        openSearch = {openSearch()},
                        fontSize = fontSize
                    )
                }

            }
        }
        if ((!isPlaying || isVideoBoundsChanged) && (word.captions.isNotEmpty() || word.externalCaptions.isNotEmpty()))
            Divider(Modifier.padding(start = 50.dp))
    }
}

fun replaceSeparator(path:String): String {
    val absPath = if (isWindows()) {
        path.replace('/', '\\')
    } else {
        path.replace('\\', '/')
    }
    return absPath
}

/**
 * Altyazıları al
 * @return Map türü parametre açıklaması:
 * Int      -> index, esas olarak altyazıları silmek ve zaman çizelgesini güncellemek için kullanılır
 * - Triple'ın Caption'ı  -> caption.content giriş ve okuma için, caption.start ve caption.end video oynatma için
 * - Triple'ın String'i   -> altyazının karşılık geldiği video adresi
 * - Triple'ın Int'i      -> altyazının parçası
 */
fun getPlayTripleMap(
    vocabularyType: VocabularyType,
    subtitlesTrackId: Int,
    relateVideoPath:String,
    word: Word
): MutableMap<Int, Triple<Caption, String, Int>> {

    val playTripleMap = mutableMapOf<Int, Triple<Caption, String, Int>>()
    if (vocabularyType == VocabularyType.DOCUMENT) {
        if (word.externalCaptions.isNotEmpty()) {
            word.externalCaptions.forEachIndexed { index, externalCaption ->
                val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                val playTriple =
                    Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
                playTripleMap[index] = playTriple
            }
        }
    } else {
        if (word.captions.isNotEmpty()) {
            word.captions.forEachIndexed { index, caption ->
                val playTriple =
                    Triple(caption, relateVideoPath, subtitlesTrackId)
                playTripleMap[index] = playTriple
            }

        }
    }
    return playTripleMap
}

fun secondsToString(seconds: Double): String {
    val duration = Duration.ofMillis((seconds * 1000).toLong())
    return String.format(
        "%02d:%02d:%02d.%03d",
        duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart()
    )
}

/**
 * Altyazı bileşeni
 * @param isPlaying oynatılıyor mu
 * @param isWriteSubtitles altyazılar kopyalanıyor mu
 * @param captionContent altyazının içeriği
 * @param textFieldValue girilen altyazı
 * @param typingResult altyazı giriş sonucu
 * @param checkTyping altyazı girildikten sonra çağrılan geri arama
 * @param index mevcut altyazının dizini
 * @param playingIndex oynatılan altyazının dizini
 * @param focusRequester odak isteyicisi
 * @param focused odakta mı
 * @param focusChanged odak değişikliklerini işleme fonksiyonu
 * @param playCurrentCaption mevcut altyazıyı oynatma fonksiyonu
 * @param captionKeyEvent mevcut altyazı için kısayol tuşlarını işleme fonksiyonu
 * @param selectable kopyalamak için seçilebilir mi
 * @param setSelectable seçilebilir olup olmadığını ayarlama
 * @param isPlayFailed yol hatalı mı
 */
@OptIn(
    ExperimentalFoundationApi::class,
)
@Composable
fun Caption(
    isPlaying: Boolean,
    isWriteSubtitles: Boolean,
    captionContent: String,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (Int, String, String) -> Unit,
    index: Int,
    playingIndex: Int,
    focusRequester:FocusRequester,
    focused: Boolean,
    focusChanged:(Boolean) -> Unit,
    playCurrentCaption:()-> Unit,
    captionKeyEvent:(KeyEvent) -> Boolean,
    selectable:Boolean,
    setSelectable:(Boolean) -> Unit,
    isPlayFailed:Boolean,
    resetPlayState:() -> Unit,
    failedMessage:String,
    openSearch: () -> Unit,
    fontSize: TextUnit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        // 字幕的行数
        val row = if(isWriteSubtitles) 1 else captionContent.split("\n").size
        val rowHeight = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                24.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                20.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.subtitle1.fontSize -> {
                16.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.subtitle2.fontSize -> {
                14.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.body1.fontSize -> {
                16.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.body2.fontSize -> {
                14.dp * 2 * row + 4.dp
            }
            else -> 16.dp * 2 * row + 4.dp
        }
        val background = if(focused && !isWriteSubtitles) MaterialTheme.colors.primary.copy(alpha = 0.05f) else MaterialTheme.colors.background
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(rowHeight).width(IntrinsicSize.Max).background(background)
        ) {
            val dropMenuFocusRequester = remember { FocusRequester() }
            Box(Modifier.width(IntrinsicSize.Max)) {
                val textHeight = rowHeight -4.dp
                CustomTextMenuProvider {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            checkTyping(index, input, captionContent)
                        },
                        singleLine = isWriteSubtitles,
                        readOnly = !isWriteSubtitles,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = LocalTextStyle.current.copy(
                            color = if(focused && !isWriteSubtitles) MaterialTheme.colors.primary else  MaterialTheme.colors.onBackground,
                            fontSize = fontSize
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(textHeight)
                            .align(Alignment.CenterStart)
                            .focusRequester(focusRequester)
                            .onFocusChanged {focusChanged(it.isFocused)}
                            .onKeyEvent { captionKeyEvent(it) }
                    )
                }

                if(isWriteSubtitles){
                    Text(
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart).height(textHeight),
                        overflow = TextOverflow.Ellipsis,
                        text = buildAnnotatedString(captionContent, typingResult, fontSize)
                    )
                }


                DropdownMenu(
                    expanded = selectable,
                    onDismissRequest = { setSelectable(false) },
                    offset = DpOffset(0.dp, (if(isWriteSubtitles)-30 else -70).dp)
                ) {
                    // Bir kontrol ekleyin, altyazının karakter uzunluğunu kontrol edin, bazı altyazılar makine tarafından oluşturulur, bir bölümde çok fazla altyazı olabilir, sınırı aşabilir ve programın çökmesine neden olabilir.
                    val content = if(captionContent.length>400){
                       captionContent.substring(0,400)
                    }else captionContent

                    BasicTextField(
                        value = content,
                        onValueChange = {},
                        singleLine = isWriteSubtitles,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle =  LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                            fontSize = fontSize,
                        ),
                        modifier = Modifier.focusable()
                            .focusRequester(dropMenuFocusRequester)
                            .onKeyEvent {
                                if (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                    scope.launch { setSelectable(!selectable) }
                                    true
                                }else if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
                                    scope.launch { openSearch() }
                                    true
                                } else false
                            }
                    )
                    LaunchedEffect(Unit) {
                        dropMenuFocusRequester.requestFocus()
                    }

                }
            }

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val shift = if (isMacOS()) "⇧" else "Shift"
                        val text: Any = when (index) {
                            0 -> "Oynat $ctrl+$shift+Z"
                            1 -> "Oynat $ctrl+$shift+X"
                            2 -> "Oynat $ctrl+$shift+C"
                            else -> println("Altyazı sayısı aralık dışında")
                        }
                        Text(text = text.toString(), modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.TopCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = {
                    playCurrentCaption()
                },
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    val tint = if(isPlaying && playingIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = tint
                    )
                }
            }
            if (isPlayFailed) {
                Text(failedMessage, color = Color.Red)
                Timer("Durumu Geri Yükle", false).schedule(2000) {
                    resetPlayState()
                }
            }
        }
    }


}

@Composable
fun buildAnnotatedString(
    captionContent:String,
    typingResult:List<Pair<Char, Boolean>>,
    fontSize: TextUnit,
):AnnotatedString{
    return buildAnnotatedString {
        typingResult.forEach { (char, correct) ->
            if (correct) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colors.primary,
                        fontSize = fontSize,
                        letterSpacing = LocalTextStyle.current.letterSpacing,
                        fontFamily = LocalTextStyle.current.fontFamily,
                    )
                ) {
                    append(char)
                }
            } else {
                withStyle(
                    style = SpanStyle(
                        color = Color.Red,
                        fontSize = fontSize,
                        letterSpacing = LocalTextStyle.current.letterSpacing,
                        fontFamily = LocalTextStyle.current.fontFamily,
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

        if (!(typingResult.isNotEmpty() && captionContent.length < typingResult.size)) {
            var remainChars = captionContent.substring(typingResult.size)
            // Bir kontrol ekleyin, altyazının karakter uzunluğunu kontrol edin, bazı altyazılar makine tarafından oluşturulur, bir bölümde çok fazla altyazı olabilir, sınırı aşabilir ve programın çökmesine neden olabilir.
            if (remainChars.length > 400) {
                remainChars = remainChars.substring(0, 400)
            }

            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = fontSize,
                    letterSpacing = LocalTextStyle.current.letterSpacing,
                    fontFamily = LocalTextStyle.current.fontFamily,
                )
            ) {
                append(remainChars)
            }
        }

    }
}

/** Silme düğmesi*/
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun DeleteButton(onClick:()->Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "Kelimeyi Sil")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        val shift = if (isMacOS()) "⇧" else "Shift"
                        Text(text = " $shift + Delete ")
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = { onClick() },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                onClick()
                true
            }else false
        }) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}
/** Düzenleme düğmesi*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun EditButton(onClick: () -> Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "Düzenle", modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = {
//            showEditWordDialog = true
            onClick()
        }) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** Zor kelimeler düğmesi */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HardButton(
    contains:Boolean,
    onClick: () -> Unit,
    fontFamily:FontFamily,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    val text = if(contains) "Zor Kelimelerden Kaldır" else "Zor Kelimelere Ekle"
                    Text(text = text)
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + ")
                        Text(text = "I", fontFamily = fontFamily)
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {

        IconButton(onClick = { onClick() }) {
            val icon = if(contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            val tint = if(contains) Color(255, 152, 0) else MaterialTheme.colors.onBackground
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = tint
            )
        }
    }
}

/** Bildik kelimeler düğmesi */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FamiliarButton(
    onClick: () -> Unit,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "Bildiklerine Taşı")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + Y")
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = { onClick() },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                onClick()
                true
            }else false
        }) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** Ctrl + I kısayol tuşunu kullanarak mevcut kelimeyi zor kelimelere eklediğinizde 0.3 saniye görüntülenir ve sonra kaybolur */
@Composable
fun BookmarkButton(
    modifier: Modifier,
    contains:Boolean,
    disappear:() ->Unit
){
        IconButton(onClick = {},modifier = modifier) {
            val icon = if(contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            val tint = if(contains) Color(255, 152, 0) else MaterialTheme.colors.onBackground
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = tint,
            )
            SideEffect{
                Timer("Yer İmi Simgesini Gizle", false).schedule(300) {
                    disappear()
                }
            }
        }

}

/** Kopyala düğmesi */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CopyButton(wordValue:String){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "Kopyala")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + C")
                    }
                }

            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        val clipboardManager = LocalClipboardManager.current
        IconButton(onClick = {
            clipboardManager.setText(AnnotatedString(wordValue))
        },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                clipboardManager.setText(AnnotatedString(wordValue))
                true
            }else false
        }) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}


/**
 * @param currentWord mevcut ezberlenen kelime
 * @param index linklerin dizini
 * @return Triple<Caption, String, Int>? , video oynatıcının ihtiyaç duyduğu bilgiler
 */
fun getPayTriple(currentWord: Word, index: Int): Triple<Caption, String, Int>? {

    return if (index < currentWord.externalCaptions.size) {
        val externalCaption = currentWord.externalCaptions[index]
        val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
        Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
    } else {
        null
    }
}

/**  Sürükle ve bırak dosyalarını işleme fonksiyonunu ayarla
 *  @param window  ana pencere
 *  @param appState uygulama genel durumu
 *  @param wordScreenState kelime ezberleme arayüzü durumu
 *  @param showVideoPlayer video oynatıcıyı göster
 *  @param setVideoPath video yolunu ayarla
 *  @param setVideoVocabulary video için karşılık gelen kelime dağarcığını ayarla
 * */
@OptIn(ExperimentalSerializationApi::class)
fun setWindowTransferHandler(
    window: ComposeWindow,
    appState: AppState,
    wordScreenState: WordScreenState,
    showVideoPlayer:(Boolean) -> Unit,
    setVideoPath:(String) -> Unit,
    setVideoVocabulary:(String) -> Unit
){
    window.transferHandler = createTransferHandler(
        showWrongMessage = { message ->
            JOptionPane.showMessageDialog(window, message)
        },
        parseImportFile = {files ->
            val file = files.first()
            if (file.extension == "json") {
                if (wordScreenState.vocabularyPath != file.absolutePath) {
                    val index = appState.findVocabularyIndex(file)
                    appState.changeVocabulary(file,wordScreenState,index)
                } else {
                    JOptionPane.showMessageDialog(window, "Kelime dağarcığı zaten açık.")
                }

            } else if (file.extension == "mkv" || file.extension == "mp4") {
                showVideoPlayer(true)
                setVideoPath(file.absolutePath)
                setVideoVocabulary(wordScreenState.vocabularyPath)
            } else {
                JOptionPane.showMessageDialog(window, "Dosya formatı desteklenmiyor.")
            }
        }
    )
}