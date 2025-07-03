package ui.wordscreen

import androidx.compose.runtime.*
import com.formdev.flatlaf.FlatLightLaf
import data.Word
import data.loadMutableVocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getResourcesFile
import state.getSettingsDirectory
import java.awt.Rectangle
import java.io.File
import javax.swing.JOptionPane

/** Kelime ezberleme için veri sınıfı */
@ExperimentalSerializationApi
@Serializable
data class WordScreenData(
    val wordVisible: Boolean = true,
    val phoneticVisible: Boolean = true,
    val morphologyVisible: Boolean = true,
    val definitionVisible: Boolean = true,
    val translationVisible: Boolean = true,
    val subtitlesVisible: Boolean = true,
    val sentencesVisible: Boolean = true,
    val isPlaySoundTips: Boolean = true,
    val soundTipsVolume: Float = 0.6F,
    val pronunciation: String = "us",
    val playTimes:Int = 1,
    val isAuto: Boolean = false,
    val repeatTimes: Int = 1,
    val index: Int = 0,
    val hardVocabularyIndex: Int = 0,
    val familiarVocabularyIndex: Int = 0,
    var vocabularyName: String = "",
    var vocabularyPath: String = "",
    var externalSubtitlesVisible: Boolean = true,
    var isWriteSubtitles: Boolean = true,
    var isChangeVideoBounds: Boolean = false,
    var playerLocationX: Int = 0,
    var playerLocationY: Int = 0,
    var playerWidth: Int = 1005,
    var playerHeight: Int = 502,
)

/** Kelime ezberleme için gözlemlenebilir durum */
@OptIn(ExperimentalSerializationApi::class)
class WordScreenState(wordScreenData: WordScreenData) {

    // Kalıcılaştırılabilir durum BAŞLANGIÇ
    /**
     * Kelime bileşeninin görünürlüğü
     */
    var wordVisible by mutableStateOf(wordScreenData.wordVisible)

    /**
     * Fonetik bileşeninin görünürlüğü
     */
    var phoneticVisible by mutableStateOf(wordScreenData.phoneticVisible)

    /**
     * Morfoloji bileşeninin görünürlüğü
     */
    var morphologyVisible by mutableStateOf(wordScreenData.morphologyVisible)

    /**
     * Tanım bileşeninin görünürlüğü
     */
    var definitionVisible by mutableStateOf(wordScreenData.definitionVisible)

    /**
     * Çeviri bileşeninin görünürlüğü
     */
    var translationVisible by mutableStateOf(wordScreenData.translationVisible)

    /**
     * Altyazı bileşeninin görünürlüğü
     */
    var subtitlesVisible by mutableStateOf(wordScreenData.subtitlesVisible)

    /**
     * Örnek cümle bileşeninin görünürlüğü
     */
    var sentencesVisible by mutableStateOf(wordScreenData.sentencesVisible)

    /**
     * İpucu sesi çalınıyor mu
     */
    var isPlaySoundTips by mutableStateOf(wordScreenData.isPlaySoundTips)

    /**
     * İpucu sesi seviyesi
     */
    var soundTipsVolume by mutableStateOf(wordScreenData.soundTipsVolume)

    /**
     * Telaffuz seçimi, İngiliz İngilizcesi, Amerikan İngilizcesi, Japonca seçenekleri mevcuttur
     */
    var pronunciation by mutableStateOf(wordScreenData.pronunciation)

    /**
     * Kelime telaffuzunun çalma sayısı
     */
    var playTimes by mutableStateOf(wordScreenData.playTimes)

    /**
     * Otomatik geçiş mi
     */
    var isAuto by mutableStateOf(wordScreenData.isAuto)

    /**
     * Kelimenin tekrar sayısı
     */
    var repeatTimes by mutableStateOf(wordScreenData.repeatTimes)

    /**
     * Geçerli kelimenin dizini, 0'dan başlar, başlık çubuğunda görüntülendiğinde +1
     */
    var index by mutableStateOf(wordScreenData.index)

    /**
     * Zor kelime dağarcığının dizini, 0'dan başlar, başlık çubuğunda görüntülendiğinde +1
     */
    var hardVocabularyIndex by mutableStateOf(wordScreenData.hardVocabularyIndex)

    /**
     * Tanıdık kelime dağarcığının dizini, 0'dan başlar, başlık çubuğunda görüntülendiğinde +1
     */
    var familiarVocabularyIndex by mutableStateOf(wordScreenData.hardVocabularyIndex)

    /**
     * Geçerli kelimenin bölümü, 1'den başlar
     */
    var chapter by mutableStateOf((wordScreenData.index / 20) + 1)

    /**
     * Kelime dağarcığının adı
     */
    var vocabularyName by mutableStateOf(wordScreenData.vocabularyName)

    /**
     * Şu anda öğrenilmekte olan kelime dağarcığının yolu
     */
    var vocabularyPath by mutableStateOf(wordScreenData.vocabularyPath)

    /** Harici altyazıların görünürlüğü */
    var externalSubtitlesVisible by mutableStateOf(wordScreenData.externalSubtitlesVisible)

    /** Altyazıları yaz, açıldıktan sonra belirli bir altyazı oynatıldığında imleç altyazıya geçer, böylece altyazılar yazılabilir */
    var isWriteSubtitles by mutableStateOf(wordScreenData.isWriteSubtitles)

    var isChangeVideoBounds by mutableStateOf(wordScreenData.isChangeVideoBounds)

    var playerLocationX by mutableStateOf(wordScreenData.playerLocationX)

    var playerLocationY by mutableStateOf(wordScreenData.playerLocationY)

    var playerWidth by mutableStateOf(wordScreenData.playerWidth)

    var playerHeight by mutableStateOf(wordScreenData.playerHeight)

    // Kalıcılaştırılabilir durum SON

    /** Kelime giriş kutusuna girilen sonuç*/
    val wordTypingResult =  mutableStateListOf<Pair<Char, Boolean>>()

    /** Kelime giriş kutusundaki dize*/
    var wordTextFieldValue by  mutableStateOf("")

    /** Geçerli kelimenin doğru sayısı */
    var wordCorrectTime by mutableStateOf(0)

    /** Geçerli kelimenin yanlış sayısı */
    var wordWrongTime by mutableStateOf(0)

    /** İlk altyazının giriş dizesi*/
    var captionsTextFieldValue1 by  mutableStateOf("")

    /** İkinci altyazının giriş dizesi*/
    var captionsTextFieldValue2 by  mutableStateOf("")

    /** Üçüncü altyazının giriş dizesi*/
    var captionsTextFieldValue3 by mutableStateOf("")

    /** Altyazı giriş kutusunun sonucu */
    val captionsTypingResultMap =
        mutableStateMapOf<Int, MutableList<Pair<Char, Boolean>>>()

    /** Şu anda öğrenilmekte olan kelime dağarcığı */
    var vocabulary = loadMutableVocabulary(vocabularyPath)

    /** Kelime ezberleme arayüzünün ezberleme stratejisi */
    var memoryStrategy by mutableStateOf(MemoryStrategy.Normal)

    /** Dikte edilecek kelimeler */
    val dictationWords = mutableStateListOf<Word>()

    /** Dikte kelimeleri sırasındaki dizin */
    var dictationIndex by mutableStateOf(0)

    /** Ayrı olarak dikte testi yapılacak kelimeler */
    val reviewWords = mutableStateListOf<Word>()

    /** Dikte sırasında yanlış yazılan kelimeler */
    val wrongWords = mutableStateListOf<Word>()

    /** Dikte moduna girmeden önce `typing` değişkeninin bazı durumlarını kaydetmek gerekir, dikte modundan çıktıktan sonra geri yüklenir */
    private val visibleMap = mutableStateMapOf<String, Boolean>()
    // visible
    /** Geçerli kelimeyi al */
    fun getCurrentWord(): Word {

        return when (memoryStrategy){

            MemoryStrategy.Normal -> getWord(index)

            MemoryStrategy.Dictation -> dictationWords[dictationIndex]

            MemoryStrategy.DictationTest -> reviewWords[dictationIndex]

            MemoryStrategy.NormalReviewWrong -> wrongWords[dictationIndex]

            MemoryStrategy.DictationTestReviewWrong -> wrongWords[dictationIndex]
        }

    }

    fun getVocabularyDir():File{
        return File(vocabularyPath).parentFile
    }

    /** Dizine göre kelime döndür */
    private fun getWord(index: Int): Word {
        val size = vocabulary.wordList.size
        return if (index in 0 until size) {
            vocabulary.wordList[index]
        } else {
            // Kullanıcı düzenleyiciyi kullanarak dizini değiştirirse ve kelime listesi aralığında değilse, dizini 0 olarak değiştirin.
            this.index = 0
            saveWordScreenState()
            vocabulary.wordList[0]
        }

    }


    /**
     * Dikte modu için rastgele bir kelime dağarcığı oluştur
    - Sözde kod
    - 1 -> 0,19
    - 2 -> 20,39
    - 3 -> 40,59
    - eğer bölüm == 2
    - başlangıç = 2 * 20 -20, bitiş = 2 * 20  -1
    - eğer bölüm == 3
    - başlangıç = 3 * 20 -20, bitiş = 3 * 20 - 1
     */
    fun generateDictationWords(currentWord: String): List<Word> {
        val start = chapter * 20 - 20
        var end = chapter * 20
        if(end > vocabulary.wordList.size){
            end = vocabulary.wordList.size
        }
        var list = vocabulary.wordList.subList(start, end).shuffled()
        // Karıştırılmış listenin ilk kelimesi geçerli bölümün son kelimesine eşitse, yeniden oluşturma tetiklenmez
        while (list[0].value == currentWord) {
            list = vocabulary.wordList.subList(start, end).shuffled()
        }
        return list
    }

    /** Dikte moduna gir, dikte moduna girerken geçerli durumu kaydet, dikte modundan çıktıktan sonra geri yükle */
    fun hiddenInfo(
        dictationState: DictationState
    ) {
        // Önce durumu kaydet
        visibleMap["isAuto"] = isAuto
        visibleMap["wordVisible"] = wordVisible
        visibleMap["phoneticVisible"] = phoneticVisible
        visibleMap["definitionVisible"] = definitionVisible
        visibleMap["morphologyVisible"] = morphologyVisible
        visibleMap["translationVisible"] = translationVisible
        visibleMap["subtitlesVisible"] = subtitlesVisible
        // Sonra durumu değiştir
        isAuto = true
        wordVisible = false
        phoneticVisible = dictationState.phoneticVisible
        definitionVisible = dictationState.definitionVisible
        morphologyVisible = dictationState.morphologyVisible
        translationVisible = dictationState.translationVisible
        subtitlesVisible = dictationState.subtitlesVisible

    }

    /** Dikte modundan çık, uygulama durumunu geri yükle */
    fun showInfo(clear:Boolean = true) {
        // Durumu geri yükle
        isAuto = visibleMap["isAuto"]!!
        wordVisible = visibleMap["wordVisible"]!!
        phoneticVisible = visibleMap["phoneticVisible"]!!
        definitionVisible = visibleMap["definitionVisible"]!!
        morphologyVisible = visibleMap["morphologyVisible"]!!
        translationVisible = visibleMap["translationVisible"]!!
        subtitlesVisible = visibleMap["subtitlesVisible"]!!

        if(clear){
            dictationWords.clear()
        }

    }

    /** Geçerli kelimenin durumunu temizle */
    val clearInputtedState:() -> Unit = {
        wordTypingResult.clear()
        wordTextFieldValue = ""
        captionsTypingResultMap.clear()
        captionsTextFieldValue1 = ""
        captionsTextFieldValue2 = ""
        captionsTextFieldValue3 = ""
        wordCorrectTime = 0
        wordWrongTime = 0
    }


    /** Geçerli kelime dağarcığını kaydet */
    fun saveCurrentVocabulary() {

        runBlocking {
            launch (Dispatchers.IO){
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(vocabulary.serializeVocabulary)
                val file = getResourcesFile(vocabularyPath)
                file.writeText(json)
            }
        }
    }


    /** Kelime ezberleme ayar bilgilerini kaydet */
    fun saveWordScreenState() {
        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        // Yalnızca normal kelime ezberleme ve yanlış kelimeleri gözden geçirme sırasındaki durum değişikliklerinin kalıcılaştırılması gerekir
        if (memoryStrategy != MemoryStrategy.Dictation && memoryStrategy != MemoryStrategy.DictationTest) {
            runBlocking {
                launch {
                    val wordScreenData = WordScreenData(
                        wordVisible,
                        phoneticVisible,
                        morphologyVisible,
                        definitionVisible,
                        translationVisible,
                        subtitlesVisible,
                        sentencesVisible,
                        isPlaySoundTips,
                        soundTipsVolume,
                        pronunciation,
                        playTimes,
                        isAuto,
                        repeatTimes,
                        index,
                        hardVocabularyIndex,
                        familiarVocabularyIndex,
                        vocabularyName,
                        vocabularyPath,
                        externalSubtitlesVisible,
                        isWriteSubtitles,
                        isChangeVideoBounds,
                        playerLocationX,
                        playerLocationY,
                        playerWidth,
                        playerHeight
                    )

                    val json = encodeBuilder.encodeToString(wordScreenData)
                    val settings = getWordSettingsFile()
                    settings.writeText(json)
                }
            }
        }

    }

    fun changePlayerBounds(rectangle: Rectangle){
        playerLocationX = rectangle.x
        playerLocationY = rectangle.y
        playerWidth = rectangle.width
        playerHeight = rectangle.height
        saveWordScreenState()
    }
}

@Composable
fun rememberWordState(): WordScreenState = remember{
    loadWordState()
}
@Composable
fun rememberPronunciation():String = remember{
    val wordState = loadWordState()
    wordState.pronunciation
}

/** Uygulama kelime ezberleme arayüzünün ayar bilgilerini yükle */
@OptIn(ExperimentalSerializationApi::class)
private fun loadWordState(): WordScreenState {
    val wordScreenSettings = getWordSettingsFile()
    return if (wordScreenSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val wordScreenData = decodeFormat.decodeFromString<WordScreenData>(wordScreenSettings.readText())
            val wordScreenState = WordScreenState(wordScreenData)
            // Esas olarak yeniden başlattıktan sonra "kelime dağarcığı bulunamadı" iletişim kutusunun görünmesini önlemek içindir
            if(wordScreenState.vocabulary.name.isEmpty() &&
                wordScreenState.vocabulary.relateVideoPath.isEmpty() &&
                wordScreenState.vocabulary.wordList.isEmpty()){
                wordScreenState.vocabularyName = ""
                wordScreenState.vocabularyPath = ""
                wordScreenState.saveWordScreenState()
            }
            wordScreenState
        } catch (exception: Exception) {
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "Ayar bilgileri ayrıştırma hatası, varsayılan ayarlar kullanılacak.\nAdres: $wordScreenSettings")
            WordScreenState(WordScreenData())
        }

    } else {
        WordScreenState(WordScreenData())
    }
}

/** Kelime ezberleme yapılandırma dosyasını al */
private fun getWordSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingWordSettings.json")
}
