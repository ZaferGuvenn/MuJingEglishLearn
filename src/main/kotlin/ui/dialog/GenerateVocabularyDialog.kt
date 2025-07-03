package ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.*
import data.VocabularyType.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import player.isWindows
import player.parseTrackList
import state.AppState
import state.getResourcesFile
import ui.components.BuiltInVocabularyMenu
import ui.components.SaveButton
import ui.dialog.FilterState.*
import ui.edit.SaveOtherVocabulary
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.*
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView
import kotlin.math.max
import kotlin.math.min

/**
 * Kelime Listesi Oluştur
 * @param state Uygulama durumu
 * @param title Başlık
 * @param type Kelime listesi türü
 */
@OptIn(
    ExperimentalSerializationApi::class
)
@ExperimentalComposeUiApi
@Composable
fun GenerateVocabularyDialog(
    state: AppState,
    title: String, // Bu başlık zaten App.kt içinde Türkçeleştirilmişti (örn: "Kelime Listesini Filtrele")
    type: VocabularyType
) {
    val windowWidth = if (type == MKV) 1320.dp else 1285.dp
    DialogWindow(
        title = title, // title parametresi zaten Türkçe olarak geliyor
        onCloseRequest = {
            onCloseRequest(state, title)
        },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(windowWidth, 850.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        val scope = rememberCoroutineScope()

        val fileFilter = when (title) {
            "Kelime Listesini Filtrele" -> FileNameExtensionFilter( // "过滤词库" -> "Kelime Listesini Filtrele"
                "Kelime Listesi (*.json)", // "词库" -> "Kelime Listesi (*.json)"
                "json",
            )

            "Belgeden Kelime Listesi Oluştur" -> FileNameExtensionFilter( // "用文档生成词库" -> "Belgeden Kelime Listesi Oluştur"
                "Desteklenen belgeler (*.pdf;*.txt;*.md...)", // "支持的文件扩展(*.pdf;*.txt;*.md*.java;*.cs;*.cpp;*.c;*.kt;*.py;*.ts)" -> "Desteklenen belgeler (*.pdf;*.txt;*.md...)"
                "pdf", "txt", "md", "java", "cs", "cpp", "c", "kt", "js", "py", "ts"
            )

            "Altyazıdan Kelime Listesi Oluştur" -> FileNameExtensionFilter( // "用字幕生成词库" -> "Altyazıdan Kelime Listesi Oluştur"
                "SRT veya ASS formatındaki altyazı dosyaları", // Aynı kalabilir veya "Altyazı Dosyaları (*.srt, *.ass)"
                "srt", "ass"
            )

            "Videodan Kelime Listesi Oluştur" -> FileNameExtensionFilter( // "用视频生成词库" -> "Videodan Kelime Listesi Oluştur"
                "MKV veya MP4 formatındaki video dosyaları", // Aynı kalabilir veya "Video Dosyaları (*.mkv, *.mp4)"
                "mkv", "mp4",
            )

            else -> null
        }

        var started by remember { mutableStateOf(false) }

        /**
         * 选择的文件列表,用于批量处理
         */
        val selectedFileList = remember { mutableStateListOf<File>() }

        /**
         * 显示任务列表,用于拖拽多个文件进入窗口时，自动为真，
         * 分析完字幕，自动为假，单击【任务列表】显示-隐藏。
         */
        var showTaskList by remember { mutableStateOf(false) }

        /**
         * 任务列表的状态
         */
        val tasksState = remember { mutableStateMapOf<File, Boolean>() }

        /**
         * 正在处理的文件
         */
        var currentTask by remember { mutableStateOf<File?>(null) }

        /**
         * 批处理时的错误信息
         */
        val errorMessages = remember { mutableStateMapOf<File, String>() }

        /**
         * 批量处理时，选择文件，用于删除
         */
        var selectable by remember { mutableStateOf(false) }

        /**
         * 勾选的文件，用于批量删除
         */
        val checkedFileMap = remember { mutableStateMapOf<File, Boolean>() }

        /**
         * 是否全选
         */
        var isSelectedAll by remember { mutableStateOf(false) }

        /**
         * 选择的文件的绝对路径
         */
        var selectedFilePath by remember { mutableStateOf("") }

        /**
         * 选择的字幕名称
         */
        var selectedSubtitlesName by remember { mutableStateOf("    ") }

        /**
         * 预览的单词
         */
        val previewList = remember { mutableStateListOf<Word>() }


        val parsedList = remember { mutableStateListOf<Word>() }

        /**
         * 用字幕生成单词 -> 相关视频的地址
         */
        var relateVideoPath by remember { mutableStateOf("") }

        /**
         * 字幕的轨道 ID
         */
        var selectedTrackId by remember { mutableStateOf(0) }

        /**
         * 需要过滤的词库的类型
         */
        var filteringType by remember { mutableStateOf(DOCUMENT) }

        /**
         * 字幕轨道列表
         */
        val trackList = remember { mutableStateListOf<Pair<Int, String>>() }

        /**
         * 这个 filterState 有四个状态：Idle、Parse、Filtering、End
         */
        var filterState by remember { mutableStateOf(Idle) }

        /**
         * 摘要词库
         */
        val summaryVocabulary = loadSummaryVocabulary()

        /**
         * 用于过滤的词库列表
         */
        val vocabularyFilterList = remember { mutableStateListOf<File>() }

        /**
         * 是否过滤词组
         */
        var enablePhrases by remember { mutableStateOf(false) }

        /**
         * 过滤单词
         */
        var filter by remember { mutableStateOf(true) }

        /**
         * 包含单词
         */
        var include by remember { mutableStateOf(false) }

        /**
         * 是否过滤所有的数字
         */
        var numberFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 BNC 词频前 1000 的单词，最常见的 1000 词
         */
        var bncNumberFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 COCA 词频前 1000 的单词，最常见的 1000 词
         */
        var frqNumFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 BNC 词频为0的单词
         */
        var bncZeroFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 COCA 词频为0的单词
         */
        var frqZeroFilter by remember { mutableStateOf(false) }

        /**
         * 是否替换索引派生词
         */
        var replaceToLemma by remember { mutableStateOf(false) }

        /** 熟悉词库 */
        val familiarVocabulary = remember { loadMutableVocabularyByName("FamiliarVocabulary") }

        /** 用鼠标删除的单词列表 */
        val removedWords = remember { mutableStateListOf<Word>() }

        var progressText by remember { mutableStateOf("") }

        var loading by remember { mutableStateOf(false) }

        var sort by remember { mutableStateOf("appearance") }

        var showCard by remember { mutableStateOf(true) }

        /** Dosya seçicinin başlığı */
        val chooseText = when (title) {
            "Kelime Listesini Filtrele" -> "Kelime Listesi Seç" // "过滤词库" -> "Kelime Listesini Filtrele", "选择词库" -> "Kelime Listesi Seç"
            "Belgeden Kelime Listesi Oluştur" -> "Belge Seç" // "用文档生成词库" -> "Belgeden Kelime Listesi Oluştur", "选择文档" -> "Belge Seç"
            "Altyazıdan Kelime Listesi Oluştur" -> "Altyazı Seç" // "用字幕生成词库" -> "Altyazıdan Kelime Listesi Oluştur", "选择字幕" -> "Altyazı Seç"
            "Videodan Kelime Listesi Oluştur" -> "Video Dosyası Seç" // "用视频生成词库" -> "Videodan Kelime Listesi Oluştur", "选择 MKV 文件" -> "Video Dosyası Seç" (MKV yerine genel Video)
            else -> ""
        }

        /** Sürüklenen dosyalar ve dosya seçiciden seçilen dosyalar bu fonksiyonu kullanır */
        val parseImportFile: (List<File>) -> Unit = { files ->
            scope.launch(Dispatchers.Default) {
                if (files.size == 1) {
                    val file = files.first()
                    when (file.extension) {
                        "pdf", "txt", "md", "java", "cs", "cpp", "c", "kt", "js", "py", "ts" -> {
                            if (type == DOCUMENT) {
                                selectedFilePath = file.absolutePath
                                selectedSubtitlesName = "    "
                            } else {
                                JOptionPane.showMessageDialog(
                                    window,
                                    "${file.nameWithoutExtension} belgesinden kelime listesi oluşturmak istiyorsanız,\nlütfen yeniden seçin: Kelime Listesi -> Belgeden Kelime Listesi Oluştur, sonra dosyayı buraya sürükleyip bırakın."
                                )
                            }
                        }

                        "srt", "ass" -> {
                            if (type == SUBTITLES) {
                                selectedFilePath = file.absolutePath
                                selectedSubtitlesName = "    "
                            } else {
                                JOptionPane.showMessageDialog(
                                    window,
                                    "${file.nameWithoutExtension} altyazısından kelime listesi oluşturmak istiyorsanız,\nlütfen yeniden seçin: Kelime Listesi -> Altyazıdan Kelime Listesi Oluştur, sonra dosyayı buraya sürükleyip bırakın."
                                )
                            }
                        }

                        "mkv", "mp4" -> {
                            when (type) {
                                MKV -> {
                                    // İlk sürükleme
                                    if (selectedFilePath.isEmpty() && selectedFileList.isEmpty()) {
                                        loading = true
                                        parseTrackList(
                                            state.videoPlayerComponent,
                                            window,
                                            state.videoPlayerWindow,
                                            file.absolutePath,
                                            setTrackList = {
                                                trackList.clear()
                                                trackList.addAll(it)
                                                if (it.isNotEmpty()) {
                                                    selectedFilePath = file.absolutePath
                                                    relateVideoPath = file.absolutePath
                                                    selectedSubtitlesName = "    "
                                                }
                                            }
                                        )

                                        loading = false
                                    } else { // Pencerede zaten dosya var
                                        // Aynı MKV videosu zaten var, ekleme
                                        if (file.absolutePath == selectedFilePath) {
                                            return@launch
                                        }
                                        // Toplu kelime listesi oluşturma geçici olarak MP4 formatını desteklemiyor
                                        if (file.extension == "mp4") {
                                            JOptionPane.showMessageDialog(
                                                window,
                                                "Toplu kelime listesi oluşturma şu anda MP4 formatını desteklememektedir."
                                            )
                                            return@launch
                                        }
                                        // Eğer daha önce bir MKV videosu varsa, önceki videoyu selectedFileList'e ekle
                                        if (selectedFilePath.isNotEmpty() && selectedFileList.isEmpty()) {
                                            val f = File(selectedFilePath)
                                            if (f.extension == "mp4") {
                                                JOptionPane.showMessageDialog(
                                                    window,
                                                    "Toplu kelime listesi oluşturma moduna geçiliyor.\n" +
                                                            "Bu mod şu anda MP4 formatını desteklememektedir.\n" +
                                                            "${f.nameWithoutExtension} listeye eklenmeyecektir."
                                                )

                                            } else {
                                                selectedFileList.add(f)
                                            }
                                            trackList.clear()
                                            selectedSubtitlesName = "    "
                                            selectedFilePath = ""
                                            relateVideoPath = ""
                                        }
                                        // Listede bu dosya yoksa ekle
                                        if (!selectedFileList.contains(file)) {
                                            selectedFileList.add(file)
                                            selectedFileList.sortBy { it.nameWithoutExtension }
                                            if (selectedFileList.isNotEmpty()) showTaskList = true
                                        }


                                    }

                                }

                                SUBTITLES -> {
                                    relateVideoPath = file.absolutePath
                                }

                                else -> {
                                    JOptionPane.showMessageDialog(
                                        window,
                                        "${file.nameWithoutExtension} videosundan kelime listesi oluşturmak istiyorsanız,\nlütfen yeniden seçin: Kelime Listesi -> Videodan Kelime Listesi Oluştur, sonra dosyayı buraya sürükleyip bırakın."
                                    )
                                }
                            }
                        }

                        "json" -> {
                            if (title == "Kelime Listesini Filtrele") { // "过滤词库" -> "Kelime Listesini Filtrele"
                                selectedFilePath = file.absolutePath
                            }
                        }

                        else -> {
                            JOptionPane.showMessageDialog(window, "Desteklenmeyen format.") // "格式不支持" -> "Desteklenmeyen format."
                        }
                    }


                } else if (files.size == 2 && type == SUBTITLES) {
                    val first = files.first()
                    val last = files.last()
                    if (first.extension == "srt" && (last.extension == "mp4" || last.extension == "mkv")) {
                        selectedFilePath = first.absolutePath
                        selectedSubtitlesName = "    "
                        relateVideoPath = last.absolutePath
                        selectedTrackId = -1
                    } else if (last.extension == "srt" && (first.extension == "mp4" || first.extension == "mkv")) {
                        selectedFilePath = last.absolutePath
                        selectedSubtitlesName = "    "
                        relateVideoPath = first.absolutePath
                        selectedTrackId = -1
                    } else if (first.extension == "srt" && last.extension == "srt") {
                        JOptionPane.showMessageDialog(
                            window,
                            "İki SRT altyazı dosyası kabul edilemez.\nBir altyazı (.srt) ve bir video (.mp4, .mkv) dosyası gereklidir."
                        )
                    } else if (first.extension == "mp4" && last.extension == "mp4") {
                        JOptionPane.showMessageDialog(
                            window,
                            "İki MP4 video dosyası kabul edilemez.\nBir altyazı (.srt) ve bir video (.mp4, .mkv) dosyası gereklidir."
                        )
                    } else if (first.extension == "mkv" && last.extension == "mkv") {
                        JOptionPane.showMessageDialog(
                            window,
                            "İki MKV video dosyası kabul edilemez.\nBir altyazı (.srt) ve bir video (.mp4, .mkv) dosyası gereklidir."
                        )
                    } else if (first.extension == "mkv" && last.extension == "mp4") {
                        JOptionPane.showMessageDialog(
                            window,
                            "İki video dosyası kabul edilemez.\nBir altyazı (.srt) ve bir video (.mp4, .mkv) dosyası gereklidir."
                        )
                    } else {
                        JOptionPane.showMessageDialog(
                            window,
                            "Format hatası.\nBir altyazı (.srt) ve bir video (.mp4, .mkv) dosyası gereklidir."
                        )
                    }

                } else if (files.size in 2..100 && type == MKV) {
                    var extensionWrong = ""
                    files.forEach { file ->
                        if (file.extension == "mkv") {
                            if (!selectedFileList.contains(file)) {
                                selectedFileList.add(file)
                            }

                        } else {
                            extensionWrong = extensionWrong + file.name + "\n"
                        }
                    }
                    selectedFileList.sortBy { it.nameWithoutExtension }
                    if (selectedFileList.isNotEmpty()) showTaskList = true
                    if (extensionWrong.isNotEmpty()) {
                        JOptionPane.showMessageDialog(window, "Aşağıdaki dosyalar MKV formatında değil:\n$extensionWrong")
                    }

                } else if (files.size > 100 && type == MKV) {
                    JOptionPane.showMessageDialog(window, "Toplu işlem en fazla 100 dosya içerebilir.")
                } else {
                    JOptionPane.showMessageDialog(window, "Dosya sayısı ikiyi geçemez.")
                }
            }
        }

        /** Dosya açıldığında çağrılan fonksiyon */
        val openFile: () -> Unit = {
            scope.launch(Dispatchers.Default) {
                val fileChooser = withContext(Dispatchers.IO) {
                    state.futureFileChooser.get()
                }
                fileChooser.dialogTitle = chooseText // chooseText zaten Türkçeleştirildi
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.isAcceptAllFileFilterUsed = false
                fileChooser.isMultiSelectionEnabled = true
                fileChooser.addChoosableFileFilter(fileFilter)
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    val files = fileChooser.selectedFiles.toList()
                    parseImportFile(files)
                }
                fileChooser.selectedFiles = null
                fileChooser.isMultiSelectionEnabled = true
                fileChooser.removeChoosableFileFilter(fileFilter)
            }
        }

        //设置窗口的拖放处理函数
        LaunchedEffect(Unit) {
            val transferHandler = createTransferHandler(
                singleFile = false,
                showWrongMessage = { message ->
                    JOptionPane.showMessageDialog(window, message)
                },
                parseImportFile = { parseImportFile(it) }
            )
            window.transferHandler = transferHandler
        }

        /** 全选 */
        val selectAll: () -> Unit = {
            if (!isSelectedAll) {
                selectedFileList.forEach { file ->
                    checkedFileMap[file] = true
                }
                isSelectedAll = true
            } else {
                selectedFileList.forEach { file ->
                    checkedFileMap[file] = false
                }
                isSelectedAll = false
            }
        }

        /** 删除 */
        val delete: () -> Unit = {
            val list = mutableListOf<File>()
            checkedFileMap.forEach { (file, checked) ->
                if (checked) {
                    list.add(file)
                }
            }
            list.forEach { file ->
                checkedFileMap.remove(file)
                selectedFileList.remove(file)
                tasksState.remove(file)
                errorMessages.remove(file)
                checkedFileMap[file] = false
                if (currentTask == file) {
                    currentTask = null
                }
            }
        }

        /** 打开关联的视频 */
        val openRelateVideo: () -> Unit = {
            val fileChooser = state.futureFileChooser.get()
            fileChooser.dialogTitle = "选择视频"
            fileChooser.isAcceptAllFileFilterUsed = true
            fileChooser.selectedFile = null
            if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                relateVideoPath = file.absolutePath
                fileChooser.selectedFile = File("")
            }
            fileChooser.selectedFile = null
            fileChooser.removeChoosableFileFilter(fileFilter)
        }

        /** 改变了左边过滤区域的状态，如有有一个为真，或者选择了一个词库，就开始过滤 */
        val shouldApplyFilters: () -> Boolean = {
            numberFilter || bncNumberFilter || frqNumFilter ||
                    bncZeroFilter || frqZeroFilter || replaceToLemma ||
                    vocabularyFilterList.isNotEmpty()
        }

        /** 分析文件里的单词 */
        val analysis: (String, Int) -> Unit = { pathName, trackId ->
            started = true
            filterState = Parsing
            vocabularyFilterList.clear()
            previewList.clear()
            parsedList.clear()
            scope.launch(Dispatchers.Default) {
                val words = when (type) {
                    DOCUMENT -> {
                        if (title == "过滤词库") {
                            val vocabulary = loadVocabulary(pathName)
                            filteringType = vocabulary.type
                            relateVideoPath = vocabulary.relateVideoPath
                            selectedTrackId = vocabulary.subtitlesTrackId
                            vocabulary.wordList
                        } else {
                            parseDocument(
                                pathName = pathName,
                                enablePhrases = enablePhrases,
                                sentenceLength = state.global.maxSentenceLength,
                                setProgressText = { progressText = it })
                        }

                    }

                    SUBTITLES -> {
                        val extension = File(pathName).extension
                        if (extension == "srt") {
                            parseSRT(pathName = pathName,
                                enablePhrases = enablePhrases,
                                setProgressText = { progressText = it }
                            )
                        } else {
                            parseASS(
                                pathName = pathName,
                                enablePhrases = enablePhrases,
                                setProgressText = { progressText = it }
                            )
                        }
                    }

                    MKV -> {
                        parseVideo(
                            pathName = pathName,
                            enablePhrases = enablePhrases,
                            trackId = trackId,
                            setProgressText = { progressText = it }
                        )
                    }
                }
                parsedList.addAll(words)
                filterState = if (shouldApplyFilters()) {
                    Filtering
                } else {
                    // 不用过滤
                    previewList.addAll(words)
                    End
                }
            }
        }

        /** 批量分析文件 MKV 视频里的单词 */
        val batchAnalysis: (String) -> Unit = { language ->
            started = true
            vocabularyFilterList.clear()
            previewList.clear()
            parsedList.clear()
            scope.launch(Dispatchers.Default) {
                val words = batchReadMKV(
                    language = language,
                    enablePhrases = enablePhrases,
                    selectedFileList = selectedFileList,
                    setCurrentTask = { currentTask = it },
                    setErrorMessages = {
                        errorMessages.clear()
                        errorMessages.putAll(it)
                    },
                    updateTaskState = {
                        tasksState[it.first] = it.second
                    }
                )
                if (words.isNotEmpty()) {
                    showTaskList = false
                    selectable = false
                }
                parsedList.addAll(words)
                filterState = if (shouldApplyFilters()) {
                    Filtering
                } else {
                    // 不用过滤
                    previewList.addAll(words)
                    End
                }

                if (errorMessages.isNotEmpty()) {
                    val string = "${errorMessages.size} adet dosya ayrıştırılamadı. Detaylar için lütfen [Görev Listesi]'ne tıklayın." // "有 ${errorMessages.size} 个文件解析失败，请点击 [任务列表] 查看详细信息" -> Türkçe çevirisi
                    JOptionPane.showMessageDialog(window, string)
                }
            }

        }

        /**
         * 手动点击删除的单词，一般都是熟悉的词，
         * 所有需要添加到熟悉词库
         */
        val removeWord: (Word) -> Unit = { word ->
            val tempWord = word.deepCopy()
            // 如果是过滤词库，同时过滤的是熟悉词库，要把删除的单词从内存中的熟悉词库删除
            if (state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary") {
                familiarVocabulary.wordList.remove(tempWord)
            } else {
                // 用字幕生成的词库和用 MKV 生成的词库，需要把内部字幕转换为外部字幕
                if (tempWord.captions.isNotEmpty()) {
                    tempWord.captions.forEach { caption ->
                        val externalCaption = ExternalCaption(
                            relateVideoPath = relateVideoPath,
                            subtitlesTrackId = selectedTrackId,
                            subtitlesName = File(selectedFilePath).nameWithoutExtension,
                            start = caption.start,
                            end = caption.end,
                            content = caption.content
                        )
                        tempWord.externalCaptions.add(externalCaption)
                    }
                    tempWord.captions.clear()
                }

                // 把单词添加到熟悉词库
                if (!familiarVocabulary.wordList.contains(tempWord)) {
                    familiarVocabulary.wordList.add(tempWord)
                }
            }

            try {
                familiarVocabulary.size = familiarVocabulary.wordList.size
                val familiarFile = getFamiliarVocabularyFile()
                saveVocabulary(familiarVocabulary.serializeVocabulary, familiarFile.absolutePath)
                previewList.remove(word)
                removedWords.add(word)
            } catch (e: Exception) {
                // Geri al
                if (state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary") {
                    familiarVocabulary.wordList.add(tempWord)
                } else {
                    familiarVocabulary.wordList.remove(tempWord)
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "Tanıdık kelime listesi kaydedilemedi. Hata:\n${e.message}") // "保存熟悉词库失败,错误信息：\n${e.message}" -> Türkçe çevirisi
            }

        }


        Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(bottom = 60.dp)
                    .background(MaterialTheme.colors.background)
            ) {
                Divider()
                Row(Modifier.fillMaxWidth()) {
                    // 左边的过滤区
                    val width = if (vocabularyFilterList.isEmpty()) 380.dp else 450.dp
                    Column(Modifier.width(width).fillMaxHeight()) {
                        BasicFilter(
                            filter = filter,
                            changeFilter = {
                                filter = it
                                scope.launch(Dispatchers.Default) {
                                    include = !include
                                    if (started) {
                                        filterState = Filtering
                                    }
                                }

                            },
                            include = include,
                            changeInclude = {
                                include = it
                                scope.launch(Dispatchers.Default) {
                                    filter = !filter
                                    if (started) {
                                        filterState = Filtering
                                    }
                                }

                            },
                            showMaxSentenceLength = (type == DOCUMENT && title != "过滤词库"),
                            numberFilter = numberFilter,
                            changeNumberFilter = {
                                numberFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            bncNum = state.global.bncNum,
                            setBncNum = { state.global.bncNum = it },
                            maxSentenceLength = state.global.maxSentenceLength,
                            setMaxSentenceLength = { state.global.maxSentenceLength = it },
                            bncNumFilter = bncNumberFilter,
                            changeBncNumFilter = {
                                bncNumberFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            frqNum = state.global.frqNum,
                            setFrqNum = { state.global.frqNum = it },
                            frqNumFilter = frqNumFilter,
                            changeFrqFilter = {
                                frqNumFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            bncZeroFilter = bncZeroFilter,
                            changeBncZeroFilter = {
                                bncZeroFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            frqZeroFilter = frqZeroFilter,
                            changeFrqZeroFilter = {
                                frqZeroFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            replaceToLemma = replaceToLemma,
                            setReplaceToLemma = {
                                replaceToLemma = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                        )
                        VocabularyFilter(
                            futureFileChooser = state.futureFileChooser,
                            vocabularyFilterList = vocabularyFilterList,
                            vocabularyFilterListAdd = {
                                if (!vocabularyFilterList.contains(it)) {
                                    vocabularyFilterList.add(it)
                                    if (started) {
                                        filterState = Filtering
                                    }
                                }
                            },
                            vocabularyFilterListRemove = {
                                vocabularyFilterList.remove(it)
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            recentList = state.recentList,
                            removeInvalidRecentItem = {
                                state.removeRecentItem(it)
                            },
                            familiarVocabulary = familiarVocabulary,
                            updateFamiliarVocabulary = {
                                val wordList = loadMutableVocabularyByName("FamiliarVocabulary").wordList
                                familiarVocabulary.wordList.addAll(wordList)
                            }
                        )
                    }
                    Divider(Modifier.width(1.dp).fillMaxHeight())
                    // 生成词库区
                    Column(
                        Modifier.fillMaxWidth().fillMaxHeight().background(MaterialTheme.colors.background)
                    ) {

                        SelectFile(
                            type = type,
                            selectedFileList = selectedFileList,
                            selectedFilePath = selectedFilePath,
                            setSelectedFilePath = { selectedFilePath = it },
                            selectedSubtitle = selectedSubtitlesName,
                            setSelectedSubtitle = { selectedSubtitlesName = it },
                            setRelateVideoPath = { relateVideoPath = it },
                            relateVideoPath = relateVideoPath,
                            trackList = trackList,
                            selectedTrackId = selectedTrackId,
                            setSelectedTrackId = { selectedTrackId = it },
                            showTaskList = showTaskList,
                            showTaskListEvent = {
                                showTaskList = !showTaskList
                                if (!showTaskList) {
                                    selectable = false
                                }
                            },
                            analysis = { pathName, trackId ->
                                analysis(pathName, trackId)
                            },
                            batchAnalysis = { batchAnalysis(it) },
                            selectable = selectable,
                            changeSelectable = { selectable = !selectable },
                            selectAll = { selectAll() },
                            delete = { delete() },
                            chooseText = chooseText,
                            openFile = { openFile() },
                            openRelateVideo = { openRelateVideo() },
                            started = started,
                            showEnablePhrases = title != "过滤词库",
                            enablePhrases = enablePhrases,
                            changeEnablePhrases = {
                                enablePhrases = it
                                filterState = Filtering
                            },
                        )

                        // 单词预览和任务列表
                        Box(Modifier.fillMaxSize()) {
                            if (started) {
                                when (filterState) {
                                    Parsing -> {
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                        ) {
                                            CircularProgressIndicator(
                                                Modifier.width(60.dp).padding(bottom = 60.dp)
                                            )
                                            Text(text = progressText, color = MaterialTheme.colors.onBackground)
                                        }
                                    }

                                    Filtering -> {
                                        CircularProgressIndicator(
                                            Modifier.width(60.dp).align(Alignment.Center)
                                        )
                                        scope.launch(Dispatchers.Default) {
                                            // 是否应该执行过滤或包含
                                            if (shouldApplyFilters()) {
                                                // 过滤词库
                                                if (filter) {
                                                    // 根据词频或原型过滤单词
                                                    val basicFilteredList = filterWords(
                                                        parsedList,
                                                        numberFilter,
                                                        state.global.bncNum,
                                                        bncNumberFilter,
                                                        state.global.frqNum,
                                                        frqNumFilter,
                                                        bncZeroFilter,
                                                        frqZeroFilter,
                                                        replaceToLemma,
                                                        selectedFileList.isNotEmpty()
                                                    )
                                                    // 根据选择的词库过滤单词
                                                    val filteredList = filterSelectVocabulary(
                                                        selectedFileList = vocabularyFilterList,
                                                        basicFilteredList = basicFilteredList
                                                    )
                                                    // 过滤手动删除的单词
                                                    filteredList.removeAll(removedWords)
                                                    previewList.clear()
                                                    previewList.addAll(filteredList)
                                                    filterState = End
                                                } else {// 包含词库
                                                    // 根据词频或原型包含单词
                                                    val basicIncludeList = includeWords(
                                                        parsedList,
                                                        numberFilter,
                                                        state.global.bncNum,
                                                        bncNumberFilter,
                                                        state.global.frqNum,
                                                        frqNumFilter,
                                                        bncZeroFilter,
                                                        frqZeroFilter,
                                                        replaceToLemma,
                                                        selectedFileList.isNotEmpty()
                                                    )
                                                    // 根据选择的词库包含单词
                                                    val includeList = includeSelectVocabulary(
                                                        selectedFileList = vocabularyFilterList,
                                                        parsedList = parsedList
                                                    )
                                                    includeList.addAll(basicIncludeList)

                                                    // 过滤手动删除的单词
                                                    includeList.removeAll(removedWords)
                                                    previewList.clear()
                                                    previewList.addAll(includeList)
                                                    filterState = End
                                                }
                                            } else {
                                                // 不用过滤或包含
                                                previewList.clear()
                                                previewList.addAll(parsedList)
                                                filterState = End
                                            }

                                        }
                                    }

                                    End -> {
                                        PreviewWords(
                                            previewList = previewList,
                                            summaryVocabulary = summaryVocabulary,
                                            removeWord = { removeWord(it) },
                                            sort = sort,
                                            changeSort = { sort = it },
                                            showCard = showCard,
                                            changeShowCard = { showCard = it }
                                        )
                                    }

                                    Idle -> {}
                                }
                            } else {
                                val text = when (type) {
                                    DOCUMENT -> {
                                        if (title !== "Kelime Listesini Filtrele") { // "过滤词库" -> "Kelime Listesini Filtrele"
                                            "Belgeleri buraya sürükleyip bırakabilirsiniz" // "可以拖放文档到这里" -> "Belgeleri buraya sürükleyip bırakabilirsiniz"
                                        } else {
                                            "Kelime listelerini buraya sürükleyip bırakabilirsiniz" // "可以拖放词库到这里" -> "Kelime listelerini buraya sürükleyip bırakabilirsiniz"
                                        }
                                    }

                                    SUBTITLES -> "SRT veya ASS altyazılarını buraya sürükleyip bırakabilirsiniz" // "可以拖放 SRT 或 ASS 字幕到这里" -> "SRT veya ASS altyazılarını buraya sürükleyip bırakabilirsiniz"
                                    MKV -> "MKV veya MP4 videolarını buraya sürükleyip bırakabilirsiniz" // "可以拖放 MKV 或 MP4 视频到这里" -> "MKV veya MP4 videolarını buraya sürükleyip bırakabilirsiniz"
                                }
                                if (!loading) {
                                    Text(
                                        text = text,
                                        color = MaterialTheme.colors.onBackground,
                                        style = MaterialTheme.typography.h6,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            if (loading) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                ) {
                                    CircularProgressIndicator(
                                        Modifier.width(60.dp).padding(bottom = 60.dp)
                                    )
                                    val text = if (selectedFileList.isNotEmpty()) {
                                        "İlk videonun altyazı izleme listesi okunuyor..." // "正在读取第一个视频的字幕轨道列表" -> "İlk videonun altyazı izleme listesi okunuyor..."
                                    } else {
                                        "Altyazı izleme listesi okunuyor..." // "正在读取字幕轨道列表" -> "Altyazı izleme listesi okunuyor..."
                                    }
                                    Text(text = text, color = MaterialTheme.colors.onBackground)
                                }
                            }
                            if (showTaskList) {
                                TaskList(
                                    selectedFileList = selectedFileList,
                                    updateOrder = {
                                        scope.launch {
                                            selectedFileList.clear()
                                            selectedFileList.addAll(it)
                                        }
                                    },
                                    tasksState = tasksState,
                                    currentTask = currentTask,
                                    errorMessages = errorMessages,
                                    selectable = selectable,
                                    checkedFileMap = checkedFileMap,
                                    checkedChange = {
                                        checkedFileMap[it.first] = it.second
                                    }
                                )
                            }
                        }
                    }
                }

            }
            // Bottom
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colors.background)
            ) {
                val fileName = File(selectedFilePath).nameWithoutExtension
                val saveEnabled = previewList.isNotEmpty()
                var saveOtherFormats by remember { mutableStateOf(false) }
                val vType = if (title == "过滤词库") {
                    filteringType
                } else if (selectedFileList.isNotEmpty()) {
                    DOCUMENT
                } else type
                Divider()
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    SaveButton(
                        enabled = saveEnabled,
                        saveClick = {
                            scope.launch(Dispatchers.Default) {
                                val fileChooser =
                                    withContext(Dispatchers.IO) {
                                        state.futureFileChooser.get()
                                    }
                                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                                fileChooser.dialogTitle = "Kelime Listesini Kaydet" // "保存词库" -> "Kelime Listesini Kaydet"
                                val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                                if (state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary") {
                                    fileChooser.selectedFile = File(selectedFilePath)
                                } else {
                                    fileChooser.selectedFile = File("$myDocuments${File.separator}$fileName.json")
                                }
                                val userSelection = fileChooser.showSaveDialog(window)
                                if (userSelection == JFileChooser.APPROVE_OPTION) {
                                    val selectedFile = fileChooser.selectedFile
                                    val vocabularyDirPath = Paths.get(getResourcesFile("vocabulary").absolutePath)
                                    val savePath = Paths.get(selectedFile.absolutePath)
                                    if (savePath.startsWith(vocabularyDirPath)) {
                                        JOptionPane.showMessageDialog(
                                            null,
                                            "Kelime listesi uygulama kurulum dizinine kaydedilemez, çünkü yazılım güncellendiğinde veya kaldırıldığında oluşturulan kelime listesi silinir." // "不能把词库保存到应用程序安装目录，因为软件更新或卸载时，生成的词库会被删除"
                                        )
                                    } else {
                                        val vocabulary = Vocabulary(
                                            name = selectedFile.nameWithoutExtension,
                                            type = vType,
                                            language = "english",
                                            size = previewList.size,
                                            relateVideoPath = relateVideoPath,
                                            subtitlesTrackId = selectedTrackId,
                                            wordList = previewList
                                        )
                                        try {
                                            saveVocabulary(vocabulary, selectedFile.absolutePath)
                                            state.saveToRecentList(vocabulary.name, selectedFile.absolutePath, 0)

                                            // Durumu temizle
                                            selectedFileList.clear()
                                            started = false
                                            showTaskList = false
                                            tasksState.clear()
                                            currentTask = null
                                            errorMessages.clear()
                                            selectedFilePath = ""
                                            selectedSubtitlesName = ""
                                            previewList.clear()
                                            parsedList.clear()
                                            relateVideoPath = ""
                                            selectedTrackId = 0
                                            filteringType = DOCUMENT
                                            trackList.clear()
                                            filterState = Idle
                                            vocabularyFilterList.clear()
                                            numberFilter = false
                                            frqNumFilter = false
                                            bncNumberFilter = false
                                            bncZeroFilter = false
                                            frqZeroFilter = false
                                            replaceToLemma = false
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            JOptionPane.showMessageDialog(
                                                window,
                                                "Kelime listesi kaydedilemedi. Hata:\n${e.message}" // "保存词库失败,错误信息：\n${e.message}" -> Türkçe çevirisi
                                            )
                                        }


                                    }


                                }
                            }


                        },
                        otherClick = { saveOtherFormats = true }
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = {
                        onCloseRequest(state, title)
                    }) {
                        Text("İptal") // "取消" -> "İptal"
                    }
                    Spacer(Modifier.width(10.dp))
                }

                if (saveOtherFormats) {
                    SaveOtherVocabulary(
                        fileName = fileName,
                        wordList = previewList,
                        vocabularyType = vType,
                        futureFileChooser = state.futureFileChooser,
                        colors = state.colors,
                        close = { saveOtherFormats = false }
                    )
                }
            }
        }

    }
}

/** FilterState 有四个状态：Idle、"Parse"、"Filtering"、"End" */
enum class FilterState {
    /** 空闲状态，预览区为空 */
    Idle,

    /** 正在解析文档或字幕 */
    Parsing,

    /** 正在过滤单词 */
    Filtering,

    /** 单词过滤完成，可以显示了*/
    End
}

@OptIn(ExperimentalSerializationApi::class)
private fun onCloseRequest(state: AppState, title: String) {
    when (title) {
        "过滤词库" -> state.filterVocabulary = false
        "用文档生成词库" -> state.generateVocabularyFromDocument = false
        "用字幕生成词库" -> state.generateVocabularyFromSubtitles = false
        "用视频生成词库" -> state.generateVocabularyFromVideo = false
    }

}

/** 返回单词的原型，如果没有原型，就返回单词本身 */
fun getWordLemma(word: Word): String {
    word.exchange.split("/").forEach { exchange ->
        val pair = exchange.split(":")
        if (pair[0] == "0") return pair[1]
    }
    return word.value
}

@Composable
fun Summary(
    list: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    sort: String,
    showCard: Boolean,
    changeShowCard: (Boolean) -> Unit,
    changeSort: (String) -> Unit,
) {

    Column(Modifier.fillMaxWidth()) {
        val height = 61.dp
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(height).padding(start = 10.dp)
        ) {
            val summary = computeSummary(list, summaryVocabulary)
            Text(text = "Toplam ${list.size} kelime  ", color = MaterialTheme.colors.onBackground) // "共 ${list.size} 词  " -> "Toplam ${list.size} kelime  "
            Text(text = "Oxford 5000: ", color = MaterialTheme.colors.onBackground) // "牛津5000核心词：" -> "Oxford 5000: "
            if (summaryVocabulary["oxford"]?.isEmpty() == true) {
                Text(text = "Liste eksik ", color = Color.Red) // "词库缺失 " -> "Liste eksik "
            } else {
                Text("${summary[0]} kelime  ", color = MaterialTheme.colors.onBackground) // "词  " -> "kelime  "
            }
            Text(text = "CET-4: ", color = MaterialTheme.colors.onBackground) // "四级：" -> "CET-4: "
            if (summaryVocabulary["cet4"]?.isEmpty() == true) {
                Text(text = "Liste eksik ", color = Color.Red)
            } else {
                Text("${summary[1]} kelime  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "CET-6: ", color = MaterialTheme.colors.onBackground) // "六级：" -> "CET-6: "
            if (summaryVocabulary["cet6"]?.isEmpty() == true) {
                Text(text = "Liste eksik ", color = Color.Red)
            } else {
                Text("${summary[2]} kelime  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "GRE: ", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["gre"]?.isEmpty() == true) {
                Text(text = "Liste eksik ", color = Color.Red)
            } else {
                Text("${summary[3]} kelime", color = MaterialTheme.colors.onBackground) // "词" -> "kelime"
            }


            var expanded by remember { mutableStateOf(false) }
            Box {
                val width = 220.dp // Genişliği artırdım çünkü Türkçe metinler daha uzun olabilir
                val text = when (sort) {
                    "appearance" -> "Görünüş Sırasına Göre" // "按出现的顺序排序" -> "Görünüş Sırasına Göre"
                    "bnc" -> "BNC Sıklığına Göre" // "按 BNC 词频排序" -> "BNC Sıklığına Göre"
                    "alphabet" -> "Alfabetik Sıraya Göre" // "按首字母排序" -> "Alfabetik Sıraya Göre"
                    else -> "COCA Sıklığına Göre" // "按 COCA 词频排序" -> "COCA Sıklığına Göre"
                }
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .width(width)
                        .padding(start = 10.dp)
                        .background(Color.Transparent)
                        .border(1.dp, Color.Transparent)
                ) {
                    Text(text = text)
                    Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(width)
                        .height(180.dp)
                ) {
                    val selectedColor = if (MaterialTheme.colors.isLight) Color(245, 245, 245) else Color(41, 42, 43)
                    val backgroundColor = Color.Transparent
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("alphabet")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "alphabet") selectedColor else backgroundColor) // "bnc" yerine "alphabet" olmalıydı
                    ) {
                        Text("Alfabetik Sıraya Göre") // "按首字母排序" -> "Alfabetik Sıraya Göre"
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("bnc")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "bnc") selectedColor else backgroundColor)
                    ) {
                        Text("BNC Sıklığına Göre") // "按BNC词频排序" -> "BNC Sıklığına Göre"
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("coca")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "coca") selectedColor else backgroundColor)
                    ) {
                        Text("COCA Sıklığına Göre") // "按COCA词频排序" -> "COCA Sıklığına Göre"

                    }


                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("appearance")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "appearance") selectedColor else backgroundColor)
                    ) {
                        Text("Görünüş Sırasına Göre") // "按出现的顺序排序" -> "Görünüş Sırasına Göre"
                    }

                }

            }
            IconButton(
                onClick = { changeShowCard(!showCard) },
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Icon(
                    if (showCard) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                    contentDescription = "Localized description",
                    tint = MaterialTheme.colors.onBackground
                )
            }
        }
        Divider()
    }


}

/**
 * 计算摘要
 */
private fun computeSummary(
    list: List<Word>,
    vocabularySummary: Map<String, List<String>>
): List<Int> {
    var oxfordCount = 0
    var cet4Count = 0
    var cet6Count = 0
    var greCount = 0
    list.forEach { word ->
        if (vocabularySummary["oxford"]?.contains(word.value) == true) {
            oxfordCount++
        }
        if (vocabularySummary["cet4"]?.contains(word.value) == true) {
            cet4Count++
        }
        if (vocabularySummary["cet6"]?.contains(word.value) == true) {
            cet6Count++
        }
        if (vocabularySummary["gre"]?.contains(word.value) == true) {
            greCount++
        }
    }

    return listOf(oxfordCount, cet4Count, cet6Count, greCount)
}

/**
 * 载入摘要词库
 */
private fun loadSummaryVocabulary(): Map<String, List<String>> {

    val oxford = loadVocabulary("vocabulary/牛津核心词/The_Oxford_5000.json").wordList
    val cet4 = loadVocabulary("vocabulary/大学英语/四级.json").wordList
    val cet6 = loadVocabulary("vocabulary/大学英语/六级.json").wordList
    val gre = loadVocabulary("vocabulary/出国/GRE.json").wordList

    val oxfordList = oxford.map { word -> word.value }
    val cet4List = cet4.map { word -> word.value }
    val cet6List = cet6.map { word -> word.value }
    val greList = gre.map { word -> word.value }

    val map = HashMap<String, List<String>>()
    map["oxford"] = oxfordList
    map["cet4"] = cet4List
    map["cet6"] = cet6List
    map["gre"] = greList

    return map
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicFilter(
    filter: Boolean,
    changeFilter: (Boolean) -> Unit,
    include: Boolean,
    changeInclude: (Boolean) -> Unit,
    showMaxSentenceLength: Boolean,
    numberFilter: Boolean,
    changeNumberFilter: (Boolean) -> Unit,
    bncNum: Int,
    setBncNum: (Int) -> Unit,
    maxSentenceLength: Int,
    setMaxSentenceLength: (Int) -> Unit,
    bncNumFilter: Boolean,
    changeBncNumFilter: (Boolean) -> Unit,
    frqNum: Int,
    setFrqNum: (Int) -> Unit,
    frqNumFilter: Boolean,
    changeFrqFilter: (Boolean) -> Unit,
    bncZeroFilter: Boolean,
    changeBncZeroFilter: (Boolean) -> Unit,
    frqZeroFilter: Boolean,
    changeFrqZeroFilter: (Boolean) -> Unit,
    replaceToLemma: Boolean,
    setReplaceToLemma: (Boolean) -> Unit,
) {
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (showMaxSentenceLength) {
                var maxLengthFieldValue by remember { mutableStateOf(TextFieldValue("$maxSentenceLength")) }
                Text(
                        "Cümledeki Maks. Kelime Sayısı ", // "单词所在句子的最大单词数 " -> "Cümledeki Maks. Kelime Sayısı "
                    color = MaterialTheme.colors.onBackground,
                    fontFamily = FontFamily.Default
                )
                BasicTextField(
                    value = maxLengthFieldValue,
                    onValueChange = { maxLengthFieldValue = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = LocalTextStyle.current.lineHeight,
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusable()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                val input = maxLengthFieldValue.text.toIntOrNull()
                                if (input != null && input >= 10) {
                                    setMaxSentenceLength(input)
                                } else {
                                    setMaxSentenceLength(10)
                                    maxLengthFieldValue = TextFieldValue("10")
                                        JOptionPane.showMessageDialog(null, "Cümledeki maksimum kelime sayısı 10'dan küçük olamaz.") // "单词所在句子的最大单词数不能小于 10"
                                }
                            }
                        }
                        .width(40.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                )
            }
        }
        Divider()
        val textWidth = 320.dp

        val textColor = MaterialTheme.colors.onBackground
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(61.dp)
        ) {
                Text("İçer", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default) // "包含" -> "İçer"
            Checkbox(
                checked = include,
                onCheckedChange = {
                    changeInclude(it)
                },
                modifier = Modifier.size(30.dp, 30.dp)
            )

            Spacer(Modifier.width(10.dp))
                Text("Filtrele", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default) // "过滤" -> "Filtrele"
            Checkbox(
                checked = filter,
                onCheckedChange = {
                    changeFilter(it)
                },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
            // Filtreleme Sıklığı
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                        Text("Filtrele ", color = MaterialTheme.colors.onBackground) // "过滤 " -> "Filtrele "
                }
                AnimatedVisibility(visible = include) {
                        Text("İçer ", color = MaterialTheme.colors.onBackground) // "包含 " -> "İçer "
                }
                Text(
                    "BNC", color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(end = 1.dp)
                )
                    Text(" sıklığı ilk ", color = MaterialTheme.colors.onBackground) // "   词频前 " -> " sıklığı ilk "
                var bncNumFieldValue by remember { mutableStateOf(TextFieldValue("$bncNum")) }
                BasicTextField(
                    value = bncNumFieldValue,
                    onValueChange = { bncNumFieldValue = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = LocalTextStyle.current.lineHeight,
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusable()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                val input = bncNumFieldValue.text.toIntOrNull()
                                if (input != null && input >= 0) {
                                    setBncNum(input)
                                } else {
                                    bncNumFieldValue = TextFieldValue("$bncNum")
                                        JOptionPane.showMessageDialog(null, "Sayı ayrıştırma hatası, varsayılan değere ayarlanacak.") // "数字解析错误，将设置为默认值"
                                }
                            }
                        }
                        .width(50.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                )
                    Text(" olan kelimeler", color = MaterialTheme.colors.onBackground) // " 的单词" -> " olan kelimeler"
            }
            Checkbox(
                checked = bncNumFilter,
                onCheckedChange = { changeBncNumFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                        Text("Filtrele ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                        Text("İçer ", color = MaterialTheme.colors.onBackground)
                }
                    Text("COCA sıklığı ilk ", color = MaterialTheme.colors.onBackground) // "COCA 词频前 " -> "COCA sıklığı ilk "
                var frqNumFieldValue by remember { mutableStateOf(TextFieldValue("$frqNum")) }
                BasicTextField(
                    value = frqNumFieldValue,
                    onValueChange = { frqNumFieldValue = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = LocalTextStyle.current.lineHeight,
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusable()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                val input = frqNumFieldValue.text.toIntOrNull()
                                if (input != null && input >= 0) {
                                    setFrqNum(input)
                                } else {
                                    frqNumFieldValue = TextFieldValue("$frqNum")
                                        JOptionPane.showMessageDialog(null, "Sayı ayrıştırma hatası, varsayılan değere ayarlanacak.")
                                }
                            }
                        }
                        .width(50.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                )
                    Text(" olan kelimeler", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = frqNumFilter,
                onCheckedChange = { changeFrqFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {


            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                        Text("Filtrele ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                        Text("İçer ", color = MaterialTheme.colors.onBackground)
                }
                    Text("tüm ", color = MaterialTheme.colors.onBackground) // "所有 " -> "tüm "

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                                Text(text = "İngiliz Ulusal Dil Havuzu", modifier = Modifier.padding(10.dp)) // "英国国家语料库" -> "İngiliz Ulusal Dil Havuzu"
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {

                    Text("BNC", color = blueColor,
                        modifier = Modifier
                            .clickable {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                        .isSupported(Desktop.Action.BROWSE)
                                ) {
                                    Desktop.getDesktop().browse(URI("https://www.natcorp.ox.ac.uk/"))
                                }
                            }
                            .pointerHoverIcon(Hand)
                            .padding(end = 3.dp))
                }

                    Text("   dil havuzunda sıklığı 0 olan kelimeler", color = textColor) // "   语料库词频顺序为0的词" -> "   dil havuzunda sıklığı 0 olan kelimeler"
            }
            Checkbox(
                checked = bncZeroFilter,
                onCheckedChange = { changeBncZeroFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                        Text("Filtrele ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                        Text("İçer ", color = MaterialTheme.colors.onBackground)
                }
                    Text("tüm ", color = MaterialTheme.colors.onBackground)
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                                Text(text = "Amerikan Çağdaş İngilizce Dil Havuzu", modifier = Modifier.padding(10.dp)) // "美国当代英语语料库" -> "Amerikan Çağdaş İngilizce Dil Havuzu"
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {
                    Text("COCA", color = blueColor,
                        modifier = Modifier.clickable {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                    .isSupported(Desktop.Action.BROWSE)
                            ) {
                                Desktop.getDesktop().browse(URI("https://www.english-corpora.org/coca/"))
                            }
                        }
                            .pointerHoverIcon(Hand))
                }

                    Text(" dil havuzunda sıklığı 0 olan kelimeler", color = textColor) // " 语料库词频顺序为0的词" -> " dil havuzunda sıklığı 0 olan kelimeler"
            }
            Checkbox(
                checked = frqZeroFilter,
                onCheckedChange = { changeFrqZeroFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                        Text("Filtrele ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                        Text("İçer ", color = MaterialTheme.colors.onBackground)
                }
                    Text("tüm sayıları ", color = MaterialTheme.colors.onBackground) // "所有数字 " -> "tüm sayıları "
            }
            Checkbox(
                checked = numberFilter,
                onCheckedChange = { changeNumberFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                    "Kelime kökünü bul (Lemmatization), örn:\ndid, done, doing, does hepsi 'do' ile değiştirilir.", // "词形还原，例如：\ndid、done、doing、does 全部替换为 do"
                fontFamily = FontFamily.Default,
                color = textColor,
                modifier = Modifier.width(textWidth)
            )
            Checkbox(
                checked = replaceToLemma,
                enabled = filter,
                onCheckedChange = { setReplaceToLemma(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
    }

}

@Composable
fun VocabularyFilter(
    futureFileChooser: FutureTask<JFileChooser>,
    vocabularyFilterList: List<File>,
    vocabularyFilterListAdd: (File) -> Unit,
    vocabularyFilterListRemove: (File) -> Unit,
    recentList: List<RecentItem>,
    removeInvalidRecentItem: (RecentItem) -> Unit,
    familiarVocabulary: MutableVocabulary,
    updateFamiliarVocabulary: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {

        Column(Modifier.width(180.dp).fillMaxHeight().background(MaterialTheme.colors.background)) {

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        getResourcesFile("vocabulary/Universite_Ingilizcesi/CET4.json").let { // Yol güncellendi (Varsayımsal, gerçek adlar farklı olabilir)
                            if (!vocabularyFilterList.contains(it)) {
                                vocabularyFilterListAdd(it)
                            }
                        }
                    }
            ) {
                Text("CET-4", color = MaterialTheme.colors.onBackground) // "四级" -> "CET-4"
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        getResourcesFile("vocabulary/Universite_Ingilizcesi/CET6.json").let { // Yol güncellendi (Varsayımsal)
                            if (!vocabularyFilterList.contains(it)) {
                                vocabularyFilterListAdd(it)
                            }
                        }
                    }
            ) {
                Text("CET-6", color = MaterialTheme.colors.onBackground) // "六级" -> "CET-6"
            }
            var expanded by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().height(40.dp)
                .background(MaterialTheme.colors.background)
                .clickable { expanded = true }
            ) {
                Text(
                    text = "Dahili Kelime Listesi", // "内置词库" -> "Dahili Kelime Listesi"
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )
                BuiltInVocabularyMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    addVocabulary = { file ->
                        if (!vocabularyFilterList.contains(file)) {
                            vocabularyFilterListAdd(file)
                        }
                    }
                )
            }


            var showDialog by remember { mutableStateOf(false) }
            if (showDialog) {
                FamiliarDialog(
                    futureFileChooser = futureFileChooser,
                    close = {
                        showDialog = false
                        updateFamiliarVocabulary()
                    },

                    )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        if (familiarVocabulary.wordList.isEmpty()) {
                            val result = JOptionPane.showConfirmDialog(
                                null,
                                "Tanıdık kelime listesinde henüz kelime yok. Kelime aktarmak ister misiniz?", // "熟悉词库现在还没有单词，是否导入单词到熟悉词库"
                                "Onay", // Pencere başlığı için bir öneri
                                JOptionPane.YES_NO_OPTION
                            )
                            if (result == 0) {
                                showDialog = true
                            }
                        } else {
                            val familiarFile = getFamiliarVocabularyFile()
                            vocabularyFilterListAdd(File(familiarFile.absolutePath))
                        }
                    }
            ) {
                if (familiarVocabulary.wordList.isNotEmpty()) {
                    BadgedBox(badge = {
                        Badge {
                            val badgeNumber = "${familiarVocabulary.wordList.size}"
                            Text(
                                badgeNumber,
                                modifier = Modifier.semantics {
                                    contentDescription = "$badgeNumber yeni bildirim" // "new notifications" -> "yeni bildirim"
                                }
                            )
                        }
                    }) {
                        Text(text = "Tanıdık Kelimeler", color = MaterialTheme.colors.onBackground) // "熟悉词库" -> "Tanıdık Kelimeler"
                    }
                } else {
                    Text(text = "Tanıdık Kelimeler", color = MaterialTheme.colors.onBackground) // "熟悉词库" -> "Tanıdık Kelimeler"
                }
            }

            if (recentList.isNotEmpty()) {
                var expandRecent by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth().height(40.dp).clickable { expandRecent = true }) {
                    Text(
                        text = "Son Kullanılanlar", // "最近词库" -> "Son Kullanılanlar"
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    val dropdownMenuHeight = if (recentList.size <= 10) (recentList.size * 40 + 20).dp else 420.dp
                    DropdownMenu(
                        expanded = expandRecent,
                        onDismissRequest = { expandRecent = false },
                        offset = DpOffset(20.dp, 0.dp),
                        modifier = Modifier
                            .widthIn(min = 300.dp, max = 700.dp)
                            .width(IntrinsicSize.Max)
                            .height(dropdownMenuHeight)
                    ) {
                        Box(Modifier.fillMaxWidth().height(dropdownMenuHeight)) {
                            val stateVertical = rememberScrollState(0)
                            Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                                Column {
                                    recentList.forEach { recentItem ->

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                                .clickable {
                                                    val recentFile = File(recentItem.path)
                                                    if (recentFile.exists()) {
                                                        vocabularyFilterListAdd(recentFile)
                                                    } else {
                                                        // Dosya silinmiş olabilir
                                                        removeInvalidRecentItem(recentItem)
                                                        JOptionPane.showMessageDialog(
                                                            null,
                                                            "Dosya yolu hatası:\n${recentItem.path}" // "文件地址错误：\n" -> "Dosya yolu hatası:\n"
                                                        )
                                                    }

                                                }
                                        ) {
                                            Text(
                                                text = recentItem.name, // Bu zaten Türkçe olmalı
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                                color = MaterialTheme.colors.onBackground,
                                                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                                            )

                                        }

                                    }

                                }
                            }

                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(
                                    scrollState = stateVertical
                                )
                            )
                        }

                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        scope.launch(Dispatchers.Default) {
                            val fileChooser = withContext(Dispatchers.IO) {
                                futureFileChooser.get()
                            }
                            fileChooser.dialogTitle = "Kelime Listesi Seç" // "选择词库" -> "Kelime Listesi Seç"
                            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                            fileChooser.isAcceptAllFileFilterUsed = false
                            val fileFilter = FileNameExtensionFilter("Kelime Listesi (*.json)", "json") // "词库" -> "Kelime Listesi (*.json)"
                            fileChooser.addChoosableFileFilter(fileFilter)
                            fileChooser.selectedFile = null
                            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val file = fileChooser.selectedFile
                                vocabularyFilterListAdd(File(file.absolutePath))
                            }
                            fileChooser.selectedFile = null
                            fileChooser.removeChoosableFileFilter(fileFilter)
                        }
                    }
            ) {
                Text("Kelime Listesi Seç", color = MaterialTheme.colors.onBackground) // "选择词库" -> "Kelime Listesi Seç"
            }

        }
        Divider(Modifier.width(1.dp).fillMaxHeight())
        Column(
            Modifier.width(270.dp).fillMaxHeight()
                .background(MaterialTheme.colors.background)
        ) {
            SelectedList(
                vocabularyFilterList,
                removeFile = {
                    vocabularyFilterListRemove(it)
                })
        }
    }
}

@Composable
fun SelectedList(
    list: List<File>,
    removeFile: (File) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LazyColumn {
            items(list) { file ->

                Box(
                    modifier = Modifier.clickable {}
                        .fillMaxWidth()
                ) {
                    var name = file.nameWithoutExtension
                    if (file.parentFile.nameWithoutExtension == "人教版英语" ||
                        file.parentFile.nameWithoutExtension == "外研版英语" ||
                        file.parentFile.nameWithoutExtension == "北师大版高中英语"
                    ) {
                        if (name.contains(" ")) {
                            name = name.split(" ")[1]
                        }
                    }
                    Text(
                        text = name,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart).width(225.dp).padding(10.dp)
                    )

                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .clickable { removeFile(file) }
                            .align(Alignment.CenterEnd)
                            .size(30.dp, 30.dp)

                    )
                }
            }
        }
    }


}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectFile(
    type: VocabularyType,
    selectedFileList: List<File>,
    selectedFilePath: String,
    setSelectedFilePath: (String) -> Unit,
    selectedSubtitle: String,
    setSelectedSubtitle: (String) -> Unit,
    setRelateVideoPath: (String) -> Unit,
    relateVideoPath: String,
    trackList: List<Pair<Int, String>>,
    selectedTrackId: Int,
    setSelectedTrackId: (Int) -> Unit,
    showTaskList: Boolean,
    showTaskListEvent: () -> Unit,
    analysis: (String, Int) -> Unit,
    batchAnalysis: (String) -> Unit,
    selectable: Boolean,
    changeSelectable: () -> Unit,
    selectAll: () -> Unit,
    delete: () -> Unit,
    chooseText: String,
    openFile: () -> Unit,
    openRelateVideo: () -> Unit,
    started: Boolean,
    showEnablePhrases: Boolean,
    enablePhrases: Boolean,
    changeEnablePhrases: (Boolean) -> Unit,
) {

    Column(Modifier.height(IntrinsicSize.Max)) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(start = 10.dp)
        ) {
            Text(chooseText, color = MaterialTheme.colors.onBackground)
            if (type == SUBTITLES || type == DOCUMENT) {
                Spacer(Modifier.width(75.dp))
            } else if (type == MKV) {
                Spacer(Modifier.width(24.dp))
            }
            BasicTextField(
                value = selectedFilePath,
                onValueChange = {
                    setSelectedFilePath(it)
                },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                textStyle = TextStyle(
                    lineHeight = 29.sp,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onBackground
                ),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
                    ) {
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .width(300.dp)
                    .padding(start = 8.dp, end = 10.dp)
                    .height(35.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            OutlinedButton(onClick = { openFile() }) {
                Text("Aç", fontSize = 12.sp) // "打开" -> "Aç"
            }

            Spacer(Modifier.width(10.dp))
            val startEnable = if (type != MKV) {
                selectedFilePath.isNotEmpty()
            } else selectedSubtitle != "    " || selectedFileList.isNotEmpty()

            OutlinedButton(
                enabled = startEnable,
                onClick = {
                    if (selectedFileList.isEmpty()) {
                        analysis(selectedFilePath, selectedTrackId)
                    } else {
                        batchAnalysis("English")
                    }

                }) {
                Text("Başlat", fontSize = 12.sp) // "开始" -> "Başlat"
            }
            Spacer(Modifier.width(20.dp))
            if (showEnablePhrases) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Kelime Gruplarını İşle ", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default) // "处理词组 " -> "Kelime Gruplarını İşle "
                    Checkbox(
                        checked = enablePhrases,
                        onCheckedChange = {
                            changeEnablePhrases(it)
                            // Eğer zaten başladıysa, yeniden başlat
                            if (started) {
                                if (selectedFileList.isEmpty()) {
                                    analysis(selectedFilePath, selectedTrackId)
                                } else {
                                    batchAnalysis("English")
                                }
                            }
                        },
                        modifier = Modifier.size(30.dp, 30.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))
            if (chooseText != "Kelime Listesi Seç") { // "选择词库" -> "Kelime Listesi Seç"
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "Yardım Belgesi", modifier = Modifier.padding(10.dp)) // "帮助文档" -> "Yardım Belgesi"
                        }
                    },
                    delayMillis = 50,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    var documentWindowVisible by remember { mutableStateOf(false) }
                    var currentPage by remember { mutableStateOf("document") }
                    IconButton(onClick = {
                        documentWindowVisible = true
                        currentPage = when (type) {
                            DOCUMENT -> "document"
                            SUBTITLES -> "subtitles"
                            MKV -> "matroska"
                        }
                    }) {
                        Icon(
                            Icons.Filled.Help,
                            contentDescription = "Localized description",
                            tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                        )
                    }


                    if (documentWindowVisible) {
                        DocumentWindow(
                            close = { documentWindowVisible = false },
                            currentPage = currentPage,
                            setCurrentPage = { currentPage = it }

                        )
                    }
                }
            }

        }

        if ((selectedFilePath.isNotEmpty() || selectedFileList.isNotEmpty()) && type == MKV) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                if (trackList.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "Altyazı Seç ", // "选择字幕 " -> "Altyazı Seç "
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 75.dp)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.width(IntrinsicSize.Max)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .width(282.dp)
                                    .background(Color.Transparent)
                                    .border(1.dp, Color.Transparent)
                            ) {
                                Text(
                                    text = selectedSubtitle, fontSize = 12.sp, // selectedSubtitle zaten dinamik olarak ayarlanıyor
                                )
                                Icon(
                                    Icons.Default.ExpandMore, contentDescription = "Localized description",
                                    modifier = Modifier.size(20.dp, 20.dp)
                                )
                            }
                            val dropdownMenuHeight = (trackList.size * 40 + 20).dp
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(282.dp)
                                    .height(dropdownMenuHeight)
                            ) {
                                trackList.forEach { (index, description) ->
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            setSelectedSubtitle(description)
                                            setSelectedTrackId(index)
                                        },
                                        modifier = Modifier.width(282.dp).height(40.dp)
                                    ) {
                                        Text(
                                            text = "$description ", fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }


                            }

                        }

                    }
                } else {
                    // 批量处理，现在只能批量处理英语字幕，所以就写死了。
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "选择字幕 ",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 75.dp)
                        )
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier
                                .width(282.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Color.Transparent)
                        ) {
                            Text(
                                text = "英语", fontSize = 12.sp,
                            )
                        }
                    }
                }


                if (selectedFileList.isNotEmpty()) {
                    OutlinedButton(onClick = { showTaskListEvent() }) {
                        Text("Görev Listesi", fontSize = 12.sp) // "任务列表" -> "Görev Listesi"
                    }
                    if (showTaskList) {
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { changeSelectable() }) {
                            Text("Seç", fontSize = 12.sp) // "选择" -> "Seç"
                        }
                    }
                    if (selectable) {
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { selectAll() }) {
                            Text("Tümünü Seç", fontSize = 12.sp) // "全选" -> "Tümünü Seç"
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { delete() }) {
                            Text("Sil", fontSize = 12.sp) // "删除" -> "Sil"
                        }
                    }
                }
            }
        }

        if (type == SUBTITLES && selectedFilePath.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                Text("İlgili Videoyu Seç (İsteğe Bağlı)", color = MaterialTheme.colors.onBackground) // "选择对应的视频(可选)" -> "İlgili Videoyu Seç (İsteğe Bağlı)"
                BasicTextField(
                    value = relateVideoPath,
                    onValueChange = setRelateVideoPath,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = 29.sp,
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(start = 8.dp, end = 10.dp)
                        .height(35.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                )
                OutlinedButton(onClick = { openRelateVideo() }) {
                    Text("Aç") // "打开" -> "Aç"
                }
            }
        }
        Divider()
    }
}


fun filterWords(
    inputWords: List<Word>,
    numberFilter: Boolean,
    bncNum: Int,
    bncNumFilter: Boolean,
    frqNum: Int,
    frqNumFilter: Boolean,
    bncZeroFilter: Boolean,
    frqZeroFilter: Boolean,
    replaceToLemma: Boolean,
    isBatchMKV: Boolean
): List<Word> {
    val resultList = ArrayList(inputWords)

    /**
     * Key 为需要转换为原型的单词，
     *  Value 是 Key 的原型词，还没有查词典，有可能词典里面没有。
     */
    val lemmaMap = HashMap<Word, String>()

    /** 原型词 > 内部字幕列表 映射 */
    val captionsMap = HashMap<String, MutableList<Caption>>()

    /** 原型词 -> 外部字幕列表映射,批量生成 MKV 词库时，字幕保存在单词的外部字幕列表 */
    val externalCaptionsMap = HashMap<String, MutableList<ExternalCaption>>()

    /** 原型词 -> 例句列表映射 */
    val sentencesMap = HashMap<String, MutableList<String>>()

    inputWords.forEach { word ->

        if (numberFilter && (word.value.toDoubleOrNull() != null)) {
            // 过滤数字
            resultList.remove(word)
        } else if (bncNumFilter && (word.bnc!! in 1 until bncNum)) {
            // 过滤最常见的词
            resultList.remove(word)
        } else if (frqNumFilter && (word.frq!! in 1 until frqNum)) {
            // 过滤最常见的词
            resultList.remove(word)
        } else if (bncZeroFilter && word.bnc == 0) {
            // 过滤 BNC 词频为 0 的词
            resultList.remove(word)
        } else if (frqZeroFilter && word.frq == 0) {
            // 过滤 COCA 词频为 0 的词
            resultList.remove(word)
        }


        if (replaceToLemma) {
            val lemma = getWordLemma(word)
            if (lemma.isNotEmpty()) {
                lemmaMap[word] = lemma
                // 处理内部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                if (!isBatchMKV) {
                    if (captionsMap[lemma].isNullOrEmpty()) {
                        captionsMap[lemma] = word.captions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<Caption>()
                        list.addAll(captionsMap[lemma]!!)
                        for (caption in word.captions) {
                            if (list.size < 3) {
                                list.add(caption)
                            }
                        }
                        captionsMap[lemma] = list
                    }
                    // 处理外部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                } else {
                    if (externalCaptionsMap[lemma].isNullOrEmpty()) {
                        externalCaptionsMap[lemma] = word.externalCaptions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<ExternalCaption>()
                        list.addAll(externalCaptionsMap[lemma]!!)
                        for (externalCaption in word.externalCaptions) {
                            if (list.size < 3) {
                                list.add(externalCaption)
                            }
                        }
                        externalCaptionsMap[lemma] = list
                    }
                }

                // 处理例句,sentencesMap 最多只保留 3 个例句
                if (sentencesMap[lemma].isNullOrEmpty()) {
                    sentencesMap[lemma] = word.pos.split("\n").toMutableList()
                } else {
                    word.pos.split("\n").forEach {
                        if (sentencesMap[lemma]!!.size < 3) {
                            sentencesMap[lemma]!!.add(it)
                        }
                    }
                }

            }
        }
    }

    //替换原型需要特殊处理
    if (replaceToLemma) {
        // 查询单词原型
        val queryList = lemmaMap.values.toList()
        val lemmaList = Dictionary.queryList(queryList)
        val validLemmaMap = HashMap<String, Word>()
        lemmaList.forEach { lemmaWord ->
            // 处理内部字幕
            if (!isBatchMKV) {
                val captions = captionsMap[lemmaWord.value]!!
                lemmaWord.captions = captions
                // 处理外部字幕
            } else {
                val externalCaptions = externalCaptionsMap[lemmaWord.value]!!
                lemmaWord.externalCaptions = externalCaptions
            }
            // 处理例句
            val sentences = sentencesMap[lemmaWord.value]!!
            if(sentences.isNotEmpty()) {
                lemmaWord.pos = sentences.joinToString("\n")
            }
            validLemmaMap[lemmaWord.value] = lemmaWord
        }

        val toLemmaList = lemmaMap.keys
        for (word in toLemmaList) {
            val index = resultList.indexOf(word)
            // 有一些词可能 属于 BNC 或 FRQ 为 0 的词，已经被过滤了，所以 index 为 -1
            if (index != -1) {
                val lemmaStr = lemmaMap[word]
                val validLemma = validLemmaMap[lemmaStr]
                if (validLemma != null) {
                    resultList.remove(word)
                    if (!resultList.contains(validLemma)) {
                        // 默认 add 为真
                        var add = true
                        // 但是，如果单词的词频为 0 或者是最常见的单词就不添加
                        if (bncNumFilter && (validLemma.bnc!! in 1 until bncNum)) {
                            add = false
                        } else if (frqNumFilter && (validLemma.frq!! in 1 until frqNum)) {
                            add = false
                        } else if (bncZeroFilter && validLemma.bnc == 0) {
                            add = false
                        } else if (frqZeroFilter && validLemma.frq == 0) {
                            add = false
                        }

                        if (add) {
                            resultList.add(index, validLemma)
                        }
                    }
                }

            }

        }
    }

    return resultList
}

fun includeWords(
    inputWords: List<Word>,
    numberFilter: Boolean,
    bncNum: Int,
    bncNumFilter: Boolean,
    frqNum: Int,
    frqNumFilter: Boolean,
    bncZeroFilter: Boolean,
    frqZeroFilter: Boolean,
    replaceToLemma: Boolean,
    isBatchMKV: Boolean
): List<Word> {
    val resultList = ArrayList<Word>()

    /**
     * Key 为需要转换为原型的单词，
     *  Value 是 Key 的原型词，还没有查词典，有可能词典里面没有。
     */
    val lemmaMap = HashMap<Word, String>()

    /** 原型词 > 内部字幕列表 映射 */
    val captionsMap = HashMap<String, MutableList<Caption>>()

    /** 原型词 -> 外部字幕列表映射,批量生成 MKV 词库时，字幕保存在单词的外部字幕列表 */
    val externalCaptionsMap = HashMap<String, MutableList<ExternalCaption>>()

    inputWords.forEach { word ->

        if (numberFilter && (word.value.toDoubleOrNull() != null)) {
            // 包含数字
            resultList.add(word)
        } else if (bncNumFilter && (word.bnc!! in 1 until bncNum)) {
            // 包含最常见的词
            resultList.add(word)
        } else if (frqNumFilter && (word.frq!! in 1 until frqNum)) {
            // 包含最常见的词
            resultList.add(word)
        } else if (bncZeroFilter && word.bnc == 0) {
            // 包含 BNC 词频为 0 的词
            resultList.add(word)
        } else if (frqZeroFilter && word.frq == 0) {
            // 包含 COCA 词频为 0 的词
            resultList.add(word)
        }


    }

    if (replaceToLemma) {
        resultList.forEach { word ->

            val lemma = getWordLemma(word)
            if (lemma.isNotEmpty()) {
                lemmaMap[word] = lemma
                // 处理内部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                if (!isBatchMKV) {
                    if (captionsMap[lemma].isNullOrEmpty()) {
                        captionsMap[lemma] = word.captions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<Caption>()
                        list.addAll(captionsMap[lemma]!!)
                        for (caption in word.captions) {
                            if (list.size < 3) {
                                list.add(caption)
                            }
                        }
                        captionsMap[lemma] = list
                    }
                    // 处理外部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                } else {
                    if (externalCaptionsMap[lemma].isNullOrEmpty()) {
                        externalCaptionsMap[lemma] = word.externalCaptions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<ExternalCaption>()
                        list.addAll(externalCaptionsMap[lemma]!!)
                        for (externalCaption in word.externalCaptions) {
                            if (list.size < 3) {
                                list.add(externalCaption)
                            }
                        }
                        externalCaptionsMap[lemma] = list
                    }
                }
            }

        }


        // 查询单词原型
        val queryList = lemmaMap.values.toList()
        val lemmaList = Dictionary.queryList(queryList)
        val validLemmaMap = HashMap<String, Word>()
        lemmaList.forEach { word ->
            // 处理内部字幕
            if (!isBatchMKV) {
                val captions = captionsMap[word.value]!!
                word.captions = captions
                // 处理外部字幕
            } else {
                val externalCaptions = externalCaptionsMap[word.value]!!
                word.externalCaptions = externalCaptions
            }
            validLemmaMap[word.value] = word
        }

        val toLemmaList = lemmaMap.keys
        for (word in toLemmaList) {
            val index = resultList.indexOf(word)
            // 有一些词可能 属于 BNC 或 FRQ 为 0 的词，已经被过滤了，所以 index 为 -1
            if (index != -1) {
                val lemmaStr = lemmaMap[word]
                val validLemma = validLemmaMap[lemmaStr]
                if (validLemma != null) {
                    resultList.remove(word)
                    if (!resultList.contains(validLemma)) {
                        // 默认 add 为真
                        var add = true
                        // 但是，如果单词的词频为 0 或者是最常见的单词就不添加
                        if (bncNumFilter && (validLemma.bnc!! in 1 until bncNum)) {
                            add = false
                        } else if (frqNumFilter && (validLemma.frq!! in 1 until frqNum)) {
                            add = false
                        } else if (bncZeroFilter && validLemma.bnc == 0) {
                            add = false
                        } else if (frqZeroFilter && validLemma.frq == 0) {
                            add = false
                        }

                        if (add) {
                            resultList.add(index, validLemma)
                        }
                    }
                }

            }

        }
    }


    return resultList
}

fun filterSelectVocabulary(
    selectedFileList: List<File>,
    basicFilteredList: List<Word>
): MutableList<Word> {
    val list = ArrayList(basicFilteredList)
    selectedFileList.forEach { file ->
        if (file.exists()) {
            val vocabulary = loadVocabulary(file.absolutePath)
            list.removeAll(vocabulary.wordList.toSet())
        } else {
            JOptionPane.showMessageDialog(null, "找不到词库：\n${file.absolutePath}")
        }

    }
    return list
}

fun includeSelectVocabulary(
    selectedFileList: List<File>,
    parsedList: List<Word>
): MutableList<Word> {
    val list = ArrayList(parsedList)
    val includeSet = mutableSetOf<Word>()
    selectedFileList.forEach { file ->
        if (file.exists()) {
            val vocabulary = loadVocabulary(file.absolutePath)

            includeSet.addAll(vocabulary.wordList)
        } else {
            JOptionPane.showMessageDialog(null, "找不到词库：\n${file.absolutePath}")
        }
    }
    // 交集 list 和 includeSet 的交集
    list.retainAll(includeSet)
    return list
}

/**
 * 预览单词
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PreviewWords(
    previewList: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    removeWord: (Word) -> Unit,
    sort: String,
    changeSort: (String) -> Unit,
    showCard: Boolean,
    changeShowCard: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        // 显示方式：卡片或列表

        Summary(
            previewList,
            summaryVocabulary,
            sort = sort, changeSort = { changeSort(it) },
            showCard = showCard, changeShowCard = changeShowCard
        )

        val sortedList = when (sort) {
            "alphabet" -> {
                val sorted = previewList.sortedBy { it.value }
                sorted
            }

            "bnc" -> {
                val sorted = previewList.sortedBy { it.bnc }
                val zeroBnc = mutableListOf<Word>()
                val greaterThanZero = mutableListOf<Word>()
                for (word in sorted) {
                    if (word.bnc == 0) {
                        zeroBnc.add(word)
                    } else {
                        greaterThanZero.add(word)
                    }
                }
                greaterThanZero.addAll(zeroBnc)
                greaterThanZero
            }

            "coca" -> {
                val sorted = previewList.sortedBy { it.frq }
                val zeroFrq = mutableListOf<Word>()
                val greaterThanZero = mutableListOf<Word>()
                for (word in sorted) {
                    if (word.frq == 0) {
                        zeroFrq.add(word)
                    } else {
                        greaterThanZero.add(word)
                    }
                }
                greaterThanZero.addAll(zeroFrq)
                greaterThanZero
            }

            else -> previewList
        }


        if (showCard) {
            val listGridState = rememberLazyGridState()
            Box(Modifier.fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp),
                    contentPadding = PaddingValues(15.dp),
                    modifier = Modifier
                        .fillMaxWidth(),
                    state = listGridState
                ) {
                    itemsIndexed(sortedList) { _: Int, word ->

                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape,
                                ) {
                                    Column(Modifier.padding(5.dp).width(200.dp)) {
                                        Text(
                                            text = word.value,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        val lemma = getWordLemma(word)
                                        Text(text = "原型:$lemma", fontSize = 12.sp)
                                        Row {
                                            Text(
                                                text = "BNC  ",
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(end = 2.dp)
                                            )
                                            Text(text = ":${word.bnc}", fontSize = 12.sp)
                                        }

                                        Text(text = "COCA:${word.frq}", fontSize = 12.sp)
                                        Divider()
                                        Text(
                                            text = word.translation,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                                        )
                                    }
                                }
                            },
                            delayMillis = 50,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomStart,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset.Zero
                            )
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(7.5.dp),
                                elevation = 3.dp
                            ) {
                                var closeVisible by remember { mutableStateOf(false) }
                                Box(Modifier.size(width = 130.dp, height = 65.dp)
                                    .onPointerEvent(PointerEventType.Enter) {
                                        closeVisible = true
                                    }
                                    .onPointerEvent(PointerEventType.Exit) {
                                        closeVisible = false
                                    }) {
                                    Text(
                                        text = word.value,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    if (closeVisible) {
                                        Icon(
                                            Icons.Filled.Close, contentDescription = "",
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .clickable {
                                                    scope.launch(Dispatchers.Default) {
                                                        removeWord(word)
                                                    }
                                                }
                                        )
                                    }


                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    style = LocalScrollbarStyle.current.copy(
                        shape = if (isWindows()) RectangleShape else RoundedCornerShape(
                            4.dp
                        )
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = listGridState
                    )
                )


            }
        } else {
            val listState = rememberLazyListState()
            var shiftPressed by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                            shiftPressed = true
                        }
                    } else if (event.type == KeyEventType.KeyUp) {
                        if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                            shiftPressed = false
                        }
                    }
                    false
                }
            ) {
                val selectedList = remember { mutableStateListOf<Word>() }
                var latestSelectedIndex by remember { mutableStateOf(-1) }
                val topPadding = if (selectedList.isNotEmpty()) 0.dp else 0.dp
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(top = topPadding).fillMaxSize()
                ) {
                    itemsIndexed(sortedList) { index: Int, word: Word ->
                        val selected = selectedList.contains(word)

                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                .background(if (selected) MaterialTheme.colors.onBackground.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    if (shiftPressed) {
                                        // 如果最后一次选择不存在（第一次选择），
                                        // 就选中当前的,并且把多选的开始设置为列表的第一个
                                        if (latestSelectedIndex == -1) {
                                            latestSelectedIndex = index
                                            val subList = sortedList.subList(0, index + 1)
                                            selectedList.addAll(subList)
                                        } else {
                                            // 如果最后一次选择存在，就选中最后一次选择到当前的
                                            val start = min(latestSelectedIndex, index)
                                            val end = max(latestSelectedIndex, index)
                                            val subList = sortedList.subList(start, end + 1)
                                            selectedList.addAll(subList)
                                        }
                                    } else {
                                        latestSelectedIndex = index
                                        if (!selected) {
                                            selectedList.add(word)
                                        } else {
                                            selectedList.remove(word)
                                        }
                                    }

                                },

                            ) {
                            Text(
                                text = word.value,
                                modifier = Modifier.padding(start = 20.dp).width(130.dp),
                                color = MaterialTheme.colors.onBackground,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(20.dp))
                            Text(
                                text = word.translation.replace("\n", "  "),
                                color = MaterialTheme.colors.onBackground,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (selectedList.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch(Dispatchers.Default) {
                                selectedList.forEach { removeWord(it) }
                                selectedList.clear()
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }

                VerticalScrollbar(
                    style = LocalScrollbarStyle.current.copy(
                        shape = if (isWindows()) RectangleShape else RoundedCornerShape(
                            4.dp
                        )
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = listState
                    )
                )
            }

        }

    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    selectedFileList: List<File>,
    updateOrder: (List<File>) -> Unit,
    tasksState: Map<File, Boolean>,
    currentTask: File?,
    errorMessages: Map<File, String>,
    selectable: Boolean,
    checkedFileMap: Map<File, Boolean>,
    checkedChange: (Pair<File, Boolean>) -> Unit,
) {

    val items = remember { mutableStateOf(selectedFileList) }
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        items.value = items.value.toMutableList().apply {
            add(to.index, removeAt(from.index))
            updateOrder(items.value)
        }
    })

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        LazyColumn(
            state = state.listState,
            modifier = Modifier.fillMaxSize().reorderable(state)
        ) {
            items(items.value, { it }) { item ->
                ReorderableItem(state, orientationLocked = true, key = item) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)
                    Column(
                        modifier = Modifier
                            .shadow(elevation.value)
                            .background(MaterialTheme.colors.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable { }
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .detectReorder(state)
                        ) {
                            Text(
                                text = item.nameWithoutExtension,
                                modifier = Modifier.align(Alignment.CenterStart).padding(top = 16.dp, bottom = 16.dp),
                                color = MaterialTheme.colors.onBackground
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                if (selectable) {
                                    val checked = checkedFileMap[item]

                                    Checkbox(
                                        checked = checked == true,
                                        onCheckedChange = { checkedChange(Pair(item, it)) }
                                    )
                                }


                                if (tasksState[item] == true) {
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                ),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = "Tamamlandı", modifier = Modifier.padding(10.dp)) // "完成" -> "Tamamlandı"
                                            }
                                        },
                                        delayMillis = 300,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.TopCenter,
                                            alignment = Alignment.TopCenter,
                                            offset = DpOffset.Zero
                                        ),
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Outlined.TaskAlt,
                                                contentDescription = "",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                    }


                                } else if (item == currentTask) {
                                    CircularProgressIndicator(
                                        Modifier
                                            .padding(start = 8.dp, end = 16.dp).width(24.dp).height(24.dp)
                                    )
                                } else if (tasksState[item] == false) {

                                    val text = errorMessages[item].orEmpty()
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                ),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = text, modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 300,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.CenterStart,
                                            alignment = Alignment.CenterStart,
                                            offset = DpOffset.Zero
                                        ),
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Outlined.Error,
                                                contentDescription = "",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }


                        }
                        Divider()
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(state.listState)
        )
    }


}


fun removeItalicSymbol(content: String): String {
    var string = content
    if (string.contains("<i>")) {
        string = string.replace("<i>", "")
    }
    if (string.contains("</i>")) {
        string = string.replace("</i>", "")
    }
    return string
}

/** 删除换行符，换行符替换为空格 */
fun removeNewLine(content: String): String {
    var string = content
    if (string.contains("\r\n")) {
        string = string.replace("\r\n", " ")
    }
    if (string.contains("\n")) {
        string = string.replace("\n", " ")
    }
    if (string.contains("<br />")) {
        string = string.replace("<br />", " ")
    }
    if (string.endsWith(" ")) {
        string = string.substring(0, string.length - 1)
    }
    return string
}

/** <br /> 替换为 \n  */
fun replaceNewLine(content: String): String {
    var string = content
    if (string.contains("<br />")) {
        string = string.replace("<br />", "\n")
    }
    return string
}