package ui.dialog

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import ui.window.windowBackgroundFlashingOnCloseFixHack
import java.util.*
import kotlin.concurrent.schedule

@Composable
fun DocumentWindow(
    close: () -> Unit,
    currentPage:String,
    setCurrentPage:(String) -> Unit
) {
    Window(
        title = "Kullanım Kılavuzu",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1170.dp, 720.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface {
            Column (Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
                Divider()
                Row{
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.width(200.dp).fillMaxHeight()
                    ) {
                        val selectedColor = if(MaterialTheme.colors.isLight) Color(245, 245, 245) else Color(41, 42, 43)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "features")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("features" )}) {
                            Text("Ana Özellikler", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "features"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "vocabulary")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("vocabulary" )}) {
                            Text("Kelime Dağarcığı Tanıtımı", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "vocabulary"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "tips")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("tips" )}) {
                            Text("Kullanım İpuçları", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "tips"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "document")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("document") }) {
                            Text("Belgeden Kelime Dağarcığı Oluştur", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "document"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "subtitles")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("subtitles") }) {
                            Text("Altyazıdan Kelime Dağarcığı Oluştur", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "subtitles"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "video")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("video")}) {
                            Text("Videodan Kelime Dağarcığı Oluştur", modifier = Modifier.padding(start = 16.dp))
                            if( currentPage == "video"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "Danmaku")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("Danmaku") }) {
                            Text("Kelime Baloncuğu Nasıl Açılır", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "Danmaku"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "linkVocabulary")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("linkVocabulary") }) {
                            Text("Altyazı Kelime Dağarcığını Bağla", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "linkVocabulary"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "linkCaptions")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("linkCaptions") }) {
                            Text("Altyazıyı Bağla", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "linkCaptions"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "download")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("download") }) {
                            Text("Video Kaynaklarını İndir", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "download"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }

                    }
                    Divider(Modifier.width(1.dp).fillMaxHeight())

                    when(currentPage){
                        "features" -> {
                            FeaturesPage()
                        }
                        "vocabulary" -> {
                            VocabularyPage()
                        }
                        "tips" -> {
                            Tips()
                        }
                        "document" -> {
                            DocumentPage()
                        }
                        "subtitles" -> {
                            SubtitlesPage()
                        }
                        "video" -> {
                            VideoPage()
                        }
                        "download" -> {
                            DownloadPage()
                        }
                        "Danmaku" -> {
                            DanmakuPage()
                        }
                        "linkVocabulary" -> {
                            LinkVocabularyPage()
                        }
                        "linkCaptions" -> {
                            LinkCaptionsPage()
                        }
                    }
                }
            }

        }
    }
}

const val frequencyText = "\nİngiliz Ulusal Dil Derlemi (BNC) ve Çağdaş Amerikan İngilizcesi Derlemi (COCA) içindeki kelime sıklığı sıralamasına giriş\n" +
        "BNC kelime sıklığı istatistikleri son birkaç yüz yıldaki çeşitli İngilizce materyallerin geçmişini kapsarken, Çağdaş Dil Derlemi yalnızca son 20 yılı kapsar. Neden her ikisi de sağlanmaktadır?\n" +
        "Çok basit, 'quay' (rıhtım) kelimesi Çağdaş Dil Derlemi'nde (COCA) yirmi binin üzerindedir, bu yüzden öğrenilmesi gerekmeyen nadir bir kelime olduğunu düşünebilirsiniz, ancak BNC'de\n" +
        "8906. sırada yer alır, bu da onu temelde sık kullanılan bir kelime yapar. Neden mi? Geçmişte denizciliğin hala önemli bir ulaşım aracı olduğunu hayal edebilirsiniz, bu nedenle geçmişteki çeşitli\n" +
        "yazılı materyallerde bu kelimeden daha sık bahsedilmiştir. 19. yüzyıl ve öncesine ait çeşitli klasikleri anlamak istiyorsanız, BNC kelime sıklığının çok faydalı olduğunu göreceksiniz. Ve çeşitli\n" +
        "modern dergileri okumak istiyorsanız, Çağdaş Dil Derlemi'nin rolü belirginleşir. Örneğin, 'Taliban', BNC kelime sıklığında temel olarak yer almaz (ilk 200.000\n" +
        "kelime dağarcığına girmez), ancak Çağdaş Dil Derlemi'nde 6089 numaraya yükselmiştir, sık kullanılanlar arasında sık kullanılır. BNC daha kapsamlı ve gelenekseldir ve hedeflenmiş öğrenme, çeşitli\n" +
        "yabancı edebi klasikleri okumanıza yardımcı olabilir. Çağdaş Dil Derlemi daha modern ve günceldir ve teknolojiyle yakından ilişkilidir. Bu nedenle, ikisini birleştirmek işleri kolaylaştırır.[2]\n"
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrequencyRelatedLink(){

    val uriHandler = LocalUriHandler.current
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom =10.dp)){
        Text("[2] ")
        val annotatedString1 = buildAnnotatedString {
            pushStringAnnotation(tag = "android", annotation = "https://github.com/skywind3000/ECDICT#单词标注")
            withStyle(style = SpanStyle(color = blueColor)) {
                append("https://github.com/skywind3000/ECDICT#单词标注")
            }
            pop()
        }
        ClickableText(
            text = annotatedString1,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .pointerHoverIcon(Hand),
            onClick = { offset ->
                annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                    uriHandler.openUri(it.item)
                }
            })
    }
}


@Composable
fun FeaturesPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()){
                Image(
                    painter = painterResource("screenshot/features-$theme/features-word.png"),
                    contentDescription = "features-word",
                    modifier = Modifier.width(150.dp).height(90.dp)
                )
                Spacer(Modifier.width(200.dp))
            }

            Text("Kelimeleri ezberlerken, kelimenin telaffuzu otomatik olarak oynatılır, ardından yazarak pratik yapmak için klavyeyi kullanırsınız. Her kelime hatırlanana kadar birden çok kez girilebilir. Videolardan oluşturulan kelime dağarcıklarından (kelime defterleri), kelimelere karşılık gelen altyazıları kopyalayabilir ve kelimelere karşılık gelen video klipleri oynatabilirsiniz. Varsayılan olarak bir sonraki kelimeye geçmek için Enter tuşu kullanılır.\n\n")

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()){
                Image(
                    painter = painterResource("screenshot/features-$theme/features-subtitle.png"),
                    contentDescription = "features-subtitle",
                    modifier = Modifier.width(150.dp).height(90.dp)
                )
                Spacer(Modifier.width(200.dp))
            }



            Text("Altyazı görüntüleyici, altyazılara göz atmanıza, Amerikan dizileri, filmler ve TED konuşmalarıyla gölgeleme alıştırması yapmanıza olanak tanır. İsteğe bağlı olarak bir veya daha fazla altyazı satırını oynatabilirsiniz. Birden çok satırı oynatmak istiyorsanız, etkinleştirmek için soldaki numaraya tıklayın. 5 ve 10'a tıklayıp ardından soldaki oynat düğmesine tıklarsanız, 5. satırdan başlayıp 10. satırda sona erecektir. Altyazıları da kopyalayabilirsiniz.\n\n")

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()){
                Image(
                    painter = painterResource("screenshot/features-$theme/features-player.png"),
                    contentDescription = "features-player",
                    modifier = Modifier.width(150.dp).height(90.dp)
                )
                Spacer(Modifier.width(200.dp))
            }

            Text("Kelimeleri akan yazı (Danmaku) biçiminde gözden geçirin. Bir film oynatırken, filmden oluşturulan kelime dağarcığını oynatıcıya ekleyin; kelimeler akan yazı olarak görünecektir. Belirli bir kelimenin Çince açıklamasını görüntülemek için kelimeyi veya karşılık gelen numarayı girmeniz yeterlidir." +
                    "Akan yazıyı açmanın kısayolu: Video veya altyazılardan oluşturulmuş bir kelime dağarcığını ezberliyorsanız, videoyu kelime ezberleme arayüzüne sürükleyip bırakarak videoyu ve akan yazıyı hızla açabilirsiniz.\n\n")
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()){
                Image(
                    painter = painterResource("screenshot/features-$theme/features-text.png"),
                    contentDescription = "features-text",
                    modifier = Modifier.width(150.dp).height(90.dp)
                )
                Spacer(Modifier.width(200.dp))
            }


            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()){
                Text("Metin kopyalama, txt formatındaki metinleri kopyalayabilirsiniz")
                Spacer(Modifier.width(200.dp))
            }


        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }


}
@Composable
fun VocabularyPage(){
    val ctrl = LocalCtrl.current
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        Text("Kelime dağarcıkları iki kategoriye ayrılabilir:\n" +
                "     • Belge kelime dağarcıkları: Yazılımdaki dahili kelime dağarcıkları belge kelime dağarcıklarıdır ve belgeler kullanılarak oluşturulan kelime dağarcıkları da belge kelime dağarcıklarıdır.\n" +
                "     • Altyazı kelime dağarcıkları: Altyazı kelime dağarcıkları da iki kategoriye ayrılır:\n" +
                "         • MKV kelime dağarcıkları: MKV veya MP4 videolarındaki dahili altyazılar kullanılarak oluşturulan kelime dağarcıkları.\n" +
                "         • SUBTITLES kelime dağarcıkları: Harici altyazılar kullanılarak oluşturulan kelime dağarcıkları.\n\n" +
                "Altyazı kelime dağarcıklarındaki ve MKV kelime dağarcıklarındaki altyazılar, belge kelime dağarcıklarındaki kelimelere bağlanabilir.\n" +
                "Altyazı kelime dağarcıklarını ve ilgili video dosyalarını aynı klasöre koymanız önerilir, böylece altyazı kelime dağarcıklarını ve videoları arkadaşlarınızla birlikte paylaşabilirsiniz. Altyazı kelime dağarcığını oluşturduktan sonra ilişkili videonun adını değiştirmeyin.\n"
        )

        Text("Bildik kelimeler: Çok tanıdık, artık ezberlenmesi gerekmeyen kelimeler.\n" +
                "Kelimeleri ezberlerken, bir kelimenin çok tanıdık olduğunu ve artık ezberlenmesi gerekmediğini düşünüyorsanız, bu kelimeyi bildiklerinize eklemek için $ctrl + Y kısayol tuşunu kullanabilirsiniz.\n" +
                "Bir kelime dağarcığı oluştururken, soldaki filtre alanında bildik kelimeleri seçerek bunları toplu olarak filtreleyebilirsiniz.\n")

        Text("Zor kelimeler: Yazılması zor kelimeler, örneğin düzensiz telaffuzlu kelimeler veya daha uzun kelimeler. Bu kelimeyi zor kelimeler listesine eklemek için $ctrl + I kısayol tuşunu kullanabilirsiniz.\n")
    }
}

@Composable
fun Tips(){
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
        val ctrl = LocalCtrl.current
        Row(Modifier.fillMaxWidth()){
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold,color = MaterialTheme.colors.onBackground)) {
                    append("Kelimeyi Kopyala")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append(", kopyalamakta olduğunuz altyazıyı veya metni kopyalamak istiyorsanız, önce imleci kopyalamak istediğiniz satıra getirin, ardından ")
                }

                withStyle(style = SpanStyle(color =  MaterialTheme.colors.primary,background = background)) {
                    append("$ctrl + B")
                }

                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append(" tuşuna basın, böylece kelimeyi kopyalayabilirsiniz. ")
                }
            }
            Text(annotatedString)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)){
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold,color = MaterialTheme.colors.onBackground)) {
                    append("Video Akan Yazısını Hızla Aç")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append(", videodan oluşturulmuş bir kelime dağarcığını ezberliyorsanız, videoyu kelime ezberleme arayüzüne sürükleyip bırakarak videoyu ve akan yazıyı hızla açabilirsiniz.")
                }
            }
            Text(annotatedString)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)){
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold,color = MaterialTheme.colors.onBackground)) {
                    append("Birden Fazla Altyazı Satırını Oynat")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append(", altyazı görüntüleyici arayüzünde, birden fazla altyazı satırını oynatmak istiyorsanız, etkinleştirmek için soldaki numaraya tıklayın. 5 ve 10'a tıklayıp ardından soldaki oynat düğmesine tıklarsanız, " +
                            "5. satırdan başlayıp 10. satırda sona erecektir. Kısayol tuşu ")
                }
                withStyle(style = SpanStyle(color =  MaterialTheme.colors.primary,background = background)) {
                    append("$ctrl + N ")
                }
            }
            Text(annotatedString)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"

            Text("\n1. Fareyi ekranın üst kısmındaki menü çubuğuna getirin > Kelime Dağarcığı'na tıklayın > ardından Belgeden Kelime Dağarcığı Oluştur'a tıklayın, sonra belgeyi seçin. Hızlı açmak için belgeyi pencereye sürükleyip bırakabilirsiniz,\n" +
                    "    Burada 1300 sayfalık bir Android geliştirme İngilizce belgesi seçtim[1]. Başlat düğmesine tıklayın.\n")
            Image(
                painter = painterResource("screenshot/document-$theme/document-1.png"),
                contentDescription = "document-step-1", // Belge Adım 1
                modifier = Modifier.width(640.dp).height(150.dp).padding(start = 20.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            SameSteps()
            val uriHandler = LocalUriHandler.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Row (verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 30.dp)){
                Text("[1]Örnek belge AndroidNotesForProfessionals şuradan alınmıştır:")
                val annotatedString1 = buildAnnotatedString {
                    pushStringAnnotation(tag = "android", annotation = "https://goalkicker.com/AndroidBook/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("https://goalkicker.com/AndroidBook/")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString1,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand),
                    onClick = { offset ->
                        annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()

        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@Composable
fun SameSteps(){
    val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
    Text("\nSağdaki önizleme alanında program tarafından oluşturulan kelimeleri görebilirsiniz. Herhangi bir kelimeyi silmek istemiyorsanız, doğrudan sağ alt köşedeki kaydet düğmesine tıklayabilirsiniz,\n" +
            "Çok sayıda rakam veya artık ezberlemek istemediğiniz birçok tanıdık kelime varsa, gereksiz kelimeleri filtrelemek için soldaki filtre seçeneğini seçin.\n",
        modifier = Modifier.padding(start = 20.dp)
        )

    Text("\n2. Soldaki, kelime sıklığı sırası 0 olan kelimeleri filtrele'ye tıklayabilirsiniz. Kelime sıklığı 0 olan kelimeler arasında basit harfler ve rakamların yanı sıra kelime sıklığı sırasına dahil edilmemiş bazı nadir kelimeler bulunur.")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
        Spacer(Modifier.width(3.dp).height(180.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
        Text(frequencyText, modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)) // frequencyText zaten çevrildi
    }

    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-2.png"),
            contentDescription = "step-2", // Adım 2
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }

    Text("\n3. [İlk 1000 COCA kelime sıklığını filtrele] veya [İlk 1000 BNC kelime sıklığını filtrele] seçeneklerini işaretleyebilirsiniz,\n" +
            "    En yaygın 1000 kelimeyi filtreleyin, bu değer 2000 veya 3000 olarak değiştirilebilir.")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-7.png"),
            contentDescription = "step-3", // Adım 3
            modifier = Modifier.width(406.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(406.dp).padding(start = 20.dp))
    }
    Text("\n4. Ayrıca tüm türetilmiş kelimeleri kök kelimelerle değiştirebilirsiniz.")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-3.png"),
            contentDescription = "step-4", // Adım 4
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }

    Text("\n5. Rakamlar varsa, rakamları filtrelemek için de kullanabilirsiniz.")
    Column {

        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-4.png"),
            contentDescription = "step-5", // Adım 5
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }

    Text("\n6. Önceki filtrelemeden sonra hala çok tanıdık kelimeleriniz varsa, örneğin Oxford Core 5000 kelimesine zaten çok aşinaysanız,\n" +
            "    Soldaki dahili kelime dağarcığına tıklayın, ardından şunu seçin: Oxford Temel Kelimeler -> The_Oxford_5000. Seçimden sonra kelime sayısı önemli ölçüde azaldı mı?")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-5.png"),
            contentDescription = "step-6", // Adım 6
            modifier = Modifier.width(475.dp).height(636.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(475.dp).padding(start = 20.dp))
    }

    Text("\n7. Hala tanıdık kelimeleriniz varsa, önce sıralamayı [COCA kelime sıklığına göre sırala] veya [BNC kelime sıklığına göre sırala] olarak değiştirebilirsiniz,\n" +
            "    Bu şekilde tanıdık kelimeler en üstte görünecektir. Ardından kelimenin sağ üst köşesindeki sil düğmesine tıklamak için fareyi kullanın; silinen kelimeler bildik kelimeler listesine eklenecektir.\n")

    Image(
        painter = painterResource("screenshot/mkv-$theme/MKV-6.png"),
        contentDescription = "step-7", // Adım 7
        modifier = Modifier.width(890.dp).height(400.dp).padding(start = 20.dp)
            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
    )
    Text("\n8. Kelimeleri ezberlerken tanıdık kelimeleri de silebilirsiniz. Fareyi ezberlemekte olduğunuz kelimenin üzerine getirdiğinizde bir menü açılacaktır; kelimeleri buradan silebilirsiniz.\n" +
            "    Kelimeleri doğrudan silmek için Delete kısayol tuşunu kullanabilirsiniz.\n")
    Image(
        painter = painterResource("screenshot/document-$theme/document-7.png"),
        contentDescription = "step-8", // Adım 8
        modifier = Modifier.width(620.dp).height(371.dp).padding(start = 20.dp,bottom = 10.dp)
            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
    )
    Text("\n9. Kelime dağarcığını uygulamanın kurulum dizinine kaydetmeyin. Yükseltme yaparken önce yazılımı kaldırmanız gerekir ve kaldırma sırasında kurulum dizini silinir.\n" +
            "    Dahili kelime dağarcığını ve oluşturulan kelime dağarcığını bir arada tutmak istiyorsanız, dahili kelime dağarcığını kopyalayabilirsiniz.\n",
        color = Color.Red)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubtitlesPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
                Spacer(Modifier.width(3.dp).height(130.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
                Column (Modifier.padding(start = 10.dp)){
                    Row(){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " Altyazılardan oluşturulan kelime dağarcıklarını kullanırken, her kelime en fazla üç altyazı satırıyla eşleştirilir.", fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " Kelime dağarcığı oluşturmak videoyu kesmez. Kelime dağarcığını oluşturduktan sonra videoyu yeniden adlandırmayın. Videoyu yeniden adlandırırsanız, videoyu oynatırken bir hata oluşur ve yeniden oluşturmanız gerekir.",
                            fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = " Oluşturulan kelime dağarcığını ilgili video dosyasının bulunduğu klasöre kaydetmeniz önerilir, böylece kelime dağarcığını ve videoyu arkadaşlarınızla birlikte paylaşabilirsiniz. Birlikte yerleştirildikten sonra, tüm klasörü taşırsanız video oynatılırken video adresi hatası olmaz.",
                            fontWeight = FontWeight.Bold)
                    }


                }
            }

            Text("\n1. Fareyi ekranın üst kısmındaki menü çubuğuna getirin > Kelime Dağarcığı'na tıklayın > ardından Altyazıdan Kelime Dağarcığı Oluştur'a tıklayın, sonra SRT altyazısını seçin,\n    Hızlı açmak için dosyayı pencereye sürükleyip bırakabilirsiniz, " +
                    "karşılık gelen bir video varsa, karşılık gelen videoyu seçin, ardından Başlat düğmesine tıklayın.[1]\n")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-1.png"),
                contentDescription = "subtitles-step-1", // Altyazılar Adım 1
                modifier = Modifier.width(633.dp).height(199.dp).padding(start = 20.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            SameSteps()
            Row{
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                Text("[1]Örnek altyazılar şuradan alınmıştır:")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "blender", annotation = "https://durian.blender.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Sintel")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.pointerHoverIcon(Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "blender", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@Composable
fun VideoPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Row(  verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
                Spacer(Modifier.width(3.dp).height(130.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
                Column (Modifier.padding(start = 10.dp)){
                    Row{
                        Text(text = "•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " Videolardan oluşturulan kelime dağarcıklarını kullanırken, her kelime en fazla üç altyazı satırıyla eşleştirilir.",
                            fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " Kelime dağarcığı oluşturmak videoyu kesmez. Kelime dağarcığını oluşturduktan sonra videoyu yeniden adlandırmayın. Videoyu yeniden adlandırırsanız, videoyu oynatırken bir hata oluşur ve yeniden oluşturmanız gerekir.",
                            fontWeight = FontWeight.Bold,)
                    }
                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = " Oluşturulan kelime dağarcığını videonun bulunduğu klasöre kaydetmeniz önerilir, böylece kelime dağarcığını ve videoyu arkadaşlarınızla birlikte paylaşabilirsiniz. Birlikte yerleştirildikten sonra, tüm klasörü taşırsanız video oynatılırken video adresi hatası olmaz.",
                            fontWeight = FontWeight.Bold)
                    }

                }
            }



            Text("\n1. Fareyi ekranın üst kısmındaki menü çubuğuna getirin > Kelime Dağarcığı'na tıklayın > ardından Videodan Kelime Dağarcığı Oluştur'a tıklayın, sonra MKV veya MP4 videosunu seçin,\n    Hızlı açmak için dosyayı pencereye sürükleyip bırakabilirsiniz, ardından Başlat düğmesine tıklayın.[1]\n")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
                Spacer(Modifier.width(3.dp).height(60.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
                Text(text = "En son sürüm, birden fazla videoyu sürükleyip bırakmayı destekler. Birden fazla videoyu pencereye sürükleyip bırakabilirsiniz. Birden fazla video kullanarak bir kelime dağarcığı oluşturmak için her videoda bir İngilizce altyazı parçası olduğundan emin olun. MuJing, her videodaki İngilizce altyazı parçasını çıkaracak ve ardından tüm altyazıları tek bir kelime dağarcığında birleştirecektir.",
                    modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
                )
            }
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-1.png"),
                contentDescription = "mkv-step-1", // MKV Adım 1
                modifier = Modifier.width(685.dp).height(192.dp).padding(start = 20.dp,top = 10.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            SameSteps()
            Row{
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                Text("[1]Örnek video şuradan alınmıştır:")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "Sintel", annotation = "https://www.youtube.com/watch?v=eRsGyueVLvQ")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Sintel")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "Sintel", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DownloadPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val uriHandler = LocalUriHandler.current
            val clipboard = LocalClipboardManager.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Text("Youtube Video İndirme:\n", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)){
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "yt-dlp", annotation = "https://github.com/yt-dlp/yt-dlp")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("yt-dlp")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "yt-dlp", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" Çok güçlü bir video indirme programı, 1000'den fazla video sitesinden video indirebilir,")
                Text("İngilizce altyazı ve video indirme komutu:")
            }
            val command = "yt-dlp.exe  --proxy \"URL\" --sub-lang en --convert-subs srt --write-sub URL"
            Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .background(if(MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35))){
                SelectionContainer {
                    Text("    $command")
                }

                Box{
                    var copyed by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                    clipboard.setText(AnnotatedString(command))
                    copyed = true
                    Timer("恢复状态", false).schedule(2000) {
                        copyed = false
                    }
                }){
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                    DropdownMenu(
                        expanded = copyed,
                        onDismissRequest = {copyed = false}
                    ){
                        Text("Kopyalandı")
                    }
                }


            }

            val annotatedString = buildAnnotatedString {
                pushStringAnnotation(tag = "howto", annotation = "https://zh.wikihow.com/%E4%B8%8B%E8%BD%BDYouTube%E8%A7%86%E9%A2%91") // URL Çince karakterler içeriyor, olduğu gibi bırakıyorum
                withStyle(style = SpanStyle(color = blueColor)) {
                    append("wikiHow: YouTube Videolarını İndirmenin 5 Yolu")
                }
                pop()
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.pointerHoverIcon(Hand).padding(start = 16.dp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "howto", start = offset, end = offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }
                })

            Text("\nTorrent İndirme:\n", fontWeight = FontWeight.Bold)
            val btString = buildAnnotatedString {
                pushStringAnnotation(tag = "howto", annotation = "https://zh.wikihow.com/%E4%B8%8B%E8%BD%BDBT%E7%A7%8D%E5%AD%90%E6%96%87%E4%BB%B6") // URL Çince karakterler içeriyor
                withStyle(style = SpanStyle(color = blueColor)) {
                    append("wikiHow: Torrent Dosyaları Nasıl İndirilir")
                }
                pop()
            }
            ClickableText(
                text = btString,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.pointerHoverIcon(Hand).padding(start = 16.dp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "howto", start = offset, end = offset).firstOrNull()?.let { // btString olmalıydı, düzeltiyorum
                        uriHandler.openUri(it.item)
                    }
                })

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)){
                Text("Torrent İstemcisi Önerileri:")
                val qbittorrentString = buildAnnotatedString {
                    pushStringAnnotation(tag = "qbittorrent", annotation = "https://www.qbittorrent.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("qbittorrent")
                    }
                    pop()
                }
                ClickableText(
                    text = qbittorrentString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        qbittorrentString.getStringAnnotations(tag = "qbittorrent", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Spacer(Modifier.width(10.dp))
                val xunleiString = buildAnnotatedString {
                    pushStringAnnotation(tag = "xunlei", annotation = "https://www.xunlei.com/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Xunlei") // Özel isim
                    }
                    pop()
                }
                ClickableText(
                    text = xunleiString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        xunleiString.getStringAnnotations(tag = "xunlei", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })


            }

            Text("\nAltyazı İndirme:\n", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)){
                Text("Çift Dilli Altyazı ")
                val subHDString = buildAnnotatedString {
                    pushStringAnnotation(tag = "SubHD", annotation = "https://subhd.tv/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("SubHD")
                    }
                    pop()
                }
                ClickableText(
                    text = subHDString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        subHDString.getStringAnnotations(tag = "SubHD", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Spacer(Modifier.width(10.dp))
                Text("İngilizce Altyazı ")
                val opensubtitlesString = buildAnnotatedString {
                    pushStringAnnotation(tag = "opensubtitles", annotation = "https://www.opensubtitles.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("OpenSubtitles")
                    }
                    pop()
                }
                ClickableText(
                    text = opensubtitlesString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        opensubtitlesString.getStringAnnotations(tag = "opensubtitles", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })


            }

        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

@Composable
fun DanmakuPage(){
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        Text(
           """
              Kelime baloncuğundaki kelimeler altyazı kelime dağarcığındaki kelimelerdir. Kelime baloncuğunu açmak için önce altyazıları veya videodaki dahili altyazıları kullanarak bir kelime dağarcığı oluşturmanız gerekir.
              Bir altyazı kelime dağarcığı oluşturduysanız, video oynatıcıyı açın > videoyu açın > kelime dağarcığını ekleyin, ardından kelime baloncuğunu açabilirsiniz.
              
              Kelime baloncuğunu açmanın başka bir hızlı yolu daha vardır: Videodan oluşturulmuş bir kelime dağarcığını ezberliyorsanız, videoyu kelime ezberleme arayüzüne sürükleyip bırakarak videoyu ve kelime baloncuğunu hızla açabilirsiniz.
           """.trimIndent()
        )
    }
}

@Composable
fun LinkVocabularyPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("Seviye 4 kelimelerini ezberliyorsanız ve altyazıları veya videoları kullanarak bir altyazı kelime dağarcığı oluşturduysanız ve bu kelime dağarcığında bazı Seviye 4 kelimeleri varsa,\n" +
                    "bu altyazıları Seviye 4 belge kelime dağarcığına bağlamak için altyazı kelime dağarcığını bağla işlevini kullanabilirsiniz. Bağlantıdan sonra altyazı kelime dağarcığını değiştirmek veya silmek belge kelime dağarcığını etkilemez.\n")
            Text("İpucu: Bağlı kelime dağarcığını uygulamanın kurulum dizinine kaydetmeyin\n")
            Text("1. Altyazılar > Altyazı Kelime Dağarcığını Bağla (L) altyazı bağlama iletişim kutusunu açın, ardından bir kelime dağarcığı seçin veya bir kelime dağarcığını pencereye sürükleyip bırakın.")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-1.png"),
                contentDescription = "link-vocabulary-1", // Bağlantı Kelime Dağarcığı 1
                modifier = Modifier.width(590.dp).height(436.dp).padding(start = 20.dp)
            )
            Text("\n2. Burada örnek olarak Seviye 4 kelime dağarcığı kullanılmıştır.")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-2.png"),
                contentDescription = "link-vocabulary-2", // Bağlantı Kelime Dağarcığı 2
                modifier = Modifier.width(590.dp).height(436.dp).padding(start = 20.dp)
            )
            Text("\n3. Ardından altyazılı başka bir kelime dağarcığı seçin. Seçtikten sonra video kliplerini önizleyebilir, ardından bağlantıya tıklayabilirsiniz; altyazılı kelime dağarcığı altyazısız kelime dağarcığına bağlanacaktır.")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-3.png"),
                contentDescription = "link-vocabulary-3", // Bağlantı Kelime Dağarcığı 3
                modifier = Modifier.width(590.dp).height(650.dp).padding(start = 20.dp)
            )
            Text("\n4. Bağlantıya tıkladıktan sonra ana altyazı bağlama arayüzüne dönün. Birden fazla altyazılı kelime dağarcığını da bağlayabilirsiniz. Zaten bağlanmış altyazıları da silebilirsiniz. Bağlamak istemiyorsanız kaydet'e tıklayın ve son olarak kelime dağarcığını uygulamanın kurulum dizinine kaydetmemeye dikkat edin.")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-4.png"),
                contentDescription = "link-vocabulary-4", // Bağlantı Kelime Dağarcığı 4
                modifier = Modifier.width(580.dp).height(436.dp).padding(start = 20.dp)
            )

        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

@Composable
fun LinkCaptionsPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text(
                "Seviye 4 kelimelerini ezberliyorsanız ve altyazıları veya videoları kullanarak bir altyazı kelime dağarcığı oluşturduysanız ve Seviye 4 kelime dağarcığında örneğin 'Dragon' gibi bir kelime varsa ve altyazı kelime dağarcığında da 'Dragon' varsa," +
                        "Tüm kelime dağarcığı yerine yalnızca altyazı kelime dağarcığındaki tek bir kelimeyi bağlamak istiyorsanız, altyazı bağlama işlevini kullanabilirsiniz.\n\n"
            )

            Text(
                "1. Bu işlev kelime düzenleme arayüzündedir. Kelime düzenlemeyi açmanın iki yolu vardır:\n" +
                        "Biri kelime ezberleme arayüzündedir. Fareyi ezberlemekte olduğunuz kelimenin üzerine getirdiğinizde bir menü açılacaktır, ardından kelimeyi düzenle'yi seçin.\n" +
                        "Diğeri kelime dağarcığını düzenleme arayüzündedir. Bir kelime seçip farenin sol tuşuna çift tıkladığınızda kelime düzenleme arayüzü açılır.\n"

            )
            Row(Modifier.padding(start = 155.dp)){
                Image(
                    painter = painterResource("screenshot/link-captions-$theme/edit word button.png"),
                    contentDescription = "edit word button", // Kelime Düzenle Düğmesi
                    modifier = Modifier.width(520.dp).height(250.dp).padding(start = 20.dp)

                )
            }
            Text(
                "\n\n2. Kelime düzenlemeyi açtıktan sonra, mevcut kelimenin altyazı sayısı 3'ten azsa, altyazı bağlama işlevi altta görünecektir.\n"
            )
            Image(
                painter = painterResource("screenshot/link-captions-$theme/edit word.png"),
                contentDescription = "edit word", // Kelime Düzenle
                modifier = Modifier.width(850.dp).height(807.dp).padding(start = 20.dp)
            )
            Text(
                "\n\n3. Açtıktan sonra bir altyazı kelime dağarcığı seçin. Altyazı kelime dağarcığında mevcut kelime dağarcığıyla eşleşen kelimeler varsa, bir altyazı listesi görünecektir.\n" +
                        "Ardından karşılık gelen altyazıyı seçmeniz yeterlidir.\n"
            )
            Row(Modifier.padding(start = 115.dp,bottom = 20.dp)){
                Image(
                    painter = painterResource("screenshot/link-captions-$theme/link caption.png"),
                    contentDescription = "link caption", // Altyazı Bağla
                    modifier = Modifier.width(621.dp).height(697.dp).padding(start = 20.dp)
                )
            }

        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}
