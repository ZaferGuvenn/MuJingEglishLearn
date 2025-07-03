package ui

import CustomLocalProvider
import LocalCtrl
import PlayerLocalProvider
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.movcontext.MuJing.BuildConfig
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import scrollbarStyle
import state.*
import theme.toAwt
import ui.dialog.*
import ui.edit.ChooseEditVocabulary
import ui.edit.EditVocabulary
import ui.edit.checkVocabulary
import ui.flatlaf.setupFileChooser
import ui.flatlaf.updateFlatLaf
import ui.search.Search
import ui.subtitlescreen.SubtitleScreen
import ui.subtitlescreen.SubtitlesState
import ui.subtitlescreen.rememberSubtitlesState
import ui.textscreen.TextScreen
import ui.textscreen.TextState
import ui.textscreen.rememberTextState
import ui.wordscreen.WordScreen
import ui.wordscreen.WordScreenState
import ui.wordscreen.rememberPronunciation
import ui.wordscreen.rememberWordState
import util.computeVideoBounds
import java.awt.Rectangle
import java.io.File
import javax.swing.JOptionPane


@ExperimentalFoundationApi
@ExperimentalAnimationApi
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalSerializationApi::class
)
@Composable
fun App(
    appState: AppState = rememberAppState(),
    playerState: PlayerState = rememberPlayerState(),
    wordState: WordScreenState = rememberWordState(),
    subtitlesState: SubtitlesState = rememberSubtitlesState(),
    textState: TextState = rememberTextState()
) {

    var showMainWindow by remember { mutableStateOf(true) }
    if (showMainWindow) {
        CustomLocalProvider{
            val audioPlayerComponent = LocalAudioPlayerComponent.current

            val close: () -> Unit = {
                showMainWindow = false
                audioPlayerComponent.mediaPlayer().release()
                appState.videoPlayerComponent.mediaPlayer().release()
            }

            val windowState = rememberWindowState(
                position = appState.global.position,
                placement = appState.global.placement,
                size = appState.global.size,
            )

            var title by remember{ mutableStateOf("") }
            Window(
                title = title,
                icon = painterResource("logo/logo.png"),
                state = windowState,
                onCloseRequest = {close() },
            ) {

                MaterialTheme(colors = appState.colors) {
                    // 和 Compose UI 有关的 LocalProvider 需要放在 MaterialTheme 里面,不然无效。
                    CompositionLocalProvider(
                        LocalScrollbarStyle provides scrollbarStyle(),
                    ){
                        appState.global.wordFontSize = computeFontSize(appState.global.wordTextStyle)
                        appState.global.detailFontSize = computeFontSize(appState.global.detailTextStyle)
                        WindowMenuBar(
                            window = window,
                            appState = appState,
                            wordScreenState = wordState,
                            close = {close()}
                        )
                        MenuDialogs(appState)
                        if(appState.searching){
                            Search(
                                appState = appState,
                                wordScreenState = wordState,
                                vocabulary = wordState.vocabulary,
                            )
                        }
                        when (appState.global.type) {
                            ScreenType.WORD -> {
                                title = computeTitle(wordState.vocabularyName,wordState.vocabulary.wordList.isNotEmpty())

                                // 显示器缩放
                                val density = LocalDensity.current.density
                                // 视频播放器的位置，大小
                                val videoBounds by remember (windowState,appState.openSettings,density){
                                    derivedStateOf {
                                        if(wordState.isChangeVideoBounds){
                                            Rectangle(wordState.playerLocationX,wordState.playerLocationY,wordState.playerWidth,wordState.playerHeight)
                                        }else{
                                            computeVideoBounds(windowState, appState.openSettings,density)
                                        }
                                    }
                                }

                                val resetVideoBounds :() -> Rectangle ={
                                    val bounds = computeVideoBounds(windowState, appState.openSettings,density)
                                    wordState.isChangeVideoBounds = false
                                    appState.videoPlayerWindow.size =bounds.size
                                    appState.videoPlayerWindow.location = bounds.location
                                    appState.videoPlayerComponent.size = bounds.size
                                    videoBounds.location = bounds.location
                                    videoBounds.size = bounds.size
                                    wordState.changePlayerBounds(bounds)
                                    bounds
                                }
                                WordScreen(
                                    window = window,
                                    title = title,
                                    appState = appState,
                                    wordScreenState = wordState,
                                    videoBounds = videoBounds,
                                    resetVideoBounds = resetVideoBounds,
                                    showPlayer = { playerState.showPlayerWindow = it },
                                    setVideoPath = playerState.videoPathChanged,
                                    setVideoVocabulary = playerState.vocabularyPathChanged
                                )
                            }
                            ScreenType.SUBTITLES -> {
                                title = computeTitle(subtitlesState)
                                SubtitleScreen(
                                    subtitlesState = subtitlesState,
                                    globalState = appState.global,
                                    saveSubtitlesState = { subtitlesState.saveTypingSubtitlesState() },
                                    saveGlobalState = { appState.saveGlobalState() },
                                    isOpenSettings = appState.openSettings,
                                    setIsOpenSettings = { appState.openSettings = it },
                                    window = window,
                                    title = title,
                                    playerWindow = appState.videoPlayerWindow,
                                    videoVolume = appState.global.videoVolume,
                                    mediaPlayerComponent = appState.videoPlayerComponent,
                                    futureFileChooser = appState.futureFileChooser,
                                    openLoadingDialog = { appState.openLoadingDialog()},
                                    closeLoadingDialog = { appState.loadingFileChooserVisible = false },
                                    openSearch = {appState.openSearch()},
                                    showPlayer = { playerState.showPlayerWindow = it },
                                )
                            }

                            ScreenType.TEXT -> {
                                title = computeTitle(textState)
                                TextScreen(
                                    title = title,
                                    window = window,
                                    globalState = appState.global,
                                    saveGlobalState = { appState.saveGlobalState() },
                                    textState = textState,
                                    saveTextState = { textState.saveTypingTextState() },
                                    isOpenSettings = appState.openSettings,
                                    setIsOpenSettings = {appState.openSettings = it},
                                    futureFileChooser = appState.futureFileChooser,
                                    openLoadingDialog = { appState.openLoadingDialog()},
                                    closeLoadingDialog = { appState.loadingFileChooserVisible = false },
                                    openSearch = {appState.openSearch()},
                                    showVideoPlayer = { playerState.showPlayerWindow = it },
                                    setVideoPath = playerState.videoPathChanged,
                                )
                            }
                        }
                    }

                }

                //移动，或改变窗口后保存状态到磁盘
                LaunchedEffect(windowState) {
                    snapshotFlow { windowState.size }
                        .onEach{onWindowResize(windowState.size,appState)}
                        .launchIn(this)

                    snapshotFlow { windowState.placement }
                        .onEach {  onWindowPlacement(windowState.placement,appState)}
                        .launchIn(this)

                    snapshotFlow { windowState.position }
                        .onEach { onWindowRelocate(windowState.position,appState) }
                        .launchIn(this)
                }
                val scope = rememberCoroutineScope()
                /** 启动应用后，自动检查更新 */
                LaunchedEffect(Unit) {
                    if (appState.global.autoUpdate) {
                        scope.launch(Dispatchers.IO) {
                            delay(5000)
                            val result = autoDetectingUpdates(BuildConfig.APP_VERSION)
                            if (result.first && result.second != appState.global.ignoreVersion) {
                                appState.showUpdateDialog = true
                                appState.latestVersion = result.second
                                appState.releaseNote = result.third
                            }
                        }
                    }
                }
            }

        }
    }

    if(playerState.showPlayerWindow){

        MaterialTheme(colors = appState.colors) {
            // 和 Compose UI 有关的 LocalProvider 需要放在 MaterialTheme 里面,不然无效。
            PlayerLocalProvider {
                val pronunciation = rememberPronunciation()
                Player(
                    playerState = playerState,
                    audioSet = appState.localAudioSet,
                    pronunciation = pronunciation,
                    audioVolume = appState.global.audioVolume,
                    videoVolume = appState.global.videoVolume,
                    videoVolumeChanged = {
                        appState.global.videoVolume = it
                        appState.saveGlobalState()
                    },
                )
            }


        }


    }

    var showEditVocabulary by remember { mutableStateOf(false) }
    var chosenPath by remember { mutableStateOf("") }
    if(appState.editVocabulary){
        ChooseEditVocabulary(
            close = {appState.editVocabulary = false},
            recentList = appState.recentList,
            removeRecentItem = {appState.removeRecentItem(it)},
            openEditVocabulary = {
                chosenPath = it
                showEditVocabulary = true
                appState.editVocabulary = false
                },
            colors = appState.colors,
        )
    }
    if (appState.newVocabulary) {
        NewVocabularyDialog(
            close = { appState.newVocabulary = false },
            setEditPath = {
                chosenPath = it
                showEditVocabulary = true
            },
            colors = appState.colors,
        )
    }
    if(showEditVocabulary){
        val valid by remember { mutableStateOf(checkVocabulary(chosenPath)) }
        if(valid){
            EditVocabulary(
                close = {showEditVocabulary = false},
                vocabularyPath = chosenPath,
                isDarkTheme = appState.global.isDarkTheme,
                updateFlatLaf = {
                    updateFlatLaf(
                        darkTheme = appState.global.isDarkTheme,
                        isFollowSystemTheme = appState.global.isFollowSystemTheme,
                        background = appState.global.backgroundColor.toAwt(),
                        onBackground = appState.global.onBackgroundColor.toAwt()
                    )
                }
            )
        }else{
            showEditVocabulary = false
        }

    }

    // 改变主题后，更新菜单栏、标题栏的样式
    LaunchedEffect(appState.global.isDarkTheme,appState.global.isFollowSystemTheme){
        updateFlatLaf(
            darkTheme = appState.global.isDarkTheme,
            isFollowSystemTheme = appState.global.isFollowSystemTheme,
            background = appState.global.backgroundColor.toAwt(),
            onBackground = appState.global.onBackgroundColor.toAwt()
        )
        appState.futureFileChooser = setupFileChooser()
    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun onWindowResize(size: DpSize, state: AppState) {
    state.global.size = size
    state.saveGlobalState()
}

@OptIn(ExperimentalSerializationApi::class)
private fun onWindowRelocate(position: WindowPosition, state: AppState) {
    state.global.position = position as WindowPosition.Absolute
    state.saveGlobalState()
}

@OptIn(ExperimentalSerializationApi::class)
private fun onWindowPlacement(placement: WindowPlacement, state: AppState){
    state.global.placement = placement
    state.saveGlobalState()
}


private fun computeTitle(
    name:String,
    isNotEmpty:Boolean
) :String{
    return if (isNotEmpty) {
        when (name) {
            "FamiliarVocabulary" -> {
                "Tanıdık Kelimeler" // "熟悉词库" -> "Tanıdık Kelimeler"
            }
            "HardVocabulary" -> {
                "Zor Kelimeler" // "困难词库" -> "Zor Kelimeler"
            }
            else -> name
        }
    } else {
        "Lütfen Kelime Listesi Seçin" // "请选择词库" -> "Lütfen Kelime Listesi Seçin"
    }
}
private fun computeTitle(subtitlesState: SubtitlesState) :String{
    val mediaPath = subtitlesState.mediaPath
    return if(mediaPath.isNotEmpty()){
        try{
            val fileName = File(mediaPath).nameWithoutExtension
            fileName + " - " + subtitlesState.trackDescription // trackDescription'ın kendisi de çevrilecek bir UI elemanı olabilir, şimdilik bırakıyorum.
        }catch (exception:Exception){
            "Altyazı Tarayıcısı" // "字幕浏览器" -> "Altyazı Tarayıcısı"
        }

    }else{
        "Altyazı Tarayıcısı" // "字幕浏览器" -> "Altyazı Tarayıcısı"
    }
}

private fun computeTitle(textState: TextState) :String{
    val textPath = textState.textPath
    return if(textPath.isNotEmpty()){
        try{
            val fileName = File(textPath).nameWithoutExtension
            fileName
        }catch (exception :Exception){
            "Metin Kopyala" // "抄写文本" -> "Metin Kopyala"
        }

    }else {
        "Metin Kopyala" // "抄写文本" -> "Metin Kopyala"
    }
}

/**
 * 菜单栏
 */
@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun FrameWindowScope.WindowMenuBar(
    window: ComposeWindow,
    appState: AppState,
    wordScreenState: WordScreenState,
    close: () -> Unit,
) = MenuBar {
    Menu("Kelime Listesi(K)", mnemonic = 'K') { // "词库(V)" -> "Kelime Listesi(K)"
        var showFilePicker by remember {mutableStateOf(false)}
        Item("Kelime Listesi Aç(A)", mnemonic = 'A') { showFilePicker = true } // "打开词库(O)" -> "Kelime Listesi Aç(A)"
        val extensions = if(isMacOS()) listOf("public.json") else listOf("json")
        FilePicker(
            show = showFilePicker,
            fileExtensions = extensions,
            initialDirectory = ""){pickFile ->
            if(pickFile != null){
                if(pickFile.path.isNotEmpty()){
                    val file = File(pickFile.path)
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
            }

            showFilePicker = false
        }
        Menu("Son Kullanılanlar(S)",enabled = appState.recentList.isNotEmpty(), mnemonic = 'S') { // "打开最近词库(R)" -> "Son Kullanılanlar(S)"
            for (i in 0 until appState.recentList.size){
                val recentItem = appState.recentList.getOrNull(i)
                if(recentItem!= null){
                    Item(text = recentItem.name, onClick = { // recentItem.name zaten Türkçe olmalı (JSON'dan gelecek)
                        val recentFile = File(recentItem.path)
                        if (recentFile.exists()) {
                            val changed = appState.changeVocabulary(recentFile,wordScreenState, recentItem.index)
                            if(changed){
                                appState.global.type = ScreenType.WORD
                                appState.saveGlobalState()
                            }else{
                                appState.removeRecentItem(recentItem)
                            }

                        } else {
                            appState.removeRecentItem(recentItem)
                            JOptionPane.showMessageDialog(window, "Dosya yolu hatası:\n${recentItem.path}") // "文件地址错误：\n" -> "Dosya yolu hatası:\n"
                        }

                        appState.loadingFileChooserVisible = false

                    })

                }
            }
        }
        Item("Yeni Kelime Listesi(Y)", mnemonic = 'Y', onClick = { // "新建词库(N)" -> "Yeni Kelime Listesi(Y)"
            appState.newVocabulary = true
        })
        Item("Kelime Listesini Düzenle(D)", mnemonic = 'D', onClick = { // "编辑词库(E)" -> "Kelime Listesini Düzenle(D)"
            appState.editVocabulary = true
        })
        Separator()
        var showBuiltInVocabulary by remember{mutableStateOf(false)}
        Item("Dahili Kelime Listesi Seç(B)", mnemonic = 'B', onClick = {showBuiltInVocabulary = true}) // "选择内置词库(B)" -> "Dahili Kelime Listesi Seç(B)" (B harfi "Built-in" için kalabilir veya "D" yapılabilir)
        BuiltInVocabularyDialog(
            show = showBuiltInVocabulary,
            close = {showBuiltInVocabulary = false},
            futureFileChooser = appState.futureFileChooser
        )
        Item("Tanıdık Kelimeler(T)", mnemonic = 'T',onClick = { // "熟悉词库(I)" -> "Tanıdık Kelimeler(T)"
            val file = getFamiliarVocabularyFile()
            if(file.exists()){
                val vocabulary =loadVocabulary(file.absolutePath)
                if(vocabulary.wordList.isEmpty()){
                    JOptionPane.showMessageDialog(window,"Tanıdık kelime listesinde henüz kelime yok") // "熟悉词库现在还没有单词" -> "Tanıdık kelime listesinde henüz kelime yok"
                }else{
                    val changed = appState.changeVocabulary(file, wordScreenState,wordScreenState.familiarVocabularyIndex)
                    if(changed){
                        appState.global.type = ScreenType.WORD
                        appState.saveGlobalState()
                    }
                }

            }else{
                JOptionPane.showMessageDialog(window,"Tanıdık kelime listesinde henüz kelime yok") // "熟悉词库现在还没有单词" -> "Tanıdık kelime listesinde henüz kelime yok"
            }
        })
        Item("Zor Kelimeler(Z)", enabled = appState.hardVocabulary.wordList.isNotEmpty(), mnemonic = 'Z',onClick = { // "困难词库(K)" -> "Zor Kelimeler(Z)"
            val file = getHardVocabularyFile()
            val changed = appState.changeVocabulary(file, wordScreenState,wordScreenState.hardVocabularyIndex)
            if(changed){
                appState.global.type = ScreenType.WORD
                appState.saveGlobalState()
            }

        })

        Separator()
        Item("Kelime Listelerini Birleştir(M)", mnemonic = 'M', onClick = { // "合并词库(M)" -> "Kelime Listelerini Birleştir(M)" (M harfi "Merge" için kalabilir)
            appState.mergeVocabulary = true
        })
        Item("Kelime Listesini Filtrele(F)", mnemonic = 'F', onClick = { // "过滤词库(F)" -> "Kelime Listesini Filtrele(F)"
            appState.filterVocabulary = true
        })
        var matchVocabulary by remember{ mutableStateOf(false) }
        Item("Kelime Listesi Eşleştir(E)", mnemonic = 'E', onClick = { // "匹配词库(P)" -> "Kelime Listesi Eşleştir(E)"
            matchVocabulary = true
        })
        if(matchVocabulary){
            MatchVocabularyDialog(
                futureFileChooser = appState.futureFileChooser,
                close = {matchVocabulary = false}
            )
        }

        var showLinkVocabulary by remember { mutableStateOf(false) }
        if (showLinkVocabulary) {
            LinkVocabularyDialog(
                appState = appState,
                close = {
                    showLinkVocabulary = false
                }
            )
        }

        Item(
            "Altyazı Kelime Listesini Bağla(L)", mnemonic = 'L', // "链接字幕词库(L)" -> "Altyazı Kelime Listesini Bağla(L)" (L harfi "Link" için kalabilir)
            onClick = { showLinkVocabulary = true },
        )
        Item("Tanıdık Listeye Aktar(A)", mnemonic = 'A', onClick = { // "导入词库到熟悉词库(I)" -> "Tanıdık Listeye Aktar(A)" (I yerine A "Aktar")
            appState.importFamiliarVocabulary = true
        })

        Separator()
        var showWordFrequency by remember { mutableStateOf(false) }
        Item("Kelime Sıklığına Göre Oluştur(S)", mnemonic = 'S', onClick = {showWordFrequency = true }) // "根据词频生成词库(C)" -> "Kelime Sıklığına Göre Oluştur(S)"
        if(showWordFrequency){
            WordFrequencyDialog(
                futureFileChooser = appState.futureFileChooser,
                saveToRecentList = { name, path ->
                    appState.saveToRecentList(name, path,0)
                },
                close = {showWordFrequency = false}
            )
        }
        Item("Belgeden Kelime Listesi Oluştur(B)", mnemonic = 'B', onClick = { // "用文档生成词库(D)" -> "Belgeden Kelime Listesi Oluştur(B)"
            appState.generateVocabularyFromDocument = true
        })
        Item("Altyazıdan Kelime Listesi Oluştur(A)", mnemonic = 'A', onClick = { // "用字幕生成词库(Z)" -> "Altyazıdan Kelime Listesi Oluştur(A)"
            appState.generateVocabularyFromSubtitles = true
        })
        Item("Videodan Kelime Listesi Oluştur(V)", mnemonic = 'V', onClick = { // "用视频生成词库(V)" -> "Videodan Kelime Listesi Oluştur(V)"
            appState.generateVocabularyFromVideo = true
        })
        Separator()
        var showSettingsDialog by remember { mutableStateOf(false) }
        Item("Ayarlar(A)", mnemonic = 'A', onClick = { showSettingsDialog = true }) // "设置(S)" -> "Ayarlar(A)"
        if(showSettingsDialog){
            SettingsDialog(
                close = {showSettingsDialog = false},
                state = appState,
                wordScreenState = wordScreenState
            )
        }
        if(isWindows()){
            Separator()
            Item("Çıkış(Ç)", mnemonic = 'Ç', onClick = { close() }) // "退出(X)" -> "Çıkış(Ç)"
        }

    }
    Menu("Altyazılar(A)", mnemonic = 'A') { // "字幕(S)" -> "Altyazılar(A)"
        val enableTypingSubtitles = (appState.global.type != ScreenType.SUBTITLES)
        Item(
            "Altyazı Tarayıcısı(T)", mnemonic = 'T', // "字幕浏览器(T)" -> "Altyazı Tarayıcısı(T)"
            enabled = enableTypingSubtitles,
            onClick = {
                appState.global.type = ScreenType.SUBTITLES
                appState.saveGlobalState()
            },
        )

        var showLyricDialog by remember{ mutableStateOf(false) }
        if(showLyricDialog){
            LyricToSubtitlesDialog(
                close = {showLyricDialog = false},
                futureFileChooser = appState.futureFileChooser,
                openLoadingDialog = {appState.loadingFileChooserVisible = true},
                closeLoadingDialog = {appState.loadingFileChooserVisible = false}
            )
        }
        Item(
            "Şarkı Sözünü Altyazıya Dönüştür(D)",mnemonic = 'D', // "歌词转字幕(C)" -> "Şarkı Sözünü Altyazıya Dönüştür(D)"
            enabled = true,
            onClick = {showLyricDialog = true}
        )
    }
    Menu("Metin(M)", mnemonic = 'M') { // "文本(T)" -> "Metin(M)"
        val enable = appState.global.type != ScreenType.TEXT
        Item(
            "Metin Kopyala(K)", mnemonic = 'K', // "抄写文本(T)" -> "Metin Kopyala(K)"
            enabled = enable,
            onClick = {
                appState.global.type = ScreenType.TEXT
                appState.saveGlobalState()
            },
        )
        var showTextFormatDialog by remember { mutableStateOf(false) }
        if(showTextFormatDialog){
            TextFormatDialog(
                close = {showTextFormatDialog = false},
                futureFileChooser= appState.futureFileChooser,
                openLoadingDialog = {appState.loadingFileChooserVisible = true},
                closeLoadingDialog = {appState.loadingFileChooserVisible = false},
            )
        }
        Item(
            "Metin Formatlama(F)", mnemonic = 'F', // "文本格式化(F)" -> "Metin Formatlama(F)"
            onClick = { showTextFormatDialog = true },
        )
    }
    Menu("Yardım(Y)", mnemonic = 'Y') { // "帮助(H)" -> "Yardım(Y)"
        var documentWindowVisible by remember { mutableStateOf(false) }
        var currentPage by remember { mutableStateOf("features") }
        Item("Kullanım Kılavuzu(K)", mnemonic = 'K', onClick = { documentWindowVisible = true}) // "使用手册(D)" -> "Kullanım Kılavuzu(K)"
        if(documentWindowVisible){
            DocumentWindow(
                close = {documentWindowVisible = false},
                currentPage = currentPage,
                setCurrentPage = {currentPage = it}
            )
        }
        var shortcutKeyDialogVisible by remember { mutableStateOf(false) }
        Item("Kısayol Tuşları(T)", mnemonic = 'T', onClick = {shortcutKeyDialogVisible = true}) // "快捷键(K)" -> "Kısayol Tuşları(T)"
        if(shortcutKeyDialogVisible){
            ShortcutKeyDialog(close ={shortcutKeyDialogVisible = false} )
        }
        var directoryDialogVisible by remember { mutableStateOf(false) }
        Item("Özel Klasörler(O)",mnemonic = 'O', onClick = {directoryDialogVisible = true}) // "特殊文件夹(F)" -> "Özel Klasörler(O)"
        if(directoryDialogVisible){
            SpecialDirectoryDialog(close ={directoryDialogVisible = false})
        }
        var donateDialogVisible by remember { mutableStateOf(false) }
        Item("Bağış Yap(B)", onClick = { donateDialogVisible = true }) // "捐赠" -> "Bağış Yap(B)" (Mnemonic eklendi)
        if(donateDialogVisible){
            DonateDialog (
                close = {donateDialogVisible = false}
            )
        }
        Item("Güncellemeleri Kontrol Et(G)", mnemonic = 'G', onClick = { // "检查更新(U)" -> "Güncellemeleri Kontrol Et(G)"
            appState.showUpdateDialog = true
            appState.latestVersion = ""
        })
        var aboutDialogVisible by remember { mutableStateOf(false) }
        Item("Hakkında(H)", mnemonic = 'H', onClick = { aboutDialogVisible = true }) // "关于(A)" -> "Hakkında(H)"
        if (aboutDialogVisible) {
            AboutDialog(
                version = BuildConfig.APP_VERSION,
                close = { aboutDialogVisible = false }
            )
        }

    }
}

/**
 * 工具栏
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun Toolbar(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier,
    globalState: GlobalState,
    saveGlobalState:() -> Unit,
    showPlayer :(Boolean) -> Unit,
    openSearch :() -> Unit
) {

    Row (modifier = modifier.padding(top = if (isMacOS()) 30.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically){
        val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
        val scope = rememberCoroutineScope()
        Settings(
            isOpen = isOpen,
            setIsOpen = setIsOpen,
            modifier = Modifier
        )
        if(!isOpen)Divider(Modifier.width(1.dp).height(20.dp))
        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "Kelime Öğrenme", modifier = Modifier.padding(10.dp)) // "记忆单词" -> "Kelime Öğrenme"
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
                onClick = {
                    scope.launch {
                        globalState.type = ScreenType.WORD
                        saveGlobalState()
                    }
                },
                modifier = Modifier.testTag("WordButton")
            ) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (globalState.type == ScreenType.WORD) MaterialTheme.colors.primary else tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
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
                    Text(text = "Altyazı Tarayıcısı", modifier = Modifier.padding(10.dp)) // "字幕浏览器" -> "Altyazı Tarayıcısı"
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
                onClick = {
                    scope.launch {
                        globalState.type = ScreenType.SUBTITLES
                        saveGlobalState()
                    }
                },
                modifier = Modifier.testTag("SubtitlesButton")
            ) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = "Localized description",
                    tint = if (globalState.type == ScreenType.SUBTITLES) MaterialTheme.colors.primary else tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
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
                    Text(text = "Metin Kopyala", modifier = Modifier.padding(10.dp)) // "抄写文本" -> "Metin Kopyala"
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
                onClick = {
                    scope.launch {
                        globalState.type = ScreenType.TEXT
                        saveGlobalState()
                    }
                },
                modifier = Modifier.testTag("TextButton")
            ) {
                Icon(
                    Icons.Filled.Title,
                    contentDescription = "Localized description",
                    tint = if (globalState.type == ScreenType.TEXT) MaterialTheme.colors.primary else tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
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
                    Text(text = "Video Oynatıcı", modifier = Modifier.padding(10.dp)) // "视频播放器" -> "Video Oynatıcı"
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
                onClick = { showPlayer(true) },
                modifier = Modifier.testTag("PlayerButton")
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
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
                    Text(text = "Ara", modifier = Modifier.padding(10.dp)) // "搜索" -> "Ara"
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
                onClick = openSearch,
                modifier = Modifier.testTag("PlayerButton")
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

        }
        Divider(Modifier.width(1.dp).height(20.dp))
    }
}
/**
 * 设置
 */
@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun Settings(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
            val width by animateDpAsState(targetValue = if (isOpen) 217.dp else 48.dp)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .testTag("SettingsButton")
                    .width(width)
                    .shadow(
                        elevation = 0.dp,
                        shape = if (isOpen) RectangleShape else RoundedCornerShape(50)
                    )
                    .background(MaterialTheme.colors.background)
                    .clickable { setIsOpen(!isOpen) }) {

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val ctrl = LocalCtrl.current
                            Text(text = "Ayarlar $ctrl+1", modifier = Modifier.padding(10.dp)) // "设置" -> "Ayarlar"
                        }
                    },
                    delayMillis = 100,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset(30.dp,0.dp)
                    )
                ) {
                    val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
                    Icon(
                        if (isOpen) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Tune,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.clickable { setIsOpen(!isOpen) }
                            .size(48.dp, 48.dp).padding(13.dp)
                    )

                }

                if (isOpen) {
                    Divider(Modifier.height(48.dp).width(1.dp))
                }
            }
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
        }
    }
}




/**
 * 对话框
 */
@ExperimentalFoundationApi
@OptIn(ExperimentalSerializationApi::class)
@ExperimentalComposeUiApi
@Composable
fun MenuDialogs(state: AppState) {

    if (state.loadingFileChooserVisible) {
        LoadingDialog()
    }
    if (state.mergeVocabulary) {
        MergeVocabularyDialog(
            futureFileChooser = state.futureFileChooser,
            saveToRecentList = { name, path ->
                state.saveToRecentList(name, path,0)
            },
            close = { state.mergeVocabulary = false })
    }
    if (state.filterVocabulary) {
        GenerateVocabularyDialog(
            state = state,
            title = "Kelime Listesini Filtrele", // "过滤词库" -> "Kelime Listesini Filtrele"
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.importFamiliarVocabulary) {
        FamiliarDialog(
            futureFileChooser = state.futureFileChooser,
            close = { state.importFamiliarVocabulary = false }
        )
    }
    if (state.generateVocabularyFromDocument) {
        GenerateVocabularyDialog(
            state = state,
            title = "Belgeden Kelime Listesi Oluştur", // "用文档生成词库" -> "Belgeden Kelime Listesi Oluştur"
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.generateVocabularyFromSubtitles) {
        GenerateVocabularyDialog(
            state = state,
            title = "Altyazıdan Kelime Listesi Oluştur", // "用字幕生成词库" -> "Altyazıdan Kelime Listesi Oluştur"
            type = VocabularyType.SUBTITLES
        )
    }

    if (state.generateVocabularyFromVideo) {
        GenerateVocabularyDialog(
            state = state,
            title = "Videodan Kelime Listesi Oluştur", // "用视频生成词库" -> "Videodan Kelime Listesi Oluştur"
            type = VocabularyType.MKV
        )
    }

    if(state.showUpdateDialog){
        UpdateDialog(
            close = {state.showUpdateDialog = false},
            version =BuildConfig.APP_VERSION,
            autoUpdate = state.global.autoUpdate,
            setAutoUpdate = {
                state.global.autoUpdate = it
                state.saveGlobalState()
            },
            latestVersion = state.latestVersion,
            releaseNote = state.releaseNote,
            ignore = {
                state.global.ignoreVersion = it
                state.saveGlobalState()
            }
        )
    }
}


/**
 * 等待窗口
 */
@Composable
fun LoadingDialog() {
    DialogWindow(
        title = "Dosya Seçici Yükleniyor...", // "正在加载文件选择器" -> "Dosya Seçici Yükleniyor..."
        icon = painterResource("logo/logo.png"),
        onCloseRequest = {},
        undecorated = true,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(300.dp, 300.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        ) {
            Box(Modifier.width(300.dp).height(300.dp)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}
