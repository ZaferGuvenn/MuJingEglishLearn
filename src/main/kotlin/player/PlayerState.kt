package player

import androidx.compose.runtime.*
import data.MutableVocabulary
import data.loadMutableVocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import java.io.File
import javax.swing.JOptionPane

@OptIn(ExperimentalSerializationApi::class)
class PlayerState(playerData: PlayerData) {

    /** Video oynatıcıyı göster */
    var showPlayerWindow by  mutableStateOf(false)
    /** Oynatıcı > Video Adresi */
    var videoPath by mutableStateOf("")
    /** Videoyla ilişkili kelime dağarcığı, akan yazı oluşturmak için kullanılır */
    var vocabulary by  mutableStateOf<MutableVocabulary?>(null)
    /** Videoyla ilişkili kelime dağarcığı adresi, kelime dağarcığını kaydetmek için kullanılır, çünkü video izlerken kelime ayrıntılarını görüntüleyebilir ve çok basitse silebilir veya bildiklerinize ekleyebilirsiniz */
    var vocabularyPath by mutableStateOf("")

    var showSequence by mutableStateOf(playerData.showSequence)
    var danmakuVisible by mutableStateOf(playerData.danmakuVisible)
    var autoCopy by mutableStateOf(playerData.autoCopy)
    var autoSpeak by mutableStateOf(playerData.autoSpeak)
    var preferredChinese by mutableStateOf(playerData.preferredChinese)


    /** Video adresini ayarlama işlevi, buraya yerleştirilmiştir çünkü kelime ezberleme penceresi sürüklenip bırakılan videoları kabul edebilir ve ardından video oynatıcıyı açabilir */
    val videoPathChanged:(String) -> Unit = {
        // Bir video zaten açıkken yeni bir video açılırsa, eski videoyla ilişkili kelime dağarcığını sıfırlayın.
        if(videoPath.isNotEmpty() && vocabulary != null){
            vocabularyPath = ""
            vocabulary = null
        }
        videoPath = it
    }
    /** Kelime dağarcığı adresini ayarlama işlevi, buraya yerleştirilmiştir çünkü kelime ezberleme sürüklenip bırakılan videoları kabul edebilir ve ardından mevcut kelime dağarcığını açık video oynatıcıyla ilişkilendirebilir.*/
    val vocabularyPathChanged:(String) -> Unit = {
        if(videoPath.isNotEmpty()){
            vocabularyPath = it
            val newVocabulary = loadMutableVocabulary(it)
            vocabulary = newVocabulary
        }else{
            JOptionPane.showMessageDialog(null,"Önce videoyu açın, ardından kelime dağarcığını sürükleyip bırakın.")
        }
    }



    fun savePlayerState() {
        runBlocking {
            launch (Dispatchers.IO){
                val playerData = PlayerData(
                    showSequence, danmakuVisible, autoCopy, autoSpeak, preferredChinese
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(playerData)
                val playerSettings = getPlayerSettingsFile()
                playerSettings.writeText(json)
            }
        }
    }

    fun closePlayerWindow(){
        showPlayerWindow = false
        videoPath = ""
        vocabularyPath = ""
        vocabulary = null
    }
}

private fun getPlayerSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "PlayerSettings.json")
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberPlayerState() = remember {
    val playerSettings = getPlayerSettingsFile()
    if (playerSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys }
            val playerData = decodeFormat.decodeFromString<PlayerData>(playerSettings.readText())
            PlayerState(playerData)
        } catch (exception: Exception) {
            println("Video oynatıcı ayarları ayrıştırılamadı, varsayılan değerler kullanılacak")
            val playerState = PlayerState(PlayerData())
            playerState
        }
    } else {
        val playerState = PlayerState(PlayerData())
        playerState
    }
}