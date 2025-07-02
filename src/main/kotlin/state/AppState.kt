package state

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.formdev.flatlaf.FlatLightLaf
import data.RecentItem
import data.getHardVocabularyFile
import data.loadMutableVocabulary
import data.loadMutableVocabularyByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.createMediaPlayerComponent
import player.isLinux
import player.isMacOS
import player.isWindows
import theme.createColors
import ui.flatlaf.initializeFileChooser
import ui.wordscreen.MemoryStrategy
import ui.wordscreen.WordScreenState
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.FutureTask
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane

/** Tüm arayüzler tarafından paylaşılan durum */
@ExperimentalSerializationApi
class AppState {

    /** Genel durumda kalıcı olması gereken durum */
    var global: GlobalState = loadGlobalState()

    /** Materyal renkleri */
    var colors by mutableStateOf(createColors(global))

    /** Video oynatma penceresi, JFrame kullanılmasının bir nedeni, swingPanel yeniden oluşturulduğunda bir flaş oluşmasıdır,
     * İlgili Sorun: https://github.com/JetBrains/compose-jb/issues/1800,
     * Jetbrains hatayı düzelttikten sonra yeniden düzenleyin. */
    var videoPlayerWindow = createVideoPlayerWindow()

    /** VLC video oynatma bileşeni */
    var videoPlayerComponent = createMediaPlayerComponent()

    /** Dosya seçici, önceden yüklenmezse tepki çok yavaş olacaktır */
    var futureFileChooser: FutureTask<JFileChooser> = initializeFileChooser(global.isDarkTheme,global.isFollowSystemTheme)

    /** Zor kelime dağarcığı */
    var hardVocabulary = loadMutableVocabularyByName("HardVocabulary")

    /** Son oluşturulan kelime dağarcığı listesi */
    var recentList = readRecentList()

    /** Ayarları aç */
    var openSettings by mutableStateOf(false)

    /** Bekleme penceresi gösterilsin mi */
    var loadingFileChooserVisible by mutableStateOf(false)

    /** [Yeni Kelime Dağarcığı] penceresi gösterilsin mi */
    var newVocabulary by  mutableStateOf(false)
    /** [Kelime Dağarcığını Düzenle] penceresi gösterilsin mi */
    var editVocabulary by  mutableStateOf(false)

    /** [Kelime Dağarcıklarını Birleştir] penceresi gösterilsin mi */
    var mergeVocabulary by mutableStateOf(false)

    /** [Kelime Dağarcığını Filtrele] penceresi gösterilsin mi */
    var filterVocabulary by mutableStateOf(false)

    /** [Kelime Dağarcığını Bildiklerine Aktar] penceresi gösterilsin mi */
    var importFamiliarVocabulary by mutableStateOf(false)

    /** [Belgeden Kelime Dağarcığı Oluştur] penceresi gösterilsin mi */
    var generateVocabularyFromDocument by mutableStateOf(false)

    /** [Altyazı Dosyasından Kelime Dağarcığı Oluştur] penceresi gösterilsin mi */
    var generateVocabularyFromSubtitles by mutableStateOf(false)

    /** [Videodan Kelime Dağarcığı Oluştur] penceresi gösterilsin mi */
    var generateVocabularyFromVideo by mutableStateOf(false)

    /** Yazılım güncelleme iletişim kutusunu göster */
    var showUpdateDialog by mutableStateOf(false)

    /** Yazılımın en son sürümü */
    var latestVersion by mutableStateOf("")

    /** Sürüm notları **/
    var releaseNote by mutableStateOf("")

    /** Yerel olarak önbelleğe alınmış kelime telaffuz listesi */
    var localAudioSet = loadAudioSet()

    var vocabularyChanged by mutableStateOf(false)

    /** Genel ayar bilgilerini yükle */
    private fun loadGlobalState(): GlobalState {
        val globalSettings = getGlobalSettingsFile()
        return if (globalSettings.exists()) {
            try {
                val decodeFormat = Json { ignoreUnknownKeys = true }
                val globalData = decodeFormat.decodeFromString<GlobalData>(globalSettings.readText())
                GlobalState(globalData)
            } catch (exception: Exception) {
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null, "Ayar bilgileri ayrıştırılamadı, varsayılan ayarlar kullanılacak.\nAdres: $globalSettings")
                GlobalState(GlobalData())
            }
        } else {
            GlobalState(GlobalData())
        }
    }


    /** Video oynatma penceresini başlat */
    @OptIn(ExperimentalComposeUiApi::class)
    private fun createVideoPlayerWindow(): JFrame {
        val window = JFrame()
        window.title = "Video Oynatma Penceresi"
        ResourceLoader.Default.load("logo/logo.png").use { inputStream ->
            val image = ImageIO.read(inputStream)
            window.iconImage = image
        }
        window.isUndecorated = true
        window.isAlwaysOnTop = true
        return window
    }

    /** Genel ayar bilgilerini kaydet */
    fun saveGlobalState() {
        runBlocking {
            launch (Dispatchers.IO){
                val globalData = GlobalData(
                    global.type,
                    global.isDarkTheme,
                    global.isFollowSystemTheme,
                    global.audioVolume,
                    global.videoVolume,
                    global.keystrokeVolume,
                    global.isPlayKeystrokeSound,
                    global.primaryColor.value,
                    global.backgroundColor.value,
                    global.onBackgroundColor.value,
                    global.wordTextStyle,
                    global.detailTextStyle,
                    global.letterSpacing.value,
                    global.position.x.value,
                    global.position.y.value,
                    global.size.width.value,
                    global.size.height.value,
                    global.placement,
                    global.autoUpdate,
                    global.ignoreVersion,
                    global.bncNum,
                    global.frqNum,
                    global.maxSentenceLength
                )
                val json = encodeBuilder.encodeToString(globalData)
                val settings = getGlobalSettingsFile()
                settings.writeText(json)
            }
        }
    }

    /** Kelime dağarcığını değiştir */
    fun changeVocabulary(
        vocabularyFile: File,
        wordScreenState: WordScreenState,
        index: Int
    ):Boolean {
        val newVocabulary = loadMutableVocabulary(vocabularyFile.absolutePath)
        if(newVocabulary.wordList.size>0){

            wordScreenState.clearInputtedState()
            if(wordScreenState.memoryStrategy == MemoryStrategy.Dictation || wordScreenState.memoryStrategy == MemoryStrategy.DictationTest){
                wordScreenState.memoryStrategy = MemoryStrategy.Normal
                wordScreenState.showInfo()
            }
            // Zor kelime dağarcığının ve bildik kelimeler dağarcığının dizinini wordScreenState'e kaydet.
            when (wordScreenState.vocabulary.name) {
                "HardVocabulary" -> {
                    wordScreenState.hardVocabularyIndex = wordScreenState.index
                }
                "FamiliarVocabulary" -> {
                    wordScreenState.familiarVocabularyIndex = wordScreenState.index
                }
                else -> {
                    // Mevcut kelime dağarcığının dizinini son kullanılanlar listesine kaydet,
                    if(wordScreenState.vocabularyPath.isNotEmpty()){
                        saveToRecentList(wordScreenState.vocabulary.name, wordScreenState.vocabularyPath,wordScreenState.index)
                    }
                }
            }

            wordScreenState.vocabulary = newVocabulary
            wordScreenState.vocabularyName = vocabularyFile.nameWithoutExtension
            wordScreenState.vocabularyPath = vocabularyFile.absolutePath
            wordScreenState.chapter = (index / 20) + 1
            wordScreenState.index = index
            vocabularyChanged = true
            wordScreenState.saveWordScreenState()
            return true
        }
        return false
    }

    fun findVocabularyIndex(file:File):Int{
        var index = 0
        for (recentItem in recentList) {
            if(file.absolutePath == recentItem.path){
                index = recentItem.index
            }
        }
        return index
    }

    /** Zor kelime dağarcığını kaydet */
    fun saveHardVocabulary(){
        runBlocking {
            launch (Dispatchers.IO){
                val json = encodeBuilder.encodeToString(hardVocabulary.serializeVocabulary)
                val file = getHardVocabularyFile()
                file.writeText(json)
            }
        }
    }

    /** Son oluşturulan kelime dağarcığı listesini oku */
    private fun readRecentList(): SnapshotStateList<RecentItem> {
        val recentListFile = getRecentListFile()
        var list = if (recentListFile.exists()) {
            try {
                Json.decodeFromString<List<RecentItem>>(recentListFile.readText())
            } catch (exception: Exception) {
                listOf()
            }

        } else {
            listOf()
        }
        list = list.sortedByDescending { it.time }
        return list.toMutableStateList()
    }

    private fun getRecentListFile(): File {
        val settingsDir = getSettingsDirectory()
        return File(settingsDir, "recentList.json")
    }

    fun saveToRecentList(name: String, path: String,index: Int) {
        runBlocking {
            launch (Dispatchers.IO){
                if(name.isNotEmpty()){
                    val item = RecentItem(LocalDateTime.now().toString(), name, path,index)
                    if (!recentList.contains(item)) {
                        if (recentList.size == 1000) {
                            recentList.removeAt(999)
                        }
                        recentList.add(0, item)
                    } else {
                        recentList.remove(item)
                        recentList.add(0, item)
                    }
                    val serializeList = mutableListOf<RecentItem>()
                    serializeList.addAll(recentList)

                    val json = encodeBuilder.encodeToString(serializeList)
                    val recentListFile = getRecentListFile()
                    recentListFile.writeText(json)
                }

            }
        }

    }

    fun removeRecentItem(recentItem: RecentItem) {
        runBlocking {
            launch (Dispatchers.IO){
                recentList.remove(recentItem)
                val serializeList = mutableListOf<RecentItem>()
                serializeList.addAll(recentList)
                val json = encodeBuilder.encodeToString(serializeList)
                val recentListFile = getRecentListFile()
                recentListFile.writeText(json)
            }
        }
    }

    private fun loadAudioSet(): MutableSet<String> {
        val audioDir = getAudioDirectory()
        if (!audioDir.exists()) {
            audioDir.mkdir()
        }
        val set = mutableSetOf<String>()
        audioDir.list()?.let { set.addAll(it) }
        return set
    }



    /** 搜索 */
    var searching by  mutableStateOf(false)
    /** 打开搜索 **/
    val openSearch:() -> Unit = {
        searching = true
    }

    val openLoadingDialog:() -> Unit = {
        if(isWindows()) {
            loadingFileChooserVisible = true
        }
    }

}


/** Serileştirme yapılandırması */
private val encodeBuilder = Json {
    prettyPrint = true
    encodeDefaults = true
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberAppState() = remember {
    AppState()
}

/**
 * Kaynakları yükle, kaynakların paketlemeden önceki ve sonraki yolları farklıdır
- İlgili bağlantı: #938 https://github.com/JetBrains/compose-jb/issues/938
- #938 test kodunun adresi
- https://github.com/JetBrains/compose-jb/blob/3070856954d4c653ea13a73aa77adb86a2788c66/gradle-plugins/compose/src/test/test-projects/application/resources/src/main/kotlin/main.kt
- System.getProperty("compose.application.resources.dir") null ise, henüz paketlenmemiş demektir
 */
fun composeAppResource(path: String): File {
    val property = "compose.application.resources.dir"
    val dir = System.getProperty(property)
    return if (dir != null) {
        // Paketlemeden sonraki ortam
        File(dir).resolve(path)
    } else {// Geliştirme ortamı
        // Genel kaynaklar
        var commonPath = File("resources/common/$path")
        // Windows işletim sistemine özel kaynaklar
        if (!commonPath.exists() && isWindows()) {
            commonPath = File("resources/windows/$path")
        }
        // macOS işletim sistemine özel kaynaklar
        if (!commonPath.exists() && isMacOS()) {
            val arch = System.getProperty("os.arch").lowercase()
            commonPath = if (arch == "arm" || arch == "aarch64") {
                File("resources/macos-arm64/$path")
            }else {
                File("resources/macos-x64/$path")
            }
        }
        // Linux işletim sistemine özel kaynaklar
        if (!commonPath.exists() && isLinux()) {
            commonPath = File("resources/linux/$path")
        }
        commonPath
    }
}

fun getAudioDirectory(): File {
    val homeDir = File(System.getProperty("user.home"))
    val audioDir = File(homeDir, ".MuJing/audio")
    if (!audioDir.exists()) {
        audioDir.mkdir()
    }
    return audioDir
}

/** Uygulamanın yapılandırma dosyası dizinini al */
fun getSettingsDirectory(): File {
    val homeDir = File(System.getProperty("user.home"))
    val applicationDir = File(homeDir, ".MuJing")
    if (!applicationDir.exists()) {
        applicationDir.mkdir()
    }
    return applicationDir
}

/** Genel yapılandırma dosyasını al */
private fun getGlobalSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "AppSettings.json")
}


/**
 * Kaynak dosyasını al
 * @param path dosya yolu
 */
fun getResourcesFile(path: String): File {
    val file = if (File(path).isAbsolute) {
        File(path)
    } else {
        composeAppResource(path)
    }
    return file
}