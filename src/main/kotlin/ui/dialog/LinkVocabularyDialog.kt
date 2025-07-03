package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import player.play
import state.AppState
import state.getResourcesFile
import ui.edit.computeNameMap
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.createTransferHandler
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView

/**
 * Altyazı Kelime Listesi Bağlama Penceresi
 * Altyazı kelime listesini belge kelime listesine bağlar.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class, ExperimentalFoundationApi::class)
@Composable
fun LinkVocabularyDialog(
    appState: AppState,
    close: () -> Unit
) {
    /**
     * Coroutine scope oluşturucu
     */
    val scope = rememberCoroutineScope()

    /**
     * Bağlanacak kelime listesi, genellikle dahili bir kelime listesidir
     */
    var vocabulary by remember{ mutableStateOf<MutableVocabulary?>(null) }



    /**
     * Seçilen altyazı kelime listesinin bulunduğu dizinin mutlak yolu
     */
    var vocabularyDir by remember { mutableStateOf("") }

    /**
     * Bağlanacak kelime listesinin bulunduğu dizinin mutlak yolu, örneğin CET-4 kelime listesine altyazı bağlanacaksa, bu adres CET-4 kelime listesinin bulunduğu dizindir.
     */
    var directoryPath by remember { mutableStateOf("") }

    /**
     * Mevcut kelime listesinin altyazı kelime listesine bağladığı altyazı sayısı
     */
    var linkCounter by remember { mutableStateOf(0) }

    /**
     * Bağlanmaya hazırlanan kelimeler ve altyazılar
     */
    val prepareLinks = remember { mutableStateMapOf<String, List<ExternalCaption>>() }


    /**
     * Altyazı adı
     */
    var subtitlesName by remember { mutableStateOf("") }

    var vocabularyType by remember { mutableStateOf(VocabularyType.DOCUMENT) }
    var vocabularyWrong by remember { mutableStateOf(false) } // Kelime listesi hatası
    var extractCaptionResultInfo by remember { mutableStateOf("") } // Altyazı çıkarma sonuç bilgisi
    var saveEnable by remember { mutableStateOf(false) } // Kaydet butonu aktif mi
    var showFilePicker by remember { mutableStateOf(false) } // Dosya seçici gösterilsin mi

    /**
     * [Bağla] tıklandığında çalıştırılacak geri çağrı fonksiyonu
     */
    val import: () -> Unit = {
        if (prepareLinks.isNotEmpty()) {
            vocabulary?.wordList?.forEach { word ->
                val links = prepareLinks[word.value]
                if (!links.isNullOrEmpty()) {
                    word.externalCaptions.addAll(links)
                }
            }
            saveEnable = true
        }
    }

    val clear: () -> Unit = {
        linkCounter = 0
        prepareLinks.clear()
        subtitlesName = ""
        extractCaptionResultInfo = ""
        vocabularyWrong = false
        vocabularyType = VocabularyType.DOCUMENT

    }

    /**
     * Kullanıcı altyazı kelime listesini seçtikten sonra ilgili bilgileri çıkarmak için bu fonksiyonu kullanın
     */
    val extractCaption: (File) -> Unit = {
        scope.launch (Dispatchers.Default){
                val selectedVocabulary = loadVocabulary(it.absolutePath)
                subtitlesName = if (selectedVocabulary.type == VocabularyType.SUBTITLES) selectedVocabulary.name else ""
                vocabularyType = selectedVocabulary.type
                var linkedCounter = 0

                // Altyazı kelime listesi veya MKV kelime listesi, altyazılar kelimenin captions özelliğinde saklanır
                if (selectedVocabulary.type != VocabularyType.DOCUMENT) {
                    val wordCaptionsMap = HashMap<String, List<Caption>>()
                    selectedVocabulary.wordList.forEach { word ->
                        wordCaptionsMap.put(word.value, word.captions)
                    }
                    vocabulary?.wordList?.forEach { word ->
                        if (wordCaptionsMap.containsKey(word.value.lowercase(Locale.getDefault()))) {
                            val captions = wordCaptionsMap[word.value]
                            val links = mutableListOf<ExternalCaption>()
                            // Önizleme için
                            // En fazla 3 altyazı, bu sayaç kalan sayıyı gösterir
                            var counter = 3 - word.externalCaptions.size
                            if (counter in 1..3) {
                                captions?.forEachIndexed { _, caption ->

                                    val externalCaption = ExternalCaption(
                                        selectedVocabulary.relateVideoPath,
                                        selectedVocabulary.subtitlesTrackId,
                                        subtitlesName,
                                        caption.start,
                                        caption.end,
                                        caption.content
                                    )

                                    if (counter != 0) {
                                        if (!word.externalCaptions.contains(externalCaption) && !links.contains(
                                                externalCaption
                                            )
                                        ) {
                                            links.add(externalCaption)
                                            counter--
                                        } else {
                                            linkedCounter++
                                        }
                                    }
                                }
                            } else {

                                // Altyazı zaten 3 tane, aynısı var mı diye kontrol et
                                captions?.forEachIndexed { _, caption ->
                                    val externalCaption = ExternalCaption(
                                        selectedVocabulary.relateVideoPath,
                                        selectedVocabulary.subtitlesTrackId,
                                        subtitlesName,
                                        caption.start,
                                        caption.end,
                                        caption.content
                                    )

                                    if (word.externalCaptions.contains(externalCaption)) {
                                        linkedCounter++
                                    }
                                }
                            }
                            if (links.isNotEmpty()) {
                                prepareLinks.put(word.value, links)
                                linkCounter += links.size
                            }

                        }
                    }

                } else {
                    // Belge kelime listesi, altyazılar kelimenin externalCaptions özelliğinde saklanır
                    val wordCaptionsMap = HashMap<String, List<ExternalCaption>>()
                    selectedVocabulary.wordList.forEach { word ->
                        wordCaptionsMap.put(word.value, word.externalCaptions)
                    }
                    vocabulary?.wordList?.forEach { word ->
                        if (wordCaptionsMap.containsKey(word.value.lowercase(Locale.getDefault()))) {
                            val externalCaptions = wordCaptionsMap[word.value]
                            val links = mutableListOf<ExternalCaption>()
//                        // Önizleme için
                            // En fazla 3 altyazı, bu sayaç kalan sayıyı gösterir
                            var counter = 3 - word.externalCaptions.size
                            if (counter in 1..3) {
                                externalCaptions?.forEachIndexed { _, externalCaption ->
                                    if (counter != 0) {
                                        if (!word.externalCaptions.contains(externalCaption) && !links.contains(
                                                externalCaption
                                            )
                                        ) {
                                            links.add(externalCaption)
                                            counter--
                                        } else {
                                            linkedCounter++
                                        }
                                    }
                                }
                            } else {
                                // Altyazı zaten 3 tane, aynısı var mı diye kontrol et
                                externalCaptions?.forEachIndexed { _, externalCaption ->
                                    if (word.externalCaptions.contains(externalCaption)) {
                                        linkedCounter++
                                    }
                                }
                            }
                            if (links.isNotEmpty()) {
                                prepareLinks.put(word.value, links)
                                linkCounter += links.size
                            }

                        }
                    }
                }

                // previewWords boşsa iki durum vardır:
                // 1. Zaten bir kez bağlandı.
                // 2. Eşleşen altyazı yok
                if (prepareLinks.isEmpty()) {
                    extractCaptionResultInfo = if (linkedCounter == 0) {
                        "Eşleşen altyazı bulunamadı, lütfen yeniden seçin."
                    } else {
                        "${selectedVocabulary.name} için ${linkedCounter} adet aynı altyazı zaten bağlanmış, lütfen yeniden seçin."
                    }
                    vocabularyWrong = true
                }
        }

    }

    /**
     * Giriş dosyasını işle
     */
    val handleInputFile:(File) -> Unit = {file ->
        // Belge kelime listesini veya dahili kelime listesini seç
        if(vocabulary == null){
            val newVocabulary =  MutableVocabulary(loadVocabulary(file.absolutePath))
            if(newVocabulary.type != VocabularyType.DOCUMENT){
                JOptionPane.showMessageDialog(null,
                    "Kelime listesi türü yanlış.\n" +
                        "Altyazı veya MKV videosundan oluşturulmuş kelime listeleri seçilemez.\n" +
                        "İki altyazılı kelime listesini bağlamak istiyorsanız, lütfen 'Kelime Listelerini Birleştir'i seçin."
                )
            }else{
                vocabulary = newVocabulary
            }
            directoryPath = file.parentFile.absolutePath
            // Altyazı kelime listesini seç
        }else{
            vocabularyDir = file.parentFile.absolutePath
            extractCaption(file)
        }
    }



    DialogWindow(
        title = "Altyazı Kelime Listesi Bağla",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = {
            clear()
            close()
        },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 650.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        // Pencerenin sürükle-bırak işleyicisini ayarla
        LaunchedEffect(Unit){
            val transferHandler = createTransferHandler(
                showWrongMessage = { message ->
                    JOptionPane.showMessageDialog(window, message)
                },
                parseImportFile = { files ->
                    val file = files.first()
                    scope.launch {
                        if (file.extension == "json") {
                            handleInputFile(file)
                        } else {
                            JOptionPane.showMessageDialog(window, "Kelime listesi formatı yanlış.")
                        }


                    }
                }
            )
            window.transferHandler = transferHandler
        }


        /** Kelime Listesini Kaydet */
        val save:() -> Unit = {
            scope.launch (Dispatchers.IO){

                val fileChooser = appState.futureFileChooser.get()
                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                fileChooser.dialogTitle = "Kelime Listesini Kaydet"
                val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                val appVocabulary = getResourcesFile("vocabulary")
                val parent = if (directoryPath.startsWith(appVocabulary.absolutePath)) {
                    myDocuments
                } else directoryPath
                fileChooser.selectedFile = File("$parent${File.separator}${vocabulary?.name}.json")
                val userSelection = fileChooser.showSaveDialog(window)
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    val fileToSave = fileChooser.selectedFile
                    try{
                        if (vocabulary != null) {
                            vocabulary!!.name = fileToSave.nameWithoutExtension
                            saveVocabulary(vocabulary!!.serializeVocabulary, fileToSave.absolutePath)
                        }
                        vocabulary = null
                        fileChooser.selectedFile = null
                        clear()
                        close()
                    }catch(e:Exception){
                        e.printStackTrace()
                        JOptionPane.showMessageDialog(window,"Kelime listesi kaydedilemedi. Hata:\n${e.message}")
                    }

                }
            }
        }

        WindowDraggableArea {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Divider(Modifier.align(Alignment.TopCenter))
                    if (prepareLinks.isEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize().align(Alignment.Center)
                        ) {
                            // Mevcut kelime listesine zaten bağlı olan harici altyazılar
                            val externalNameMap = remember { mutableStateMapOf<String,Int>() }
                            var deleted by remember{ mutableStateOf(false)}

                            LaunchedEffect(vocabulary){
                                if(vocabulary!= null){
                                    computeNameMap(vocabulary!!.wordList, externalNameMap)
                                }
                            }
                            LaunchedEffect(deleted){
                                if(vocabulary!= null){
                                    if(deleted){
                                        externalNameMap.clear()
                                        computeNameMap(vocabulary!!.wordList, externalNameMap)
                                        deleted = false
                                    }
                                }

                            }

                            Column(Modifier.width(IntrinsicSize.Max)) {
                                if(vocabulary != null){
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) { Text(vocabulary!!.name ?: "İsimsiz Kelime Listesi") } // vocabulary.name null ise varsayılan bir isim göster
                                    val bottom = if(externalNameMap.isEmpty()) 50.dp else 0.dp
                                    Divider(Modifier.padding(bottom = bottom))
                                }
                                if (externalNameMap.isNotEmpty()) {
                                    val boxHeight by remember(externalNameMap.size){
                                        derivedStateOf {
                                            val size = externalNameMap.size
                                            if(size <= 5){
                                                size * 48.dp
                                            }else{
                                                240.dp
                                            }
                                        }
                                    }

                                    Box(Modifier.height(boxHeight).fillMaxWidth()){
                                        val stateVertical = rememberScrollState(0)
                                        Column (Modifier.verticalScroll(stateVertical)){
                                            externalNameMap.forEach { (path, count) ->
                                                var showConfirmationDialog by remember { mutableStateOf(false) }
                                                Row(verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable{}){
                                                    val name = File(path).nameWithoutExtension
                                                    if (showConfirmationDialog) {
                                                        ConfirmDialog(
                                                            message = "$name için tüm altyazılar silinsin mi?", // "确定要删除 $name 的所有字幕吗?"
                                                            confirm = {
                                                                vocabulary?.wordList?.forEach { word ->
                                                                    val tempList = mutableListOf<ExternalCaption>()
                                                                    word.externalCaptions.forEach { externalCaption ->
                                                                        if (externalCaption.relateVideoPath == path || externalCaption.subtitlesName == path) {
                                                                            tempList.add(externalCaption)
                                                                        }
                                                                    }
                                                                    word.externalCaptions.removeAll(tempList)
                                                                }
                                                                // Seçilen kelime listesi sorunluysa, kullanıcıya kelime listesi hatası göster, kelime listesi silindikten sonra hata mesajını kaldır.
                                                                if (
                                                                    subtitlesName == path) {
                                                                    vocabularyWrong = false
                                                                }
                                                                showConfirmationDialog = false
                                                                saveEnable = true
                                                                deleted = true
                                                            },
                                                            close = { showConfirmationDialog = false }
                                                        )
                                                    }

                                                    Text(
                                                        text = name,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(250.dp).padding(end = 10.dp)
                                                    )
                                                    Text("$count adet", modifier = Modifier.width(60.dp)) // "$count" -> "$count adet"
                                                    IconButton(onClick = { showConfirmationDialog = true },modifier = Modifier.padding(end = 10.dp)) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = "Sil", // "" -> "Sil"
                                                            tint = MaterialTheme.colors.onBackground
                                                        )
                                                    }

                                                }
                                            }
                                        }

                                        VerticalScrollbar(
                                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                            adapter = rememberScrollbarAdapter(stateVertical),
                                        )
                                    }

                                }

                            }
                            if (vocabularyWrong) {
                                if (extractCaptionResultInfo.isNotEmpty()) {
                                    Text(
                                        text = extractCaptionResultInfo,
                                        color = Color.Red,
                                        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
                                    )
                                }

                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    enabled = Objects.isNull(vocabulary),
                                    onClick = {
                                        showFilePicker = true
                                        vocabularyWrong = false

                                }) {
                                    Text("1. Kelime Listesi Seç") // "1 选择词库" -> "1. Kelime Listesi Seç"
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(
                                    enabled = !Objects.isNull(vocabulary),
                                    onClick = {
                                        showFilePicker = true
                                        vocabularyWrong = false

                                }) {
                                    Text("2. Altyazı Listesi Seç") // "2 选择字幕词库" -> "2. Altyazı Listesi Seç"
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(onClick = { save() }, enabled = saveEnable) {
                                    Text("Kaydet") // "保存" -> "Kaydet"
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(onClick = {
                                    clear()
                                    close()
                                }) {
                                    Text("İptal") // "取消" -> "İptal"
                                }
                            }
                            val extensions = if(isMacOS()) listOf("public.json") else listOf("json")
                            FilePicker(
                                show = showFilePicker,
                                fileExtensions = extensions,
                                initialDirectory = ""
                            ){pickFile ->
                                if(pickFile != null){
                                    if(pickFile.path.isNotEmpty()){
                                        val file = File(pickFile.path)
                                        handleInputFile(file)
                                    }
                                }
                                showFilePicker = false
                            }
                        }

                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)){
                            Text("İpucu: Kelime listesini uygulamanın kurulum dizinine kaydetmeyin.") // "提示：不要把词库保存到应用程序的安装目录"
                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        elevation = 4.dp,
                                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                        shape = RectangleShape
                                    ) {
                                        Text(text = "Yardım", modifier = Modifier.padding(10.dp)) // "帮助" -> "Yardım"
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
                                var currentPage by remember { mutableStateOf("linkVocabulary") }
                                IconButton(onClick = {
                                    documentWindowVisible = true
                                }){
                                    Icon(
                                        Icons.Filled.Help,
                                        contentDescription = "Localized description",
                                        tint =if(MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                                    )
                                }


                                if(documentWindowVisible){
                                    DocumentWindow(
                                        close = {documentWindowVisible = false},
                                        currentPage = currentPage,
                                        setCurrentPage = {currentPage = it}

                                    )
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().align(Alignment.Center)) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp)
                            ) {
                                Text("Toplam ${prepareLinks.size} kelime, ${linkCounter} altyazı") // "总共${prepareLinks.size}个单词,${linkCounter}条字幕"
                            }
                            Divider()
                            Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                                val scrollState = rememberLazyListState()
                                LazyColumn(Modifier.fillMaxSize(), scrollState) {

                                    items(prepareLinks.toList()) { (word, captions) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start,
                                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                                                .padding(start = 10.dp, end = 10.dp)
                                        ) {

                                            Text(text = word, modifier = Modifier.width(150.dp))
                                            Divider(Modifier.width(1.dp).fillMaxHeight())
                                            Column(verticalArrangement = Arrangement.Center) {
                                                captions.forEachIndexed { index, externalCaption ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "${index + 1}. ${externalCaption.content}",
                                                            modifier = Modifier.padding(5.dp)
                                                        )
                                                        val caption = Caption(externalCaption.start,externalCaption.end,externalCaption.content)
                                                        val playTriple =
                                                            Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
                                                        val playerBounds by remember {
                                                            mutableStateOf(
                                                                Rectangle(
                                                                    0,
                                                                    0,
                                                                    540,
                                                                    303
                                                                )
                                                            )
                                                        }
                                                        val mousePoint by remember{ mutableStateOf(Point(0,0)) }
                                                        var isVideoBoundsChanged by remember{mutableStateOf(false)}
                                                        val resetVideoBounds:() -> Rectangle = {
                                                            isVideoBoundsChanged = false
                                                            Rectangle(mousePoint.x, mousePoint.y, 540, 303)
                                                        }
                                                        var isPlaying by remember { mutableStateOf(false) }
                                                        IconButton(
                                                            onClick = {},
                                                            modifier = Modifier
                                                                .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                                                                    val location =
                                                                        pointerEvent.awtEventOrNull?.locationOnScreen
                                                                    if (location != null && !isPlaying) {
                                                                        if (isVideoBoundsChanged) {
                                                                            mousePoint.x = location.x - 270 + 24
                                                                            mousePoint.y = location.y - 320
                                                                        } else {
                                                                            playerBounds.x = location.x - 270 + 24
                                                                            playerBounds.y = location.y - 320
                                                                        }
                                                                        scope.launch {
                                                                            play(
                                                                                window = appState.videoPlayerWindow,
                                                                                setIsPlaying = {
                                                                                    isPlaying = it
                                                                                },
                                                                                volume = appState.global.videoVolume,
                                                                                playTriple = playTriple,
                                                                                videoPlayerComponent = appState.videoPlayerComponent,
                                                                                bounds = playerBounds,
                                                                                resetVideoBounds = resetVideoBounds,
                                                                                isVideoBoundsChanged = isVideoBoundsChanged,
                                                                                vocabularyDir = File(vocabularyDir),
                                                                                setIsVideoBoundsChanged = {
                                                                                    isVideoBoundsChanged = it
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.PlayArrow,
                                                                contentDescription = "Localized description",
                                                                tint = MaterialTheme.colors.primary
                                                            )
                                                        }
                                                    }
                                                }

                                            }
                                        }
                                        Divider()
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                )
                            }

                            Divider()
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp)
                            ) {
                                OutlinedButton(onClick = {
                                    import()
                                    clear()
                                }) {
                                    Text("Bağla") // "链接" -> "Bağla"
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(onClick = { clear() }) {
                                    Text("İptal") // "取消" -> "İptal"
                                }
                            }
                        }
                    }
                }


            }
        }
    }
}