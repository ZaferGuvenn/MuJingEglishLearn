package tts

import com.jacob.activeX.ActiveXComponent
import com.jacob.com.Dispatch
import com.jacob.com.Variant
import state.getResourcesFile

class MSTTSpeech {

    /** Ses seviyesi 1 ila 100 */
    var volume: Int = 100

    /** Hız -10 ila 10 */
    var rate: Int = 0

    /** Çıkış aygıtı dizin numarası */
    var audio: Int = 0

    /** Ses nesnesi */
    var spVoice:Dispatch? = null

    var ax: ActiveXComponent? = null

    init {
        System.setProperty("jacob.dll.path", getResourcesFile("jacob/jacob-1.20-x64.dll").absolutePath ?: "")
        ax = ActiveXComponent("Sapi.SpVoice")
        spVoice = ax!!.`object`
    }


    /**
     * Dili oynat
     * @param text dile dönüştürülecek metin
     */
    fun speak(text: String) {
        try{
            // Ses seviyesini ayarla
            Dispatch.put(spVoice,"Volume",Variant(this.volume))
            // Hızı ayarla
            Dispatch.put(spVoice,"Rate",Variant(this.rate))
            // Okumaya başla
            Dispatch.call(spVoice,"Speak",Variant(text))
        }catch (exception: Exception) {
            println(exception.message)
            exception.printStackTrace()
        }
    }

}
