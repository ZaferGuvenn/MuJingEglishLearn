package ffmpeg

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegBuilder.Verbosity
import net.bramp.ffmpeg.job.FFmpegJob
import player.PlayerCaption
import player.isWindows
import state.getResourcesFile
import state.getSettingsDirectory
import util.parseSubtitles
import java.io.File
import javax.swing.JOptionPane

fun findFFmpegPath(): String {
    val path: String = if(isWindows()){
        getResourcesFile("ffmpeg/ffmpeg.exe").absolutePath
    }else{
        val ffmpegFile = getResourcesFile("ffmpeg/ffmpeg")
        if(ffmpegFile.exists() && !ffmpegFile.canExecute()){
            ffmpegFile.setExecutable(true)
        }
        getResourcesFile("ffmpeg/ffmpeg").absolutePath
    }
    return path
}

/**
 * FFmpeg kullanarak videodan altyazıları çıkarın ve altyazıları SRT formatına dönüştürün
 */
fun extractSubtitles(
    input: String, subtitleId: Int,
    output: String,
    verbosity: Verbosity = Verbosity.INFO
): String {
    return try {
        val ffmpeg = FFmpeg(findFFmpegPath())
        val builder = FFmpegBuilder()
            .setVerbosity(verbosity)
            .setInput(input)
            .addOutput(output)
            .addExtraArgs("-map", "0:s:$subtitleId") //  -map 0:s:0 ilk altyazıyı, -map 0:s:1 ikinci altyazıyı çıkarır.
            .done()
        val executor = FFmpegExecutor(ffmpeg)
        val job = executor.createJob(builder)
        job.run()
        if (job.state == FFmpegJob.State.FINISHED) {
            "finished"
        } else {
            JOptionPane.showMessageDialog(null, "Altyazı çıkarılamadı", "Hata", JOptionPane.ERROR_MESSAGE)
            "failed"
        }
    } catch (e: Exception) {
        JOptionPane.showMessageDialog(null, "Seçilen altyazı formatı geçici olarak desteklenmiyor\n ${e.message}", "Hata", JOptionPane.ERROR_MESSAGE)
        "failed"
    }
}

/**
 * FFmpeg kullanarak altyazı formatını SRT formatına dönüştürün
 */
fun convertToSrt(
    input:String,
    output:String,
    verbosity: Verbosity = Verbosity.INFO
    ):String{
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
        .setVerbosity(verbosity)
        .setInput(input)
        .addOutput(output)
        .done()
    val executor = FFmpegExecutor(ffmpeg)
    val job = executor.createJob(builder)
    job.run()
    if (job.state == FFmpegJob.State.FINISHED) {
        return "finished"
    }
    return "failed"
}

/**
 * FFmpeg kullanarak videodan altyazıları çıkarın ve bir Caption Listesi döndürün
 */
fun readCaptionList(
    videoPath: String,
    subtitleId: Int,
    verbosity: Verbosity = Verbosity.INFO
    ): List<PlayerCaption> {
    val captionList = mutableListOf<PlayerCaption>()
    val applicationDir = getSettingsDirectory()
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
        .setVerbosity(verbosity)
        .setInput(videoPath)
        .addOutput("$applicationDir/temp.srt")
        .addExtraArgs("-map", "0:s:$subtitleId") //  -map 0:s:0 ilk altyazıyı, -map 0:s:1 ikinci altyazıyı çıkarır.
        .done()
    val executor = FFmpegExecutor(ffmpeg)
    val job = executor.createJob(builder)
    job.run()
    if (job.state == FFmpegJob.State.FINISHED) {
        println("extractSubtitle success")
        captionList.addAll(parseSubtitles("$applicationDir/temp.srt"))
        File("$applicationDir/temp.srt").delete()
    }
    return captionList
}


/**
 * Zengin metin etiketleriyle eşleşen düzenli ifade
 */
const val RICH_TEXT_REGEX = "<(b|i|u|font|s|ruby|rt|rb|sub|sup).*?>|</(b|i|u|font|s|ruby|rt|rb|sub|sup)>"


/**
 * SRT altyazılarındaki zengin metin etiketlerini kaldırın
 *FFmpeg kullanarak mov_text altyazılarını çıkarırken, bu zengin metin etiketleri korunur, ancak yalnızca düz metne ihtiyacımız olduğu için bu etiketleri kaldırmamız gerekir.
 *
 * mov_text altyazıları aşağıdaki zengin metin biçimi etiketlerini destekler:
 * <font>: Yazı tipi stili (face, size ve color özelliklerini içerir)
 * <b>: Kalın metin
 * <i>: İtalik metin
 * <u>: Altı çizili metin
 * <s>: Üstü çizili metin
 * <ruby>: Fonetik veya açıklama için kullanılan metin
 * <rt>: Fonetik metin
 * <rb>: Temel metin (<ruby> ile birlikte kullanılır)
 * <sub>: Alt simge metni
 * <sup>: Üst simge metni
 */
fun removeRichText(srtFile: File){
    var content = srtFile.readText()
    content = removeRichText(content)

    srtFile.writeText(content)
}

fun removeRichText(content: String): String {
    val richTextRegex = Regex(RICH_TEXT_REGEX)
    return richTextRegex.replace(content, "")
}


fun hasRichText(srtFile: File): Boolean {
    val content = srtFile.readText()
    val richTextRegex = Regex(RICH_TEXT_REGEX)
        return richTextRegex.containsMatchIn(content)
}


/**
 * Seçilen altyazıları kullanıcı dizinine çıkarın, altyazı tarayıcı arayüzü tarafından kullanılır
 * */
fun writeSubtitleToFile(
    videoPath: String,
    trackId: Int,
): File? {
    val settingsDir = getSettingsDirectory()
    val subtitleFile = File(settingsDir, "subtitles.srt")
    val result = extractSubtitles(videoPath, trackId, subtitleFile.absolutePath)
    if(result == "finished"){
        // Altyazı dosyasının zengin metin etiketleri içerip içermediğini kontrol edin
        val hasRichText = hasRichText(subtitleFile)
        if(hasRichText){
            removeRichText(subtitleFile)
        }

        return subtitleFile
    }else{
        return null
    }
}