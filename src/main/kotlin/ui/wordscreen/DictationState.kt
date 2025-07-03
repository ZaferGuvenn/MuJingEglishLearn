package ui.wordscreen

import androidx.compose.runtime.*
import com.formdev.flatlaf.FlatLightLaf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import java.io.File
import javax.swing.JOptionPane


/**
 * Dikte kelimeleri için veri sınıfı
 */
@ExperimentalSerializationApi
@Serializable
data class DataDictationState(
    val phoneticVisible: Boolean = false,
    val morphologyVisible: Boolean = false,
    val definitionVisible: Boolean = false,
    val translationVisible: Boolean = false,
    val subtitlesVisible: Boolean = false,
    val sentencesVisible: Boolean = false,
)

/**
 * Kelimeleri dikte ederken durum
 */
@OptIn(ExperimentalSerializationApi::class)
class DictationState(dataDictationState: DataDictationState){
    /**
     * Fonetik bileşenin görünürlüğü
     */
    var phoneticVisible by mutableStateOf(dataDictationState.phoneticVisible)

    /**
     * Morfoloji bileşeninin görünürlüğü
     */
    var morphologyVisible by mutableStateOf(dataDictationState.morphologyVisible)

    /**
     * Tanım bileşeninin görünürlüğü
     */
    var definitionVisible by mutableStateOf(dataDictationState.definitionVisible)

    /**
     * Çeviri bileşeninin görünürlüğü
     */
    var translationVisible by mutableStateOf(dataDictationState.translationVisible)

    /**
     * Altyazı bileşeninin görünürlüğü
     */
    var subtitlesVisible by mutableStateOf(dataDictationState.subtitlesVisible)

    /**
     * Örnek cümle bileşeninin görünürlüğü
     */
    var sentencesVisible by mutableStateOf(dataDictationState.subtitlesVisible)

    /** Dikte ederken yapılandırma bilgilerini kaydet */
    fun saveDictationState() {
        runBlocking {
            launch (Dispatchers.IO){
                val dataDictationState = DataDictationState(
                    phoneticVisible,
                    morphologyVisible,
                    definitionVisible,
                    translationVisible,
                    subtitlesVisible,
                    sentencesVisible,
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(dataDictationState)
                val dictationSettings = getDictationFile()
                dictationSettings.writeText(json)
            }
        }
    }
}



@Composable
fun rememberDictationState(): DictationState = remember{
    loadDictationState()
}

/** Dikte kelimeleri için arayüz ayar bilgilerini yükle */
@OptIn(ExperimentalSerializationApi::class)
private fun loadDictationState(): DictationState {
    val dictationSettings = getDictationFile()
    return if (dictationSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataDictationState = decodeFormat.decodeFromString<DataDictationState>(dictationSettings.readText())
            val dictationState = DictationState(dataDictationState)
            dictationState
        } catch (exception: Exception) {
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "Ayar bilgileri ayrıştırma hatası, varsayılan ayarlar kullanılacak.\nAdres: $dictationSettings")
            DictationState(DataDictationState())
        }

    } else {
        DictationState(DataDictationState())
    }
}

/** Ezberlenecek kelimelerin yapılandırma dosyasını al */
private fun getDictationFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "DictationSettings.json")
}