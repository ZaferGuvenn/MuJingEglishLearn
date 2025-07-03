package util

import androidx.compose.ui.window.WindowState
import data.Caption
import org.mozilla.universalchardet.UniversalDetector
import player.PlayerCaption
import player.convertTimeToMilliseconds
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import ui.dialog.removeItalicSymbol
import ui.dialog.removeNewLine
import ui.dialog.replaceNewLine
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.swing.JOptionPane

/** Dosyanın medya türünü hesapla,
 * Dosya mevcut değilse varsayılan medya türü olan videoyu döndür
 */
fun computeMediaType(mediaPath:String):String{
    val file = File(mediaPath)
    if(file.exists()){
        val extension = file.extension
        //  mp3、aac、wav、mp4、mkv，
        return if(extension =="mp3"||extension =="aac"||extension =="wav"){
            "audio"
        }else{
            "video"
        }
    }
    return "video"
}


/**
 * Video oynatma penceresinin konumunu ve boyutunu hesapla
 */
fun computeVideoBounds(
    windowState: WindowState,
    openSettings: Boolean,
    density:Float,
): Rectangle {
    var mainX = windowState.position.x.value.toInt()
    var mainY = windowState.position.y.value.toInt()
    mainX = (mainX).div(density).toInt()
    mainY = (mainY).div(density).toInt()

    val mainWidth = windowState.size.width.value.toInt()
    val mainHeight = windowState.size.height.value.toInt()

    val size = if (mainWidth in 801..1079) {
        Dimension(642, 390)
    } else if (mainWidth > 1080) {
        Dimension(1005, 610)
    } else {
        Dimension(540, 304)
    }
    if(density!=1f){
        size.width = size.width.div(density).toInt()
        size.height = size.height.div(density).toInt()
    }
    var x = (mainWidth - size.width).div(2)
    var y = ((mainHeight - size.height).div(2))
    x += mainX
    y += mainY
    if (openSettings) x += 109
    val point = Point(x, y)
    return Rectangle(point, size)
}



/**
 * Altyazıları ayrıştır, görüntülemek için maksimum karakter sayısını ve altyazı listesini döndür.
 * @param subtitlesPath Altyazıların yolu
 * @param setMaxLength Altyazıların maksimum karakter sayısını ayarlamak için geri çağırma fonksiyonu
 * @param setCaptionList Altyazı listesini ayarlamak için geri çağırma fonksiyonu
 * @param resetSubtitlesState Altyazı dosyası silindiğinde veya değiştirildiğinde ayrıştırılamazsa sıfırla
 */
fun parseSubtitles(
    subtitlesPath: String,
    setMaxLength: (Int) -> Unit,
    setCaptionList: (List<Caption>) -> Unit,
    resetSubtitlesState:() -> Unit,
) {
    val formatSRT = FormatSRT()
    val file = File(subtitlesPath)
    if(file.exists()){
        try {
            val encoding = UniversalDetector.detectCharset(file)
            val charset =  if(encoding != null){
                Charset.forName(encoding)
            }else{
                Charset.defaultCharset()
            }
            val inputStream: InputStream = FileInputStream(file)
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream,charset)
            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
            val captionList = mutableListOf<Caption>()
            var maxLength = 0
            for (caption in captions.values) {
                var content = removeLocationInfo(caption.content)
                content = removeItalicSymbol(content)
                content = removeNewLine(content)

                val newCaption = Caption(
                    start = caption.start.getTime("hh:mm:ss,ms"),
                    end = caption.end.getTime("hh:mm:ss,ms"),
                    content = content
                )
                if (caption.content.length > maxLength) {
                    maxLength = caption.content.length
                }
                captionList.add(newCaption)
            }

            setMaxLength(maxLength)
            setCaptionList(captionList)
        } catch (exception: Exception) {
            exception.printStackTrace()
            resetSubtitlesState()
            JOptionPane.showMessageDialog(
                null, "Altyazı dosyası ayrıştırma başarısız:\n${exception.message}"
            )

        }
    } else {
        JOptionPane.showMessageDialog(null, "Altyazı bulunamadı")
        resetSubtitlesState()
    }

}


fun parseSubtitles(subtitlesPath: String):List<PlayerCaption>{
    val formatSRT = FormatSRT()
    val file = File(subtitlesPath)
    val captionList = mutableListOf<PlayerCaption>()
    if(file.exists()){
        try {
            val encoding = UniversalDetector.detectCharset(file)
            val charset =  if(encoding != null){
                Charset.forName(encoding)
            }else{
                Charset.defaultCharset()
            }
            val inputStream: InputStream = FileInputStream(file)
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream,charset)
            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions

            var maxLength = 0
            for (caption in captions.values) {
                var content = removeLocationInfo(caption.content)
                content = removeItalicSymbol(content)
                content = replaceNewLine(content)
                val newCaption = PlayerCaption(
                    start = convertTimeToMilliseconds(caption.start.getTime("hh:mm:ss,ms")),
                    end = convertTimeToMilliseconds(caption.end.getTime("hh:mm:ss,ms")),
                    content = content
                )
                if (caption.content.length > maxLength) {
                    maxLength = caption.content.length
                }
                captionList.add(newCaption)
            }

        }catch (exception: Exception){
            exception.printStackTrace()
            JOptionPane.showMessageDialog(null, "Altyazı dosyası ayrıştırma başarısız:\n${exception.message}")
        }

    }else{
        JOptionPane.showMessageDialog(null, "Altyazı bulunamadı")
    }
    return captionList
}