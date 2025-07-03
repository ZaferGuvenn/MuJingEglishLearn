package util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.matthewn4444.ebml.subtitles.SSASubtitles
import data.Caption
import data.Dictionary
import data.Word
import ffmpeg.convertToSrt
import ffmpeg.extractSubtitles
import ffmpeg.hasRichText
import ffmpeg.removeRichText
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.langdetect.LanguageDetector
import opennlp.tools.langdetect.LanguageDetectorME
import opennlp.tools.langdetect.LanguageDetectorModel
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.mozilla.universalchardet.UniversalDetector
import org.slf4j.LoggerFactory
import state.getSettingsDirectory
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import javax.swing.JOptionPane




/**
 * Belgeyi ayrıştır
 * @param pathName Dosya yolu
 * @param sentenceLength Kelimenin bulunduğu cümlenin maksimum kelime sayısı
 * @param setProgressText İlerleme metnini ayarla
 */
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun parseDocument(
    pathName: String,
    enablePhrases: Boolean,
    sentenceLength:Int = 25,
    setProgressText: (String) -> Unit
): List<Word> {
    val file = File(pathName)
    var text = ""
    val extension = file.extension
    val otherExtensions = listOf("txt", "java","md","cs", "cpp", "c", "kt", "js", "py", "ts")

    try{
        if (extension == "pdf") {
            setProgressText("Belge yükleniyor")
            val document: PDDocument = PDDocument.load(file)
            //Instantiate PDFTextStripper class
            val pdfStripper = PDFTextStripper()
            text = pdfStripper.getText(document)
            document.close()
        } else if (otherExtensions.contains(extension)) {
            text = file.readText()
            // Windows metin dosyalarının BOM'unu kaldır
            if (extension =="txt" && text.isNotEmpty() && text[0].code == 65279) {
                text = text.substring(1)
            }
        }
    }catch (exception: InvalidPasswordException){
        JOptionPane.showMessageDialog(null,exception.message)
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }

    // Kelime -> cümle eşlemesi, kelimelerin belgedeki konumunu kaydetmek için kullanılır
    val map = mutableMapOf<String, MutableList<String>>()

    // Kelime ayırma modelini yükle
    val tokenModel = ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
        TokenizerModel(inputStream)
    }
    val tokenizer = TokenizerME(tokenModel)
    // Konuşma bölümü etiketleme modelini yükle
    val posModel = ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin").use { inputStream ->
        POSModel(inputStream)
    }
    val posTagger = POSTaggerME(posModel)
    // Öbekleme modelini yükle
    val chunkerModel = ResourceLoader.Default.load("opennlp/en-chunker.bin").use { inputStream ->
        ChunkerModel(inputStream)
    }
    val chunker = ChunkerME(chunkerModel)

    setProgressText("Cümleler bölünüyor")
    val sentences = sentenceDetect(text)
    setProgressText("Kelimeler ayrılıyor")
    sentences.forEach { sentence ->
        val wordList = if(enablePhrases){
            tokenizeAndChunkText(sentence, tokenizer, posTagger, chunker)
        }else{
            tokenizeText(sentence, tokenizer)
        }
        wordList.forEach { word ->
            val clippedSentence = clipSentence(word, tokenizer, sentence, sentenceLength)
            val formatSentence = clippedSentence.replace("\r\n", " ").replace("\n", " ")
            val lowercase = word.lowercase(Locale.getDefault())
            // Kod parçacıklarındaki anahtar kelimeler arasında . veya _ sembolleriyle ayır
            val delimiters = listOf(".", "_")
            delimiters.forEach { delimiter ->
                if (lowercase.contains(delimiter)) {
                    val split = lowercase.split(delimiter).toTypedArray()
                    for (str in split) {
                        if (!map.contains(str)) {
                            val list = mutableListOf(formatSentence)
                            map[str] = list
                        } else {
                            // Kelimenin konum listesi 3'ten küçükse ekle
                            if (map[str]!!.size < 3) {
                                map[str]?.add(formatSentence)
                            }
                        }
                    }
                }
            }

            if (!map.contains(lowercase)) {
                val list = mutableListOf(formatSentence)
                map[lowercase] = list
            } else {
                // Kelimenin konum listesi 3'ten küçükse ekle
                if (map[lowercase]!!.size < 3) {
                    map[lowercase]?.add(formatSentence)
                }
            }

        }

    }

    setProgressText("Belgeden ${map.size} kelime çıkarıldı, kelimeler toplu olarak sorgulanıyor, sözlükte olmayanlar atılacak")
    val validList = Dictionary.queryList(map.keys.toList())

    val filterList = listOf(
        ".", "!", "?", ";", ":",  ")",  "}",  "]", "-", "—",
        "'", "`", "~", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|",
        "/", ">", ",", "，", "。", "、", "；", "：", "？", "！",
        "）","【", "】", "｛", "…",  "》", "”", "’"
    )
    validList.forEach { word ->
        if (map[word.value] != null) {
            var pos = ""
            map[word.value]!!.forEach { sentence ->
                // Cümlenin başındaki noktalama işaretlerini atla
                pos = if(filterList.contains(sentence[0].toString())){
                    sentence.substring(1) + "\n"
                }else{
                    sentence + "\n"
                }
            }
            word.pos =pos.trim()
        }
    }
    setProgressText("${validList.size} geçerli kelime")
    setProgressText("")
    return validList
}

/**
 * Cümleyi kırp
 */
fun clipSentence(
    word: String,
    tokenizer: Tokenizer,
    sentences: String,
    sentenceLength: Int
): String {
    val tokenList = tokenizer.tokenize(sentences).toList()
    if(tokenList.size > sentenceLength){
        val index = tokenList.indexOf(word)
        if(index != -1){
            val start = if(index - sentenceLength/2 < 0) 0 else index - sentenceLength/2
            val end = if(index + sentenceLength/2 > tokenList.size) tokenList.size else index + sentenceLength/2
            var clipSentence = ""
            for(i in start until end){
                clipSentence += "${tokenList[i]} "
            }
            return clipSentence
        }else{
            // Kelime bir öbektir
            val formatSentence = sentences.replace("\r\n", " ").replace("\n", " ")
           val strIndex = formatSentence.indexOf(word)
            if(strIndex == -1){
                return formatSentence
            }
            // strIndex'i merkez alarak, başlangıcı belirlemek için geriye doğru sentenceLength/2 boşluk bul, başlangıç konumu 0'dan küçükse 0'dan başla
            // Bitişi belirlemek için ileriye doğru sentenceLength/2 boşluk bul, bitiş konumu sentences uzunluğundan büyükse sentences uzunluğunu bitiş olarak al
            var start = strIndex
            var end = strIndex
            var spaceCount = 0
            while (spaceCount < sentenceLength/2){
                if(start == 0){
                    break
                }
                start--
                if(formatSentence[start] == ' '){
                    spaceCount++
                }

            }
            spaceCount = 0
            while (spaceCount < sentenceLength/2){
                if(end == formatSentence.length){
                    break
                }
                if(formatSentence[end] == ' '){
                    spaceCount++
                }
                end++
            }
            return formatSentence.substring(start,end)

        }

    }else{
        return sentences
    }

}

/**
 * Cümleleri algılamak için OpenNLP'nin SentenceDetectorME modelini kullanın
 */
@OptIn(ExperimentalComposeUiApi::class)
fun sentenceDetect(text: String): List<String> {
    val sentences = mutableListOf<String>()
    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin").use { modelIn ->
        val model = SentenceModel(modelIn)
        val sentenceDetector = SentenceDetectorME(model)
        sentenceDetector.sentDetect(text).forEach { sentence ->
            sentences.add(sentence)
        }
    }
    return sentences
}

/**
 * Kelimeleri ve öbekleri bölmek için konuşma bölümü etiketleme ve öbekleme kullanın
 */
fun tokenizeAndChunkText(
    text: String,
    tokenizer: Tokenizer,
    posTagger: POSTaggerME,
    chunker: ChunkerME
): MutableSet<String> {
    val logger = LoggerFactory.getLogger("tokenizeAndChunkText")
    // Kelime ayırma yap
    val tokens = tokenizer.tokenize(text)
    // Konuşma bölümü etiketleme yap
    val posTags = posTagger.tag(tokens)
    // Öbekleme yap
    val chunks = chunker.chunkAsSpans(tokens, posTags)

    // Sık kullanılan noktalama işaretlerini filtrele
    // .!?;:(){}[]\-—'"`~@#$%^&*+=|\/<>,，。、；：？！（）【】｛｝—…《》“”‘’,
    val filterList = listOf(
        ".", "!", "?", ";", ":", "(", ")", "{", "}", "[", "]", "-", "—",
        "'", "`", "~", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|",
        "/","<", ">", ",", "，", "。", "、", "；", "：", "？", "！", "（",
        "）","【", "】", "｛", "｝", "…", "《", "》", "“", "”", "‘", "’"
    )

    val wordList =tokens.toMutableList()
    for (chunk in chunks) {
        var word = ""
        for(i in chunk.start until chunk.end){
           word += "${tokens[i]} "
        }
        // Kelimenin başındaki ve sonundaki boşlukları atla
        word = word.trim()
        // Kelimenin ilk karakteri noktalama işaretiyse, noktalamayı atla
        if (word.length>1 && filterList.contains(word[0].toString())) {
            logger.info("$word başlangıç noktalama işaretini atla")
            word = word.substring(1)
        }
        // Kelimenin son karakteri noktalama işaretiyse, noktalamayı atla
        if (word.length>1 && filterList.contains(word[word.length - 1].toString())) {
            logger.info("$word bitiş noktalama işaretini atla")
            word = word.substring(0, word.length - 1)
        }
        // Kelimenin başındaki ve sonundaki boşlukları bir kez daha atla
        word = word.trim()
        wordList.add(word)
    }
    val result = mutableSetOf<String>()
    // Sık kullanılan noktalama işaretlerini filtrele
    wordList.forEach { word ->
        if (!filterList.contains(word)) {
            result.add(word)
        }
    }
    return result
}

/**
 * Kelimeleri böl
 */
fun tokenizeText(
    text: String,
    tokenizer: Tokenizer,
): MutableSet<String> {
    // Kelime ayırma yap
    val tokens = tokenizer.tokenize(text)
    // Sık kullanılan noktalama işaretlerini filtrele
    // .!?;:(){}[]\-—'"`~@#$%^&*+=|\/<>,，。、；：？！（）【】｛｝—…《》“”‘’,
    val filterList = listOf(
        ".", "!", "?", ";", ":", "(", ")", "{", "}", "[", "]", "-", "—",
        "'", "`", "~", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|",
        "/","<", ">", ",", "，", "。", "、", "；", "：", "？", "！", "（",
        "）","【", "】", "｛", "｝", "…", "《", "》", "“", "”", "‘", "’"
    )

    val wordList =tokens.toMutableList()
    val result = mutableSetOf<String>()
    // Sık kullanılan noktalama işaretlerini filtrele
    wordList.forEach { word ->
        if (!filterList.contains(word)) {
            result.add(word)
        }
    }
    return result
}

/**
 * SRT altyazı dosyasını ayrıştır
 */
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun parseSRT(
    pathName: String,
    enablePhrases: Boolean,
    setProgressText: (String) -> Unit
): List<Word> {
    val srtFile = File(pathName)
    val hasRichText = hasRichText(srtFile)
    if(hasRichText){
        setProgressText("Altyazıda zengin metin etiketleri var, önce zengin metin etiketlerini kaldırın")
        removeRichText(srtFile)
    }

    val map: MutableMap<String, MutableList<Caption>> = HashMap()
    // Sırayı kaydet
    val orderList = mutableListOf<String>()
    try {
        // Kelime ayırma modelini yükle
        val tokenModel = ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            TokenizerModel(inputStream)
        }
        val tokenizer = TokenizerME(tokenModel)
        // Konuşma bölümü etiketleme modelini yükle
        val posModel = ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin").use { inputStream ->
            POSModel(inputStream)
        }
        val posTagger = POSTaggerME(posModel)
        // Öbekleme modelini yükle
        val chunkerModel = ResourceLoader.Default.load("opennlp/en-chunker.bin").use { inputStream ->
            ChunkerModel(inputStream)
        }
        val chunker = ChunkerME(chunkerModel)

        val formatSRT = FormatSRT()
        val file = File(pathName)
        val encoding = UniversalDetector.detectCharset(file)
        val charset = if (encoding != null) {
            Charset.forName(encoding)
        } else {
            Charset.defaultCharset()
        }
        val inputStream: InputStream = FileInputStream(file)

        setProgressText("Altyazı dosyası ayrıştırılıyor")
        val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream, charset)

        val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
        val captionList: Collection<subtitleFile.Caption> = captions.values
        setProgressText("Kelimeler ayrılıyor")
        for (caption in captionList) {
            var content = replaceSpecialCharacter(caption.content)
            content = removeLocationInfo(content)
            val dataCaption = Caption(
                // getTime(format) tarafından döndürülen süre oynatılamaz
                start = caption.start.getTime("hh:mm:ss,ms"),
                end = caption.end.getTime("hh:mm:ss,ms"),
                content = content
            )
            val tokenize = if(enablePhrases){
                tokenizeAndChunkText(content, tokenizer, posTagger, chunker)
            }else{
                tokenizeText(content, tokenizer)
            }
            for (word in tokenize) {
                val lowercase = word.lowercase(Locale.getDefault())
                if (!map.containsKey(lowercase)) {
                    val list = mutableListOf(dataCaption)
                    map[lowercase] = list
                    orderList.add(lowercase)
                } else {
                    if (map[lowercase]!!.size < 3 && !map[lowercase]!!.contains(dataCaption)) {
                        map[lowercase]?.add(dataCaption)
                    }
                }
            }
        }
        setProgressText("Altyazı dosyasından ${orderList.size} kelime çıkarıldı, kelimeler toplu olarak sorgulanıyor, sözlükte olmayanlar atılacak")
        val validList = Dictionary.queryList(orderList)
        setProgressText("${validList.size} geçerli kelime")
        validList.forEach { word ->
            if (map[word.value] != null) {
                word.captions = map[word.value]!!
            }
        }
        setProgressText("")
        return validList
    } catch (exception: IOException) {
        JOptionPane.showMessageDialog(null, exception.message)
    }
    return listOf()
}

/**
 * ASS altyazı dosyasını ayrıştır
 */
@Throws(IOException::class)
fun parseASS(
    pathName: String,
    enablePhrases: Boolean,
    setProgressText: (String) -> Unit
): List<Word> {
    val applicationDir = getSettingsDirectory()
    val assFile = File(pathName)
    val srtFile = File("$applicationDir/temp.srt")
    setProgressText("Altyazı dönüştürülmeye başlanıyor")
    val result = convertToSrt(assFile.absolutePath, srtFile.absolutePath)
    if(result == "finished"){
        setProgressText("Altyazı dönüştürme tamamlandı")
        val list =  parseSRT(srtFile.absolutePath,enablePhrases,setProgressText)
        srtFile.delete()
        return list
    }else{
        setProgressText("Altyazı dönüştürme başarısız oldu")
        srtFile.delete()
        return emptyList()
    }
}

/**
 * Video dosyasını FFmpeg kullanarak ayrıştır
 */
fun parseVideo(
    pathName: String,
    trackId: Int,
    enablePhrases: Boolean,
    setProgressText: (String) -> Unit,
): List<Word> {
    val applicationDir = getSettingsDirectory()
    setProgressText("Altyazılar çıkarılıyor")
    val result =  extractSubtitles(pathName, trackId, "$applicationDir/temp.srt")
    setProgressText("Altyazı çıkarma tamamlandı")
    if(result == "finished"){
        val list = parseSRT("$applicationDir/temp.srt",enablePhrases,setProgressText)
        File("$applicationDir/temp.srt").delete()
        return list
    }
    return emptyList()
}

@OptIn(ExperimentalComposeUiApi::class)
fun parseMKV(
    pathName: String,
    trackId: Int,
    setProgressText: (String) -> Unit,
): List<Word> {
    val map: MutableMap<String, ArrayList<Caption>> = HashMap()
    val orderList = mutableListOf<String>()
    var reader: EBMLReader? = null
    try {
        reader = EBMLReader(pathName)

        setProgressText("MKV dosyası ayrıştırılıyor")

        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return listOf()
        }

        /**
         * Read the tracks. This contains the details of video, audio and subtitles
         * in this file
         */
        reader.readTracks()

        /**
         * Check if there are any subtitles in this file
         */
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            return listOf()
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()


        /**
         *   OPTIONAL: You can read the header of the subtitle if it is ASS/SSA format
         *       for (int i = 0; i < reader.getSubtitles().size(); i++) {
         *         if (reader.getSubtitles().get(i) instanceof SSASubtitles) {
         *           SSASubtitles subs = (SSASubtitles) reader.getSubtitles().get(i);
         *           System.out.println(subs.getHeader());
         *         }
         *       }
         *
         *
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }
        setProgressText("Kelimeler ayrılıyor")
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            val model = TokenizerModel(inputStream)
            val tokenizer: Tokenizer = TokenizerME(model)
            val subtitle = reader.subtitles[trackId]
            var isASS = false
            if (subtitle is SSASubtitles) {
                isASS = true
            }

            val captionList = subtitle.readUnreadSubtitles()
            for (caption in captionList) {
                val captionContent =  if(isASS){
                    caption.formattedVTT.replace("\\N","\n")
                }else{
                    caption.stringData
                }

                var content = replaceSpecialCharacter(captionContent)
                content = removeLocationInfo(content)
                val dataCaption = Caption(
                    start = caption.startTime.format().toString(),
                    end = caption.endTime.format(),
                    content = content
                )

                content = content.lowercase(Locale.getDefault())
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {
                    if (!map.containsKey(word)) {
                        val list = ArrayList<Caption>()
                        list.add(dataCaption)
                        map[word] = list
                        orderList.add(word)
                    } else {
                        if (map[word]!!.size < 3 && !map[word]!!.contains(dataCaption)) {
                            map[word]!!.add(dataCaption)
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        JOptionPane.showMessageDialog(null,e.message)
        e.printStackTrace()
    } finally {
        try {
            // Remember to close this!
            reader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    setProgressText("Videodan ${orderList.size} kelime çıkarıldı, kelimeler toplu olarak sorgulanıyor, sözlükte olmayanlar atılacak")
    val validList = Dictionary.queryList(orderList)
    setProgressText("${validList.size} geçerli kelime")
    validList.forEach { word ->
        if (map[word.value] != null) {
            word.captions = map[word.value]!!
        }
    }
    setProgressText("")
    return validList
}


/**
 * MKV'leri toplu olarak oku
 */
@OptIn(ExperimentalComposeUiApi::class)
fun batchReadMKV(
    language:String,
    enablePhrases: Boolean,
    selectedFileList:(List<File>),
    setCurrentTask:(File?) -> Unit,
    setErrorMessages:(Map<File,String>) -> Unit,
    updateTaskState:(Pair<File,Boolean>) -> Unit
):List<Word>{
    val errorMessage = mutableMapOf<File, String>()
    val orderList = mutableListOf<Word>()
    val logger = LoggerFactory.getLogger("batchReadMKV")
    // Dil algılama modelini yükle
    val langModel = ResourceLoader.Default.load("opennlp/langdetect-183.bin").use { inputStream ->
        LanguageDetectorModel(inputStream)
    }
    val languageDetector: LanguageDetector = LanguageDetectorME(langModel)

    val englishIetfList = listOf("en", "en-US", "en-GB")
    val english = listOf("en", "eng")
    for (file in selectedFileList) {
        setCurrentTask(file)
        var reader: EBMLReader? = null
        try {
            reader = EBMLReader(file.absolutePath)
            if (!reader.readHeader()) {
                logger.error("Bu video MKV standart bir dosya değil")
                errorMessage[file] = "MKV dosyası değil"
                updateTaskState(Pair(file, false))
                setCurrentTask(null)
                continue
            }

            reader.readTracks()
            val numSubtitles: Int = reader.subtitles.size
            if (numSubtitles == 0) {
                errorMessage[file] = "Altyazı yok"
                logger.error("${file.nameWithoutExtension} altyazı yok")
                updateTaskState(Pair(file, false))
                setCurrentTask(null)
                continue
            }
            reader.readCues()
            for (i in 0 until reader.cuesCount) {
                reader.readSubtitlesInCueFrame(i)
            }

            var trackID = -1
            // İz adı ve iz ID'si eşlemesi, birden fazla İngilizce altyazı olabilir
            val trackMap = mutableMapOf<String,Int>()
            for (i in 0 until reader.subtitles.size) {
                val subtitles = reader.subtitles[i]
                if (englishIetfList.contains(subtitles.languageIetf) || english.contains(subtitles.language)) {
                    val name = if(subtitles.name.isNullOrEmpty()) "English" else subtitles.name
                    trackMap[name] = i
                } else {
                    // Altyazının küçük bir bölümünü çıkarın, altyazının dilini algılamak için OpenNLP'nin dil algılama aracını kullanın
                    val captionSize = subtitles.allReadCaptions.size
                    val subList = if(captionSize>10){
                        subtitles.readUnreadSubtitles().subList(0, 10)
                    }else if(captionSize> 5){
                        subtitles.readUnreadSubtitles().subList(0, 5)
                    }else{
                        subtitles.readUnreadSubtitles()
                    }

                    var content = ""
                    subList.forEach { caption ->
                        content += caption.stringData
                    }
                    val lang = languageDetector.predictLanguage(content)
                    if (lang.lang == "eng") {
                        val name = if(subtitles.name.isNullOrEmpty()) "English" else subtitles.name
                        trackMap[name] = i
                    }
                }
            }

            // SDH altyazılarını önceliklendir
            for ((name, id) in trackMap) {
                if (name.contains("SDH", ignoreCase = true)) {
                    trackID = id
                    logger.info("$name altyazı, TrackID: $id")
                    break
                }
            }
            if(trackID == -1){
                trackID = trackMap.values.first()
                logger.info("İngilizce altyazı, TrackID: $trackID")
            }

            if (trackID != -1) {
                val words = parseVideo(
                    pathName = file.absolutePath,
                    enablePhrases = enablePhrases,
                    trackId = trackID,
                    setProgressText = { }
                )
                orderList.addAll(words)
                updateTaskState(Pair(file, true))
            } else {
                errorMessage[file] = "İngilizce altyazı bulunamadı"
                logger.error("${file.nameWithoutExtension} İngilizce altyazı bulunamadı")
                updateTaskState(Pair(file, false))
                setCurrentTask(null)
                continue
            }

        } catch (exception: IOException) {
            updateTaskState(Pair(file, false))
            setCurrentTask(null)
            if (exception.message != null) {
                errorMessage[file] = exception.message.orEmpty()
                logger.error("${file.nameWithoutExtension} ${exception.message.orEmpty()}")
            } else {
                errorMessage[file] = "IO istisnası"
                logger.error("${file.nameWithoutExtension} IO istisnası\n ${exception.printStackTrace()}")
            }
            continue
        } catch (exception: UnSupportSubtitlesException) {
            updateTaskState(Pair(file, false))
            if (exception.message != null) {
                errorMessage[file] = exception.message.orEmpty()
                logger.error("${file.nameWithoutExtension} ${exception.message.orEmpty()}")
            } else {
                errorMessage[file] = "Altyazı biçimi desteklenmiyor"
                logger.error("${file.nameWithoutExtension} altyazı biçimi desteklenmiyor")
            }

            logger.error("${file.nameWithoutExtension} altyazı biçimi desteklenmiyor\n ${exception.printStackTrace()}")
            setCurrentTask(null)
            continue
        } catch (exception: NullPointerException) {
            updateTaskState(Pair(file, false))
            errorMessage[file] = "Boş işaretçi istisnası"
            logger.error("${file.nameWithoutExtension} boş işaretçi istisnası\n ${ exception.printStackTrace()}")
            setCurrentTask(null)
            continue
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                logger.error("${file.nameWithoutExtension}:\n ${ e.printStackTrace()}")
            }

        }
    }
    setErrorMessages(errorMessage)
    return orderList.toList()
}


/**
 * Bazı özel karakterleri değiştir
 */
fun replaceSpecialCharacter(captionContent: String): String {
    var content = captionContent
    if (content.startsWith("-")) content = content.substring(1)
    if (content.contains("<i>")) {
        content = content.replace("<i>", "")
    }
    if (content.contains("</i>")) {
        content = content.replace("</i>", "")
    }
    if (content.contains("<br />")) {
        content = content.replace("<br />", "\n")
    }
    content = removeLocationInfo(content)
    return content
}

/** Bazı altyazılar sabit bir konumda değil, karakterlerin yanında işaretlenmiştir, bu fonksiyon konum bilgilerini siler */
fun removeLocationInfo(content: String): String {
    val pattern = Pattern.compile("\\{.*\\}")
    val matcher = pattern.matcher(content)
    return matcher.replaceAll("")
}