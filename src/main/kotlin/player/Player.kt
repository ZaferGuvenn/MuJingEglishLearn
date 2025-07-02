package player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import tts.rememberAzureTTS
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import util.createTransferHandler
import util.rememberMonospace
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.swing.JOptionPane
import javax.swing.Timer
import kotlin.concurrent.schedule
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

/**
 * Video oynatıcı, kelime弹幕'larını (akan yazılarını) gösterebilir
 * Jetbrains https://github.com/JetBrains/compose-jb/issues/1800 sorununu düzelttikten sonra yeniden düzenleme yapılmalı.
 * SwingPanel artık en üstte gösterilmiyorsa da yeniden düzenleme yapılmalı.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun Player(
    playerState: PlayerState,
    audioSet: MutableSet<String>,
    pronunciation:String,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
) {

    val videoPath = playerState.videoPath
    val videoPathChanged = playerState.videoPathChanged
    val vocabulary = playerState.vocabulary
    val vocabularyPath = playerState.vocabularyPath
    val vocabularyPathChanged = playerState.vocabularyPathChanged

    val height = if (Toolkit.getDefaultToolkit().screenSize.height > 720) 854.dp else 662.dp
    val width = if (Toolkit.getDefaultToolkit().screenSize.width > 1280) 1289.dp else 1000.dp
    /** Pencere boyutu ve konumu */
    val windowState = rememberWindowState(
        size = DpSize(width, height),
        position = WindowPosition(Alignment.Center)
    )

    /** Oynatıcı boyutu ve konumu */
    val playerWindowState = rememberDialogState(
        width = width,
        height = height,
        position = WindowPosition(Alignment.Center)
    )

    /** Başlık */
    val title by remember (videoPath){
        derivedStateOf {
            if(videoPath.isEmpty()){
                "Video Oynatıcı"
            }else{
                File(videoPath).name
            }
        }
    }

    /** Videoyu gösteren pencere */
    var playerWindow by remember { mutableStateOf<ComposeDialog?>(null) }

    /** Video gösterimini kontrol eden pencere, akan yazılar bu pencerede gösterilir */
    var controlWindow by remember { mutableStateOf<ComposeDialog?>(null) }

    /** Tam ekran mı, sistemin tam ekranı kullanılırsa oynatıcı penceresi kararır */
    var isFullscreen by remember { mutableStateOf(false) }

    /** Tam ekrandan önceki konum */
    var fullscreenBeforePosition by remember { mutableStateOf(WindowPosition(0.dp,0.dp)) }

    /** Tam ekrandan önceki boyut */
    var fullscreenBeforeSize by remember{ mutableStateOf(DpSize(width, height)) }

    /** VLC video oynatma bileşeni */
    val videoPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent()) }

    /** VLC ses oynatma bileşeni */
    val audioPlayerComponent by remember{mutableStateOf(AudioPlayerComponent())}

    /** Video oynatılıyor mu */
    var isPlaying by remember { mutableStateOf(false) }

    /** Zaman ilerleme çubuğu */
    var timeProgress by remember { mutableStateOf(0f) }

    /** Mevcut zaman */
    var timeText by remember { mutableStateOf("") }

    /** Akan yazıyı sorgula */
    var searchDanmaku by remember { mutableStateOf("") }

    /** Akan yazı sayacı, akan yazıları hızlıca bulmak için kullanılır */
    var counter by remember { mutableStateOf(1) }

    /** Bu videodaki tüm akan yazılar */
    val danmakuMap by rememberDanmakuMap(videoPath, vocabularyPath,vocabulary)

    /** Gösterilmekte olan akan yazı, sayısal konumlandırma */
    val showingDanmakuNum = remember { mutableStateMapOf<Int, DanmakuItem>() }

    /** Gösterilmekte olan akan yazı, kelime konumlandırma */
    val showingDanmakuWord = remember { mutableStateMapOf<String, DanmakuItem>() }

    /** Gösterilmekte olan akan yazı listesine eklenmesi gereken akan yazılar */
    val shouldAddDanmaku = remember { mutableStateMapOf<Int, DanmakuItem>() }

    /** Genel duraklatma işlemi, örneğin boşluk tuşu, videoya çift tıklayarak tetiklenen duraklatma.
     * Bu şekilde duraklatma tetiklendikten sonra, birden fazla akan yazının açıklaması görüntülenebilir, oynatma işlevi tetiklenmez */
    var isNormalPause by remember { mutableStateOf(false) }

    /** Oynatıcı kontrol alanının görünürlüğü */
    var controlBoxVisible by remember { mutableStateOf(false) }
    var timeSliderPress by remember { mutableStateOf(false) }
    var audioSliderPress by remember { mutableStateOf(false) }
    var playerCursor by remember{ mutableStateOf(PointerIcon.Default) }
    /** Ayarlar menüsünü genişlet */
    var settingsExpanded by remember { mutableStateOf(false) }

    var showSubtitleMenu by remember{mutableStateOf(false)}

    /** Akan yazının sağdan sola gitmesi için gereken süre, milisaniye cinsinden */
    var widthDuration by remember { mutableStateOf(playerWindowState.size.width.value.div(3).times(30).toInt()) }

    /** Eylem dinleyicisinin her seferinde silmesi gereken akan yazı listesi */
    val removedList = remember { mutableStateListOf<DanmakuItem>() }

    /** Kelime ayrıntıları gösteriliyor */
    var showingDetail by remember { mutableStateOf(false) }

    /** Sağ tıklama menüsünü göster */
    var showDropdownMenu by remember { mutableStateOf(false) }

    /** Video dosyası seçicisini göster */
    var showFilePicker by remember {mutableStateOf(false)}

    /** Kelime dağarcığı dosyası seçicisini göster */
    var showVocabularyPicker by remember {mutableStateOf(false)}

    /** Altyazı seçicisini göster */
    var showSubtitlePicker by remember{mutableStateOf(false)}

    /** Desteklenen video türleri */
    val videoFormatList = remember{ mutableStateListOf("mp4","mkv") }

    /** Altyazı listesi */
    val subtitleTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** Ses Parçası Listesi */
    val audioTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** Mevcut gösterilen altyazı parçası */
    var currentSubtitleTrack by remember{mutableStateOf(0)}

    /** Mevcut oynatılan ses parçası */
    var currentAudioTrack by remember{mutableStateOf(0)}

    var hideControlBoxTask :TimerTask? by remember{ mutableStateOf(null)}
    val azureTTS = rememberAzureTTS()
    /** Akan yazıların sağdan sola hareket etmesini sağlayan zamanlayıcı */
    val danmakuTimer by remember {
        mutableStateOf(
            Timer(30) {
                if(playerState.danmakuVisible){
                    // showingDanmakuWord ve showingDanmakuNum değerleri aynıdır.
                    val showingList = showingDanmakuNum.values.toList()
                    for (i in showingList.indices) {
                        val danmakuItem = showingList.getOrNull(i)
                        if ((danmakuItem != null) && !danmakuItem.isPause) {
                            if (danmakuItem.position.x > -30) {
                                danmakuItem.position = danmakuItem.position.copy(x = danmakuItem.position.x - 3)
                            } else {
                                danmakuItem.show = false
                                removedList.add(danmakuItem)
                            }
                        }
                    }
                    removedList.forEach { danmakuItem ->
                        showingDanmakuNum.remove(danmakuItem.sequence)
                        showingDanmakuWord.remove(danmakuItem.content)
                    }
                    removedList.clear()
                    shouldAddDanmaku.forEach{(sequence,danmakuItem) ->
                        showingDanmakuNum.putIfAbsent(sequence,danmakuItem)
                        showingDanmakuWord.putIfAbsent(danmakuItem.content,danmakuItem)
                    }
                    shouldAddDanmaku.clear()
                }

            }
        )
    }

    /** Pencereyi kapat */
    val closeWindow: () -> Unit = {
        danmakuTimer.stop()
        playerState.closePlayerWindow()
    }

    /** Oynat */
    val play: () -> Unit = {
        if (isPlaying) {
            danmakuTimer.stop()
            isPlaying = false
            videoPlayerComponent.mediaPlayer().controls().pause()
        } else {
            danmakuTimer.restart()
            isPlaying = true
            videoPlayerComponent.mediaPlayer().controls().play()
        }
    }

    /** Manuel tetiklenen duraklatma, buna karşılık fareyle akan yazıyı hareket ettirerek tetiklenen otomatik duraklatma ve hızlı konumlandırma ile tetiklenen otomatik duraklatma vardır.*/
    val normalPause: () -> Unit = {
        isNormalPause = !isNormalPause
    }

    /** Akan yazıları temizle */
    val cleanDanmaku: () -> Unit = {
        showingDanmakuNum.clear()
        removedList.clear()
        shouldAddDanmaku.clear()
    }

    /** Kelime telaffuzunu oynat */
    val playAudio:(String) -> Unit = { word ->
        val audioPath = getAudioPath(
            word = word,
            audioSet = audioSet,
            addToAudioSet = {audioSet.add(it)},
            pronunciation = pronunciation,
            azureTTS = azureTTS,
        )
        playAudio(
            word,
            audioPath,
            pronunciation =  pronunciation,
            audioVolume,
            audioPlayerComponent,
            changePlayerState = { },
        )
    }



    /** Sürüklenip bırakılan dosyaları işlemek için bu işlevi kullanın */
    val parseImportFile: (List<File>) -> Unit = { files ->
        if(files.size == 1){
            val file = files.first()
            /** Sürüklenip bırakılan bir videodur.*/
            if(videoFormatList.contains(file.extension)){
                videoPathChanged(file.absolutePath)
            /** Sürüklenip bırakılan bir kelime dağarcığı olabilir.*/
            }else if(file.extension == "json"){
                vocabularyPathChanged(file.absolutePath)
            }
        }else if(files.size == 2){
            val first = files.first()
            val last = files.last()
            /** İlk dosya bir video dosyası, ikincisi bir kelime dağarcığıdır.*/
            if(videoFormatList.contains(first.extension) && last.extension == "json"){
                videoPathChanged(first.absolutePath)
                vocabularyPathChanged(last.absolutePath)
            /** İlk dosya bir kelime dağarcığı, ikincisi bir videodur.*/
            }else if(first.extension == "json" && videoFormatList.contains(last.extension)){
                vocabularyPathChanged(first.absolutePath)
                videoPathChanged(last.absolutePath)
             /** İki video sürüklenip bırakıldı, yalnızca ilk video işlenir.*/
            }else if(videoFormatList.contains(first.extension) && videoFormatList.contains(last.extension)){
                videoPathChanged(first.absolutePath)
            /** İki kelime dağarcığı sürüklenip bırakıldı, yalnızca ilk kelime dağarcığı işlenir. */
            }else if(first.extension == "json" && last.extension == "json"){
                vocabularyPathChanged(first.absolutePath)
            }
        }
    }

    val setCurrentSubtitleTrack:(Int)-> Unit = {
        currentSubtitleTrack = it
        videoPlayerComponent.mediaPlayer().subpictures().setTrack(it)
    }

    val setCurrentAudioTrack:(Int)-> Unit = {
        currentAudioTrack = it
        videoPlayerComponent.mediaPlayer().audio().setTrack(it)
    }

    val addSubtitle:(String) -> Unit = {path->
        videoPlayerComponent.mediaPlayer().subpictures().setSubTitleFile(path)
        Timer("altyaziListesiniGuncelle", false).schedule(500) {
            subtitleTrackList.clear()
            videoPlayerComponent.mediaPlayer().subpictures().trackDescriptions().forEach { trackDescription ->
                subtitleTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
            }
            val count = videoPlayerComponent.mediaPlayer().subpictures().trackCount()
            currentSubtitleTrack = count
        }

    }
    DisposableEffect(Unit){
        onDispose {
            videoPlayerComponent.mediaPlayer().release()
            audioPlayerComponent.mediaPlayer().release()
        }
    }

    Window(
        title = title,
        state = windowState,
        icon = painterResource("logo/logo.png"),
        undecorated = true,
        transparent = true,
        resizable = true,
        onCloseRequest = { closeWindow() },
    ){
        /** Simge durumuna küçült */
        val minimized:() -> Unit = {
            window.isMinimized = true
        }

        /** Tam ekran */
        val fullscreen:()-> Unit = {
            if(isFullscreen){
                isFullscreen = false
                playerWindowState.position =  fullscreenBeforePosition
                playerWindowState.size = fullscreenBeforeSize
                controlWindow?.isResizable = true
                playerWindow?.requestFocus()
            }else{
                isFullscreen = true
                fullscreenBeforePosition = WindowPosition(playerWindowState.position.x,playerWindowState.position.y)
                fullscreenBeforeSize =  playerWindowState.size
                playerWindowState.position = WindowPosition((-1).dp, 0.dp)
                val windowSize = Toolkit.getDefaultToolkit().screenSize.size.toComposeSize()
                playerWindowState.size = windowSize.copy(width = windowSize.width + 1.dp)
                controlWindow?.isResizable = false
                playerWindow?.requestFocus()
            }
        }

        DialogWindow(
            title = title,
            icon = painterResource("logo/logo.png"),
            state = playerWindowState,
            undecorated = true,
            resizable = false,
            onCloseRequest = { closeWindow() },
        ) {
            playerWindow = window
            Column(Modifier.fillMaxSize()) {
                if(isFullscreen){
                    Divider(color = Color(0xFF121212),modifier = Modifier.height(1.dp))
                }else{
                    Box(
                        Modifier.fillMaxWidth().height(40.dp)
                            .background(if (MaterialTheme.colors.isLight) Color.White else Color(48, 50, 52))
                    )
                }

                Box(Modifier.fillMaxSize()) {
                    val videoSize by remember(playerWindowState.size) {
                        derivedStateOf { Dimension(window.size.width, window.size.height - 40) }
                    }
                    videoPlayerComponent.size = videoSize
                    SwingPanel(
                        background = Color.Transparent,
                        modifier = Modifier.fillMaxSize(),
                        factory = { videoPlayerComponent },
                        update = {}
                    )

                }
            }
        }


        DialogWindow(
            onCloseRequest = { closeWindow() },
            title = title,
            transparent = true,
            undecorated = true,
            state = playerWindowState,
            icon = painterResource("logo/logo.png"),
            onPreviewKeyEvent ={ keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
                    play()
                    normalPause()
                    true
                } else if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown) {
                    if(isFullscreen){
                        fullscreen()
                        true
                    }else false
                }  else if (keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyUp) {
                    videoPlayerComponent.mediaPlayer().controls().skipTime(+5000L)
                    cleanDanmaku()
                    true
                } else if (keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyUp) {
                    videoPlayerComponent.mediaPlayer().controls().skipTime(-5000L)
                    cleanDanmaku()
                    true
                } else false

            }
        ) {
            controlWindow = window

            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxSize()
                    .pointerHoverIcon(playerCursor)
                    .border(border = BorderStroke(1.dp, if(isFullscreen) Color.Transparent else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                    .combinedClickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = null,
                        onDoubleClick = {
                            if (showingDetail) {
                                showingDetail = false
                            } else if(isWindows()){
                                fullscreen()
                            }
                        },
                        onClick = {},
                        onLongClick = {}
                    )
                    .onPointerEvent(PointerEventType.Enter) {
                        if (!controlBoxVisible) {
                            controlBoxVisible = true
                        }
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        if (isPlaying && !settingsExpanded && !showSubtitleMenu && !timeSliderPress && !audioSliderPress) {
                            controlBoxVisible = false
                        }
                    }
                    .onPointerEvent(PointerEventType.Move) {
                        controlBoxVisible = true
                        hideControlBoxTask?.cancel()
                        hideControlBoxTask = Timer("KontrolKutusunuGizle", false).schedule(10000) {
                            controlBoxVisible = false
                        }
                    }

            ) {

                LaunchedEffect(controlBoxVisible){
                    playerCursor = if(controlBoxVisible) PointerIcon.Default else PointerIcon.None
                }


                Column {
                    if(isFullscreen){
                        Divider(color = Color(0xFF121212),modifier = Modifier.height(1.dp))
                    }else{
                        WindowDraggableArea {
                            TitleBar(title, closeWindow,isFullscreen,fullscreen,minimized)
                        }
                    }


                    Box(Modifier
                        .fillMaxSize()
                        .onClick(
                            matcher = PointerMatcher.mouse(PointerButton.Secondary), // add onClick for every required PointerButton
                            keyboardModifiers = { true }, // e.g { isCtrlPressed }; Remove it to ignore keyboardModifiers
                            onClick = { showDropdownMenu = true}
                        )) {

                        /** Manuel olarak duraklatma tetiklenirse oynatma işlevini işleme */
                        val playEvent: () -> Unit = {
                            if (!isNormalPause) {
                                play()
                            }
                        }
                        val showingDetailChanged:(Boolean) -> Unit = {
                            showingDetail = it
                        }

                        DanmakuBox(
                            vocabulary,
                            vocabularyPath,
                            playerState,
                            showingDanmakuNum,
                            playEvent,
                            playAudio,
                            playerWindowState.size.height.value.toInt(),
                            showingDetail,
                            showingDetailChanged
                        )
                        if(isFullscreen){
                            var titleBarVisible by remember{ mutableStateOf(false) }
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .align(Alignment.TopCenter)
                                .onPointerEvent(PointerEventType.Enter){titleBarVisible = true}
                                .onPointerEvent(PointerEventType.Exit){titleBarVisible = false}
                            ){
                                AnimatedVisibility(titleBarVisible){
                                    TitleBar(title, closeWindow,isFullscreen,fullscreen,minimized)
                                }
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                        ) {
                            if (controlBoxVisible) {
                                // 进度条
                                var sliderVisible by remember { mutableStateOf(false) }
                                Box(
                                    Modifier
                                        .fillMaxWidth().padding(start = 5.dp, end = 5.dp, bottom = 10.dp)
                                        .offset(x = 0.dp, y = 20.dp)
                                        .onPointerEvent(PointerEventType.Enter) { sliderVisible = true }
                                        .onPointerEvent(PointerEventType.Exit) {
                                            if(!timeSliderPress){
                                                sliderVisible = false
                                            }
                                        }
                                ) {
                                    val animatedPosition by animateFloatAsState(
                                        targetValue = timeProgress,
                                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                                    )
                                    if (sliderVisible) {
                                        Slider(
                                            value = timeProgress,
                                            modifier = Modifier.align(Alignment.Center)
                                                .onPointerEvent(PointerEventType.Press){ timeSliderPress = true }
                                                .onPointerEvent(PointerEventType.Release){ timeSliderPress = false }
                                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                                            onValueChange = {
                                                timeProgress = it
                                                cleanDanmaku()
                                                videoPlayerComponent.mediaPlayer().controls().setPosition(timeProgress)
                                            })
                                    } else {
                                        LinearProgressIndicator(
                                            progress = animatedPosition,
                                            modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                                                .offset(x = 0.dp, y = (-20).dp).padding(top = 20.dp)
                                        )
                                    }
                                }
                                // 暂停、音量、时间、弹幕、设置
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    IconButton(onClick = {
                                        play()
                                        normalPause()
                                    }) {
                                        Icon(
                                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Localized description",
                                            tint = Color.White,
                                        )
                                    }
                                    var volumeOff by remember { mutableStateOf(false) }
                                    var volumeSliderVisible by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .onPointerEvent(PointerEventType.Enter) { volumeSliderVisible = true }
                                            .onPointerEvent(PointerEventType.Exit) { if(!audioSliderPress) volumeSliderVisible = false }
                                    ) {
                                        IconButton(onClick = {
                                            volumeOff = !volumeOff
                                            if(volumeOff){
                                                videoPlayerComponent.mediaPlayer().audio()
                                                    .setVolume(0)
                                            }
                                        }) {
                                            Icon(
                                                if (volumeOff) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = "Localized description",
                                                tint = Color.White,
                                            )
                                        }
                                        AnimatedVisibility (visible = volumeSliderVisible) {
                                            Slider(
                                                value = videoVolume,
                                                valueRange = 1f..100f,
                                                onValueChange = {
                                                    videoVolumeChanged (it)
                                                    if(it > 1f){
                                                        volumeOff = false
                                                        videoPlayerComponent.mediaPlayer().audio()
                                                            .setVolume(videoVolume.toInt())
                                                    }else{
                                                        volumeOff = true
                                                        videoPlayerComponent.mediaPlayer().audio()
                                                            .setVolume(0)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .onPointerEvent(PointerEventType.Enter) {
                                                        volumeSliderVisible = true
                                                    }
                                                    .onPointerEvent(PointerEventType.Press){ audioSliderPress = true }
                                                    .onPointerEvent(PointerEventType.Release){ audioSliderPress = false }
                                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            )
                                        }
                                    }

                                    // 时间
                                    Text(" $timeText ", color = Color.White)
                                    // 设置按钮
                                    Box {
                                        IconButton(onClick = { settingsExpanded = true }) {
                                            Icon(
                                                Icons.Filled.Settings,
                                                contentDescription = "Localized description",
                                                tint = Color.White,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = settingsExpanded,
                                            offset = DpOffset(x = (-60).dp, y = 0.dp),
                                            onDismissRequest = {
                                                settingsExpanded = false
                                                controlBoxVisible = true
                                            },
                                            modifier = Modifier
                                                .onPointerEvent(PointerEventType.Enter) {
                                                    controlBoxVisible = true
                                                }
                                                .onPointerEvent(PointerEventType.Exit) {
                                                    controlBoxVisible = true
                                                }
                                        ) {

                                            DropdownMenuItem(onClick = { }) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Kelime Konumlu Akan Yazı")
                                                    Switch(checked = !playerState.showSequence, onCheckedChange = {
                                                        playerState.showSequence = !it
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                            DropdownMenuItem(onClick = { }) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Sayı Konumlu Akan Yazı")
                                                    Switch(checked = playerState.showSequence, onCheckedChange = {
                                                        playerState.showSequence = it
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                            DropdownMenuItem(onClick = { }) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Akan Yazı")
                                                    Switch(checked = playerState.danmakuVisible, onCheckedChange = {
                                                        if (playerState.danmakuVisible) {
                                                            playerState.danmakuVisible = false
                                                            shouldAddDanmaku.clear()
                                                            showingDanmakuNum.clear()
                                                            danmakuTimer.stop()
                                                        } else {
                                                            playerState.danmakuVisible = true
                                                            danmakuTimer.restart()
                                                        }
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                        }

                                    }

                                    // 字幕和声音选择按钮
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = "Altyazı ve Ses", modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 100,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.TopCenter,
                                            alignment = Alignment.TopCenter,
                                            offset = DpOffset.Zero
                                        )
                                    ) {
                                        IconButton(onClick = {showSubtitleMenu = !showSubtitleMenu  },
                                            enabled = videoPath.isNotEmpty()) {
                                            Icon(
                                                Icons.Filled.Subtitles,
                                                contentDescription = "Localized description",
                                                tint = if(videoPath.isNotEmpty()) Color.White else Color.Gray
                                            )
                                        }
                                    }

                                    var height = (subtitleTrackList.size * 40 + 100).dp
                                    if(height>740.dp) height = 740.dp
                                    DropdownMenu(
                                        expanded = showSubtitleMenu,
                                        onDismissRequest = {showSubtitleMenu = false},
                                        modifier = Modifier.width(282.dp).height(height)
                                            .onPointerEvent(PointerEventType.Enter) {
                                                controlBoxVisible = true
                                            }
                                            .onPointerEvent(PointerEventType.Exit) {
                                                controlBoxVisible = true
                                            },
                                        offset = DpOffset(x = 170.dp, y = (-20).dp),
                                    ){
                                        var state by remember { mutableStateOf(0) }
                                        TabRow(
                                            selectedTabIndex = state,
                                            backgroundColor = Color.Transparent,
                                            modifier = Modifier.width(282.dp).height(40.dp)
                                        ) {
                                            Tab(
                                                text = { Text("Altyazı") },
                                                selected = state == 0,
                                                onClick = { state = 0 }
                                            )
                                            Tab(
                                                text = { Text("Ses") },
                                                selected = state == 1,
                                                onClick = { state = 1 }
                                            )
                                        }
                                        when (state) {
                                            0 -> {
                                                Column (Modifier.width(282.dp).height(700.dp)){
                                                    DropdownMenuItem(
                                                        onClick = {
                                                            showSubtitlePicker = true

                                                        },
                                                        modifier = Modifier.width(282.dp).height(40.dp)
                                                    ) {
                                                        Text(
                                                            text = "Altyazı Ekle",
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                    Divider()
                                                    Box(Modifier.width(282.dp).height(650.dp)){
                                                        val scrollState = rememberLazyListState()
                                                        LazyColumn(Modifier.fillMaxSize(),scrollState){
                                                            items(subtitleTrackList){(track,description) ->
                                                                DropdownMenuItem(
                                                                    onClick = {
                                                                        showSubtitleMenu = false
                                                                        setCurrentSubtitleTrack(track)
                                                                    },
                                                                    modifier = Modifier.width(282.dp).height(40.dp)
                                                                ){

                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.fillMaxWidth()) {
                                                                        val color = if(currentSubtitleTrack == track)  MaterialTheme.colors.primary else  Color.Transparent
                                                                        Spacer(Modifier
                                                                            .background(color)
                                                                            .height(16.dp)
                                                                            .width(2.dp)
                                                                        )

                                                                        Text(
                                                                            text = description,
                                                                            color = if(currentSubtitleTrack == track) MaterialTheme.colors.primary else  Color.Unspecified,
                                                                            fontSize = 12.sp,
                                                                            modifier = Modifier.padding(start = 10.dp)
                                                                        )
                                                                    }

                                                                }
                                                            }
                                                        }
                                                        VerticalScrollbar(
                                                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                                            adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                                        )
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Box(Modifier.width(282.dp).height(650.dp)){
                                                    val scrollState = rememberLazyListState()
                                                    LazyColumn(Modifier.fillMaxSize(),scrollState){
                                                        items(audioTrackList){(track,description) ->
                                                            DropdownMenuItem(
                                                                onClick = {
                                                                    showSubtitleMenu = false
                                                                    setCurrentAudioTrack(track)
                                                                },
                                                                modifier = Modifier.width(282.dp).height(40.dp)
                                                            ){

                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()) {
                                                                    val color = if(currentAudioTrack == track)  MaterialTheme.colors.primary else  Color.Transparent
                                                                    Spacer(Modifier
                                                                        .background(color)
                                                                        .height(16.dp)
                                                                        .width(2.dp)
                                                                    )

                                                                    Text(
                                                                        text = description,
                                                                        color = if(currentAudioTrack == track) MaterialTheme.colors.primary else  Color.Unspecified,
                                                                        fontSize = 12.sp,
                                                                        modifier = Modifier.padding(start = 10.dp)
                                                                    )
                                                                }

                                                            }
                                                        }
                                                    }
                                                    VerticalScrollbar(
                                                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                                    )
                                                }
                                            }
                                        }


                                    }

                                    // 输入框
                                    if (playerState.danmakuVisible && vocabularyPath.isNotEmpty()) {

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)))
                                        ) {
                                            fun searchDanmaku() {
                                                if (playerState.showSequence && searchDanmaku.isNotEmpty()) {
                                                    val num = searchDanmaku.toIntOrNull()
                                                    if (num != null) {
                                                        val danmakuItem = showingDanmakuNum[num]
                                                        if (danmakuItem != null) {
                                                            danmakuItem.isPause = true
                                                            showingDetail = true
                                                            if (!isNormalPause) {
                                                                play()
                                                            }
                                                        }
                                                    }

                                                }else if(!playerState.showSequence  && searchDanmaku.isNotEmpty()){
                                                    val danmakuItem = showingDanmakuWord[searchDanmaku]
                                                    if (danmakuItem != null) {
                                                        danmakuItem.isPause = true
                                                        showingDetail = true
                                                        if (!isNormalPause) {
                                                            play()
                                                        }
                                                    }
                                                }
                                            }
                                            Box(modifier = Modifier.width(110.dp).padding(start = 5.dp)) {
                                                BasicTextField(
                                                    value = searchDanmaku,
                                                    singleLine = true,
                                                    onValueChange = { searchDanmaku = it },
                                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                                    textStyle = MaterialTheme.typography.h5.copy(
                                                        color = Color.White,
                                                    ),
                                                    modifier = Modifier.onKeyEvent { keyEvent ->
                                                        if ((keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && keyEvent.type == KeyEventType.KeyUp) {
                                                            searchDanmaku()
                                                            true
                                                        } else false
                                                    }
                                                )
                                                if (searchDanmaku.isEmpty()) {
                                                        val text = if(playerState.showSequence) "Sayı Girin" else "Kelime Girin"
                                                    Text(text, color = Color.White)
                                                }
                                            }


                                            TooltipArea(
                                                tooltip = {
                                                    Surface(
                                                        elevation = 4.dp,
                                                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                                        shape = RectangleShape
                                                    ) {
                                                        Text(text = "Ara Enter", modifier = Modifier.padding(10.dp))
                                                    }
                                                },
                                                delayMillis = 100,
                                                tooltipPlacement = TooltipPlacement.ComponentRect(
                                                    anchor = Alignment.TopCenter,
                                                    alignment = Alignment.TopCenter,
                                                    offset = DpOffset.Zero
                                                )
                                            ) {
                                                IconButton(
                                                    onClick = { searchDanmaku() },
                                                    modifier = Modifier.size(40.dp, 40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Navigation,
                                                        contentDescription = "Localized description",
                                                        tint = Color.White,
                                                    )
                                                }

                                            }


                                        }

                                    }
                                }
                            }

                        }
                        if(videoPath.isEmpty()){
                            MaterialTheme(colors = darkColors(primary = Color.LightGray)) {
                                Row( modifier = Modifier.align(Alignment.Center)){
                                    OutlinedButton(onClick = { showFilePicker = true }){
                                        Text("Video Aç")
                                    }
                                }
                            }
                        }
                        // Video dosyası seçicisini göster
                        FilePicker(
                            show = showFilePicker,
                            initialDirectory = ""
                        ){file ->
                            if (file != null) {
                                if(file.path.isNotEmpty()){
                                    videoPathChanged(file.path)
                                }
                            }
                            showFilePicker = false
                        }
                        val extensions = if(isMacOS()) listOf("public.json") else listOf("json")
                        // Kelime dağarcığı dosyası seçicisini göster
                        FilePicker(
                            show = showVocabularyPicker,
                            fileExtensions = extensions,
                            initialDirectory = ""
                        ){file ->
                            if (file != null) {
                                if(file.path.isNotEmpty()){
                                    vocabularyPathChanged(file.path)
                                }
                            }
                            showVocabularyPicker = false
                        }
                        // Altyazı seçicisini göster
                        FilePicker(
                            show = showSubtitlePicker,
                            initialDirectory = ""
                        ){file ->
                            if (file != null) {
                                if(file.path.isNotEmpty()){
                                    addSubtitle(file.path)
                                }
                            }
                            showSubtitlePicker = false
                        }

                        // Sağ tıklama menüsünü göster
                        CursorDropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = {showDropdownMenu = false},
                        ){
                            DropdownMenuItem(onClick = {
                                showFilePicker = true
                                showDropdownMenu = false
                            }) {
                                Text("Video Aç")
                            }
                            DropdownMenuItem(
                                enabled = videoPath.isNotEmpty(),
                                onClick = {
                                    showVocabularyPicker = true
                                    showDropdownMenu = false
                                }) {
                                Text("Kelime Dağarcığı Ekle")
                            }
                        }

                    }
                }


            }


            /** Oynatıcı görüntülendikten sonra yalnızca bir kez yürütülür, minimum boyutu ayarlar, zaman ilerleme çubuğunu ve zamanı bağlar, sürükle ve bırak işlevini ayarlar */
            LaunchedEffect(Unit) {
                if(playerState.danmakuVisible && videoPath.isNotEmpty() && danmakuMap.isNotEmpty()){
                    danmakuTimer.start()
                }
                window.minimumSize = Dimension(900,662)
                val eventListener = object:MediaPlayerEventAdapter() {
                    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                        val videoDuration = videoPlayerComponent.mediaPlayer().media().info().duration()
                        timeProgress = (newTime.toFloat()).div(videoDuration)
                        var startText: String
                        timeProgress.times(videoDuration).toInt().milliseconds.toComponents { hours, minutes, seconds, _ ->
                            startText = timeFormat(hours, minutes, seconds)
                        }
                        videoDuration.milliseconds.toComponents { hours, minutes, seconds, _ ->
                            val durationText = timeFormat(hours, minutes, seconds)
                            timeText = "$startText / $durationText"
                        }

                    }

                    private fun timeFormat(hours: Long, minutes: Int, seconds: Int): String {
                        val h = if (hours < 10) "0$hours" else "$hours"
                        val m = if (minutes < 10) "0$minutes" else "$minutes"
                        val s = if (seconds < 10) "0$seconds" else "$seconds"
                        return "$h:$m:$s"
                    }
                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                        mediaPlayer.audio().setVolume(videoVolume.toInt())
                        currentSubtitleTrack = mediaPlayer.subpictures().track()
                        currentAudioTrack = mediaPlayer.audio().track()

                        if(subtitleTrackList.isNotEmpty()) subtitleTrackList.clear()
                        if(audioTrackList.isNotEmpty()) audioTrackList.clear()

                        mediaPlayer.subpictures().trackDescriptions().forEach { trackDescription ->
                            subtitleTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
                        }
                        mediaPlayer.audio().trackDescriptions().forEach { trackDescription ->
                            audioTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
                        }
                    }

                    override fun finished(mediaPlayer: MediaPlayer?) {
                        isPlaying = false
                    }

                }
                videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
                /** Sürükle ve bırak işlevini ayarla */
                val transferHandler = createTransferHandler(
                    singleFile = false,
                    showWrongMessage = { message ->
                        JOptionPane.showMessageDialog(window, message)
                    },
                    parseImportFile = {  parseImportFile(it)}
                )
                window.transferHandler = transferHandler
            }
            /** Silmek için mediaPlayerEventListener referansını kaydet.*/
            var mediaPlayerEventListener by remember{ mutableStateOf<MediaPlayerEventAdapter?>(null) }
            /** Başlangıçta bir kez yürütülür, her kelime dağarcığı eklendikten sonra tekrar yürütülür */
            LaunchedEffect(vocabularyPath) {
                if(mediaPlayerEventListener != null){
                    videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(mediaPlayerEventListener)
                }
                var lastTime = -1
                var lastMaxLength = 0
                val eventListener = object:MediaPlayerEventAdapter() {
                    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                        // Birim: saniye
                        val startTime = (newTime.milliseconds.inWholeSeconds + widthDuration.div(3000)).toInt()
                        // Saniyede bir kez yürüt
                        if (playerState.danmakuVisible && danmakuMap.isNotEmpty() && startTime != lastTime) {
                            val danmakuList = danmakuMap[startTime]
                            var offsetY = if(isFullscreen) 50 else 20
                            val sequenceWidth = if (playerState.showSequence) counter.toString().length * 12 else 0
                            val offsetX = sequenceWidth + lastMaxLength * 12 + 30
                            var maxLength = 0
                            danmakuList?.forEach { danmakuItem ->
                                if (offsetY > 395) offsetY = 10
                                danmakuItem.position = IntOffset(window.size.width + offsetX, offsetY)
                                offsetY += 35
                                if (danmakuItem.content.length > maxLength) {
                                    maxLength = danmakuItem.content.length
                                }

                                // TODO Burada hala çoklu iş parçacığı sorunu var, şöyle bir durum oluşabilir: Buraya yeni eklendiğinde, animasyonu kontrol eden başka bir Zamanlayıcı'da hemen silinebilir.
                                danmakuItem.sequence = counter
                                shouldAddDanmaku[counter++] = danmakuItem
                                if(counter == 100) counter = 1
                            }
                            lastMaxLength = maxLength
                            lastTime = startTime
                        }
                    }
                }
                videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
                mediaPlayerEventListener = eventListener
            }

            /** Videoyu açtıktan sonra otomatik oynat */
            LaunchedEffect(videoPath) {
                if(videoPath.isNotEmpty()){
                    videoPlayerComponent.mediaPlayer().media().play(videoPath,":sub-autodetect-file")
                    isPlaying = true
                    if(playerState.danmakuVisible && !danmakuTimer.isRunning){
                        danmakuTimer.restart()
                    }
                    if(danmakuTimer.isRunning){
                        showingDanmakuNum.clear()
                    }
                    // Garip bir hata var, videoyu açtıktan sonra yalnızca ses var ve görüntü yok, pencere boyutunu ayarladıktan sonra normale dönüyor.
                    playerWindowState.size =DpSize(playerWindowState.size.width + 1.dp,playerWindowState.size.height)
                    Timer("genisligiGeriYukle", false).schedule(500) {
                        playerWindowState.size =DpSize(playerWindowState.size.width - 1.dp,playerWindowState.size.height)
                    }
                }
            }

            /** Pencere boyutlarını senkronize et */
            LaunchedEffect(playerWindowState) {
                snapshotFlow { playerWindowState.size }
                    .onEach {
                        // Pencere ve iletişim kutusu boyutlarını senkronize et
                        windowState.size = playerWindowState.size
                        val titleBarHeight = if(isFullscreen) 1 else 40
                        videoPlayerComponent.size =
                            Dimension(playerWindowState.size.width.value.toInt(), playerWindowState.size.height.value.toInt() - titleBarHeight)
                        widthDuration = playerWindowState.size.width.value.div(3).times(30).toInt()
                        // Pencere genişliğini değiştirdikten sonra bazı akan yazılar hızlanır ve bazıları üst üste gelir, bu yüzden tüm akan yazıları temizlemek gerekir.
                        cleanDanmaku()
                    }
                    .launchIn(this)

                snapshotFlow { playerWindowState.position }
                    .onEach {
                        // Pencere ve iletişim kutusu konumlarını senkronize et
                        windowState.position = playerWindowState.position
                    }
                    .launchIn(this)
            }
        }
    }

}
@Composable
fun TitleBar(
    title: String,
    closeWindow: () -> Unit,
    isFullscreen:Boolean,
    fullscreen:() -> Unit,
    minimized:() -> Unit,
) {
    Box(
        Modifier.fillMaxWidth()
            .height(40.dp)
            .background(if (MaterialTheme.colors.isLight) Color.White else Color(48, 50, 52))
    ) {
        Text(
            title,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colors.onBackground
        )
        if(isMacOS()){
            Row(Modifier.align(Alignment.TopStart).padding(top = 8.dp)) {
                ThirteenPixelCircle(onClick = {closeWindow()},color = Color(246, 95, 87),modifier = Modifier.padding(start = 8.dp))
                ThirteenPixelCircle(onClick = {minimized()},color = Color(250, 188, 47),modifier = Modifier.padding(start = 9.dp))
                ThirteenPixelCircle(onClick = {},color = Color(0xFF9B9B9B),modifier = Modifier.padding(start = 9.dp))
            }

        }else {
            Row(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { minimized()
                }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Localized description",
                        tint = Color(140, 140, 140),
                    )
                }


                    IconButton(onClick = { fullscreen() },
                        modifier = Modifier.size(40.dp)) {
                        Icon(
                            if(isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "Localized description",
                            tint = Color(140, 140, 140),
                        )
                    }


                IconButton(
                    onClick = { closeWindow() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Localized description",
                        tint = Color(140, 140, 140),
                    )
                }

            }
        }


    }
}

@Composable
fun ThirteenPixelCircle(
    onClick :() -> Unit,
    color: Color,
    modifier:Modifier
) {
    BoxWithConstraints {
        Box(
            modifier = modifier
                .clickable { onClick() }
                .size(13.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}


fun Dimension.toComposeSize(): DpSize = DpSize(width.dp, height.dp)

@Composable
fun DanmakuBox(
    vocabulary: MutableVocabulary?,
    vocabularyPath:String,
    playerState: PlayerState,
    showingDanmaku: SnapshotStateMap<Int, DanmakuItem>,
    playEvent: () -> Unit,
    playAudio: (String) -> Unit,
    windowHeight: Int,
    showingDetail:Boolean,
    showingDetailChanged:(Boolean) -> Unit
) {

    /** 删除单词 */
    val deleteWord: (DanmakuItem) -> Unit = { danmakuItem ->
        if (danmakuItem.word != null) {
            val word = danmakuItem.word
            vocabulary!!.wordList.remove(word)
            vocabulary.size = vocabulary.wordList.size
            try{
                saveVocabulary(vocabulary.serializeVocabulary,vocabularyPath)
                showingDanmaku.remove(danmakuItem.sequence)
            }catch (e:Exception){
                // 回滚
                if (word != null) {
                    vocabulary.wordList.add(word)
                    vocabulary.size = vocabulary.wordList.size
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(null, "保存词库失败,错误信息:\n${e.message}")
            }

        }

        showingDetailChanged(false)
        playEvent()
    }

    /** 把单词加入到熟悉词库 */
    val addToFamiliar: (DanmakuItem) -> Unit = { danmakuItem ->
        val word = danmakuItem.word
        if (word != null) {
            val familiarWord = word.deepCopy()
            val file = getFamiliarVocabularyFile()
            val familiar = loadVocabulary(file.absolutePath)
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (vocabulary!!.type == VocabularyType.MKV ||
                vocabulary.type == VocabularyType.SUBTITLES
            ) {
                familiarWord.captions.forEach { caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = vocabulary.relateVideoPath,
                        subtitlesTrackId = vocabulary.subtitlesTrackId,
                        subtitlesName = vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    familiarWord.externalCaptions.add(externalCaption)
                }
                familiarWord.captions.clear()

            }
            if (!familiar.wordList.contains(familiarWord)) {
                familiar.wordList.add(familiarWord)
                familiar.size = familiar.wordList.size
            }
            if(familiar.name.isEmpty()){
                familiar.name = "FamiliarVocabulary"
            }
            try{
                saveVocabulary(familiar, file.absolutePath)
                deleteWord(danmakuItem)
            }catch (e:Exception){
                // 回滚
                familiar.wordList.remove(familiarWord)
                familiar.size = familiar.wordList.size
                e.printStackTrace()
                JOptionPane.showMessageDialog(null, "保存熟悉词库失败,错误信息:\n${e.message}")
            }

        }

    }

    /** Sabit genişlikli yazı tipi*/
    val monospace  = rememberMonospace()

    // Bu Box'ta Modifier.fillMaxSize() kullanmak DropdownMenu'nun yanlış konumda görüntülenmesine neden olabilir.
    Box {
        showingDanmaku.forEach { (_, danmakuItem) ->
            Danmaku(
                playerState,
                danmakuItem,
                playEvent,
                playAudio,
                monospace,
                windowHeight,
                deleteWord,
                addToFamiliar,
                showingDetail,
                showingDetailChanged
            )
        }
    }
}

@Composable
fun rememberDanmakuMap(
    videoPath: String,
    vocabularyPath: String,
    vocabulary: MutableVocabulary?
) = remember(videoPath, vocabulary){
    derivedStateOf{
        // Anahtar saniyedir > Bu saniyede görünen kelimelerin listesi
        val timeMap = mutableMapOf<Int, MutableList<DanmakuItem>>()
        val vocabularyDir = File(vocabularyPath).parentFile
        if (vocabulary != null) {
            // Altyazılar ve MKV kullanılarak oluşturulan kelime dağarcıkları
            if (vocabulary.type == VocabularyType.MKV || vocabulary.type == VocabularyType.SUBTITLES) {
                val absVideoFile = File(videoPath)
                val relVideoFile = File(vocabularyDir, absVideoFile.name)
                // absVideoFile.exists() doğruysa video dosyası taşınmamıştır ve hala kelime dağarcığında kayıtlı adrestir
                //  relVideoFile.exists() doğruysa video dosyası taşınmıştır ve kelime dağarcığında kayıtlı adres eski adrestir
                if ((absVideoFile.exists() && absVideoFile.absolutePath ==  vocabulary.relateVideoPath) ||
                    (relVideoFile.exists() && relVideoFile.name == File(vocabulary.relateVideoPath).name)
                ) {
                    vocabulary.wordList.forEach { word ->
                        if (word.captions.isNotEmpty()) {
                            word.captions.forEach { caption ->
                                val startTime = floor(convertTimeToSeconds(caption.start)).toInt()
                                addDanmakuToMap(timeMap, startTime, word)
                            }
                        }
                    }
                }

                // Belge kelime dağarcığı veya karma kelime dağarcığı
            } else {
                vocabulary.wordList.forEach { word ->
                    word.externalCaptions.forEach { externalCaption ->
                        val absVideoFile = File(videoPath)
                        val relVideoFile = File(vocabularyDir, absVideoFile.name)
                        if ((absVideoFile.exists() && absVideoFile.absolutePath == externalCaption.relateVideoPath) ||
                            (relVideoFile.exists() && relVideoFile.name == File(externalCaption.relateVideoPath).name)
                        ) {
                            val startTime = floor(convertTimeToSeconds(externalCaption.start)).toInt()
                            addDanmakuToMap(timeMap, startTime, word)
                        }
                    }
                }
            }
        }
        timeMap
    }
}

private fun addDanmakuToMap(
    timeMap: MutableMap<Int, MutableList<DanmakuItem>>,
    startTime: Int,
    word: Word
) {
    val dList = timeMap[startTime]
    val item = DanmakuItem(word.value, true, startTime, 0, false, IntOffset(0, 0), word)
    if (dList == null) {
        val newList = mutableListOf(item)
        timeMap[startTime] = newList
    } else {
        dList.add(item)
    }
}

@ExperimentalSerializationApi
@Serializable
data class PlayerData(
    var showSequence: Boolean = false,
    var danmakuVisible: Boolean = false,
    var autoCopy: Boolean = false,
    var autoSpeak: Boolean = true,
    var preferredChinese: Boolean = true
)




val PointerIcon.Companion.None: PointerIcon
    get() {
        val toolkit = Toolkit.getDefaultToolkit()
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val transparentCursor = toolkit.createCustomCursor(image, Point(0, 0), "transparentCursor")
        return PointerIcon(transparentCursor)
    }
