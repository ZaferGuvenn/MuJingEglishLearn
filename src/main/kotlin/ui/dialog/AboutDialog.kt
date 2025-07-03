package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isMacOS
import player.isWindows
import state.getResourcesFile
import ui.components.LinkText
import ui.window.windowBackgroundFlashingOnCloseFixHack

/**
 * 关于 对话框
 */
@Composable
fun AboutDialog(
    version: String,
    close: () -> Unit
) {
    DialogWindow(
        title = "Hakkında", // "关于" -> "Hakkında"
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(795.dp, 650.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Divider()
                var state by remember { mutableStateOf(0) }
                LocalUriHandler.current
                if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                TabRow(
                    selectedTabIndex = state,
                    backgroundColor = Color.Transparent
                ) {
                    Tab(
                        text = { Text("Hakkında") }, // "关于" -> "Hakkında"
                        selected = state == 0,
                        onClick = { state = 0 }
                    )
                    Tab(
                        text = { Text("Üçüncü Parti Yazılımlar") }, // "第三方软件" -> "Üçüncü Parti Yazılımlar"
                        selected = state == 1,
                        onClick = { state = 1 }
                    )
                    Tab(
                        text = { Text("Teşekkürler") }, // "致谢" -> "Teşekkürler"
                        selected = state == 2,
                        onClick = { state = 2 }
                    )
                    Tab(
                        text = { Text("Lisans") }, // "许可" -> "Lisans"
                        selected = state == 3,
                        onClick = { state = 3 }
                    )
                }
                when (state) {
                    0 -> {
                        Column (modifier = Modifier.width(IntrinsicSize.Max).padding(start = 38.dp,top = 20.dp,end = 38.dp,bottom = 20.dp)){

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                Image(
                                    painter = painterResource("logo/logo.png"),
                                    contentDescription = "logo",
                                    modifier = Modifier.width(70.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                SelectionContainer {
                                    Text("Learna $version") // "幕境" -> "Learna"
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)){
                                Text("GitHub Adresi:     ") // "GitHub 地址：" -> "GitHub Adresi:"
                                LinkText(
                                    text = "https://github.com/tangshimin/MuJing",
                                    url =  "https://github.com/tangshimin/MuJing"
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)){
                                Text("Kullanıcı Geri Bildirim Adresi:   ") // "用户反馈地址：" -> "Kullanıcı Geri Bildirim Adresi:"
                                LinkText(
                                    text = "https://support.qq.com/products/594079/",
                                    url =  "https://support.qq.com/products/594079/"
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)){
                                Text("E-posta:                ",modifier = Modifier.padding(end = 1.dp)) // "邮箱：" -> "E-posta:"
                                LinkText(
                                    text = "tang_shimin@qq.com",
                                    url = "mailto:tang_shimin@qq.com"
                                )
                            }


                        }
                    }
                    1 -> {
                        Box(Modifier.fillMaxSize()){
                            val stateVertical = rememberScrollState(0)
                            Column (Modifier.padding(start = 38.dp,top = 20.dp,end = 38.dp,bottom = 20.dp)
                                .verticalScroll(stateVertical)){


                                val LGPL = Pair( "LGPL","https://www.gnu.org/licenses/lgpl-3.0.html")
                                val GPL2 = Pair( "GPL 2","https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html")
                                val GPL3 = Pair( "GPL 3","https://www.gnu.org/licenses/gpl-3.0.en.html")
                                val Apache2 = Pair( "Apache-2.0","https://www.apache.org/licenses/LICENSE-2.0")
                                val MIT = Pair( "MIT","https://opensource.org/licenses/mit-license.php")
                                val EPL2 = Pair( "EPL2","https://www.eclipse.org/legal/epl-v20.html")
                                val BSD2 = Pair( "BSD-2-Clause","https://www.eclipse.org/legal/epl-v20.html")

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ){
                                    Text("Yazılım") // "软件" -> "Yazılım"
                                    Text("Lisans") // "License" -> "Lisans" (veya İngilizce kalabilir)
                                }

                                Divider()
                                // 各个平台的版本不一致，windows 版本是 3.0.17 ,mac intel 版本是 3.0.20 mac m1 版本是 3.0.21
                                val vlcVersion = if(isWindows()){
                                    "3.0.17.4"
                                }else if(isMacOS()){
                                    val arch = System.getProperty("os.arch").lowercase()
                                     if (arch == "arm" || arch == "aarch64") {
                                        "3.0.21"
                                    }else {
                                        "3.0.20"
                                    }
                                } else {
                                    ""
                                }
                                Dependency(
                                    name = "VLC Media Player",
                                    url = "https://www.videolan.org/",
                                    version = vlcVersion,
                                    license = GPL2,
                                )
                                Dependency(
                                    name = "VLCJ",
                                    url = "https://github.com/caprica/vlcj",
                                    version = "4.7.2",
                                    license = GPL3,
                                )
                                Dependency(
                                    name = "FFmpeg",
                                    url = "https://ffmpeg.org/",
                                    version = "7.0",
                                    license = LGPL,
                                )
                                Dependency(
                                    name = "FFmpeg CLI Wrapper",
                                    url = "https://github.com/bramp/ffmpeg-cli-wrapper",
                                    version = "0.8.0",
                                    license = BSD2,
                                )
                                Dependency(
                                    name = "FlatLaf",
                                    url = "https://github.com/JFormDesigner/FlatLaf",
                                    version = "3.1",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "Ktor",
                                    url = "https://github.com/ktorio/ktor",
                                    version = "2.3.11",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "SQLite JDBC Driver",
                                    url = "https://github.com/xerial/sqlite-jdbc",
                                    version = "3.44.1.0",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "Apache OpenNLP",
                                    url = "https://opennlp.apache.org/",
                                    version = "1.9.4",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "Apache PDFBox",
                                    url = "https://pdfbox.apache.org/",
                                    version = "2.0.24",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "Compose Multiplatform",
                                    url = "https://github.com/JetBrains/compose-jb",
                                    version = "1.4.0",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "jetbrains compose material3",
                                    url = "https://github.com/JetBrains/compose-multiplatform",
                                    version = "1.0.1",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "material-icons-extended",
                                    url = "https://github.com/JetBrains/compose-jb",
                                    version = "1.0.1",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "kotlin",
                                    url = "https://github.com/JetBrains/kotlin",
                                    version = "1.8.0",
                                    license = Apache2,
                                )

                                Dependency(
                                    name = "kotlinx-coroutines-core",
                                    url = "https://github.com/Kotlin/kotlinx.coroutines",
                                    version = "1.6.0",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "kotlinx-serialization-json",
                                    url = "https://github.com/Kotlin/kotlinx.serialization",
                                    version = "1.8.0",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "maven-artifact",
                                    url = "https://maven.apache.org/",
                                    version = "3.8.6",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "ComposeReorderable",
                                    url = "https://github.com/aclassen/ComposeReorderable",
                                    version = "0.9.6.2",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "POI",
                                    url = "https://poi.apache.org/",
                                    version = "5.2.5",
                                    license = Apache2,
                                )
                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {  }.fillMaxWidth().padding(bottom = 5.dp)){
                                    Row{
                                        LinkText(
                                            text = "Juniversalchardet",
                                            url = "https://github.com/albfernandez/juniversalchardet"
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("2.4.0")
                                    }
                                    Row{
                                        LinkText(
                                            text = "MPL 1.1",
                                            url = "https://www.mozilla.org/en-US/MPL/1.1/"
                                        )
                                        Text("/")
                                        LinkText(
                                            text = "GPL 3.0",
                                            url = "http://www.gnu.org/licenses/gpl.txt"
                                        )
                                        Text("/")
                                        LinkText(
                                            text = "LGPL 3.0",
                                            url = "http://www.gnu.org/licenses/lgpl.txt"
                                        )
                                    }

                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {  }.fillMaxWidth().padding(bottom = 5.dp)){
                                    Row{
                                        LinkText(
                                            text = "logback",
                                            url = "https://logback.qos.ch/"
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("1.4.14")
                                    }
                                    Row{
                                        LinkText(
                                            text = "EPL 1",
                                            url = "http://www.eclipse.org/legal/epl-v10.html"
                                        )
                                        Text("/")
                                        LinkText(
                                            text = "LGPL 2.1",
                                            url = "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
                                        )
                                    }

                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {  }.fillMaxWidth().padding(bottom = 5.dp)){
                                    Row{
                                        LinkText(
                                            text = "Java Native Access",
                                            url = "https://github.com/java-native-access/jna "
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("5.14.0")
                                    }
                                    Row{
                                        LinkText(
                                            text = "Apache 2",
                                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                                        )
                                        Text("/")
                                        LinkText(
                                            text = "LGPL 2.1",
                                            url = "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
                                        )
                                    }

                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {  }.fillMaxWidth().padding(bottom = 5.dp)){
                                    Row{
                                        LinkText(
                                            text = "Java Native Access Platform",
                                            url = "https://github.com/java-native-access/jna "
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("5.14.0")
                                    }
                                    Row{
                                        LinkText(
                                            text = "Apache 2",
                                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                                        )
                                        Text("/")
                                        LinkText(
                                            text = "LGPL 2.1",
                                            url = "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
                                        )
                                    }

                                }
                                Dependency(
                                    name = "junit-jupiter",
                                    url = "https://junit.org/junit5/",
                                    version = "5.8.1",
                                    license = EPL2,
                                )
                                Dependency(
                                    name = "jetbrains compose ui-test-junit4",
                                    url = "https://github.com/JetBrains/compose-jb",
                                    version = "1.2.0-alpha01-dev620",
                                    license = Apache2,
                                )
                                Dependency(
                                    name = "subtitleConvert",
                                    url = "https://github.com/JDaren/subtitleConverter",
                                    version = "1.0.2",
                                    license = MIT,
                                )
                                Dependency(
                                    name = "LyricConverter",
                                    url = "https://github.com/IntelleBitnify/LyricConverter",
                                    version = "1.0",
                                    license = MIT,
                                )
                                Dependency(
                                    name = "Jacob ",
                                    url = "https://github.com/freemansoft/jacob-project",
                                    version = "1.2.0",
                                    license = LGPL,
                                )
                                Dependency(
                                    name = "Multiplatform File Picker",
                                    url = "https://github.com/Wavesonics/compose-multiplatform-file-picker",
                                    version = "1.0.0",
                                    license = MIT,
                                )
                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                    Row{
                                        LinkText(
                                            text = "EBMLReader",
                                            url = "https://github.com/matthewn4444/EBMLReader"
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("0.1.0")
                                    }
                                }

                                Divider()
                                Row(horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){

                                    Text("Yerel Sözlük: ") // "本地词典：" -> "Yerel Sözlük: "
                                    LinkText(
                                        text = "ECDICT Yerel Sözlük", // "ECDICT 本地词典" -> "ECDICT Yerel Sözlük"
                                        url = "https://github.com/skywind3000/ECDICT"
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                    Text("Kelime Telaffuzu: Kelime ses verileri ") // "单词发音：单词的语音数据来源于 " -> "Kelime Telaffuzu: Kelime ses verileri "
                                    LinkText(
                                        text = "Youdao Sözlük", // "有道词典" -> "Youdao Sözlük"
                                        url = "https://www.youdao.com/"
                                    )
                                    Text(" çevrimiçi telaffuz API'sinden gelir.") // " 在线发音 API" -> " çevrimiçi telaffuz API'sinden gelir."
                                }
                                Row(horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                    Text("Bu programda kullanılan ses efektleri: ") // "本程序使用的音效：" -> "Bu programda kullanılan ses efektleri: "
                                    LinkText(
                                        text = "Success!!",
                                        url = "https://freesound.org/people/jobro/sounds/60445/"
                                    )
                                    Text("|", modifier = Modifier.padding(start = 5.dp,end =5.dp))
                                    LinkText(
                                        text = "short beep",
                                        url = "https://freesound.org/people/LittleJohn13/sounds/566677/"
                                    )
                                    Text("|", modifier = Modifier.padding(start = 5.dp,end =5.dp))
                                    LinkText(
                                        text = "hint",
                                        url = "https://freesound.org/people/dland/sounds/320181/"
                                    )
                                }

                            }

                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateVertical)
                            )
                        }
                    }
                    2 -> {

                        Column (Modifier.padding(start = 18.dp,top = 20.dp,end = 18.dp,bottom = 20.dp)){
                            Row(horizontalArrangement = Arrangement.Start){
                                Text("Tüm ") // "感谢 " -> "Tüm "
                                LinkText(
                                    text = "qwerty-learner",
                                    url = "https://github.com/Kaiyiwing/qwerty-learner"
                                )
                                Text(" katkıda bulunanlarına, bir zamanlar vazgeçtiğim bir uygulamayı yeniden hayata geçirme fırsatı verdikleri için teşekkürler.") // "的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。"
                            }
                            Row{
                                Text("") // "感谢 " kaldırıldı, cümle akışı düzenlendi.
                                LinkText(
                                    text = "skywind3000",
                                    url = "https://github.com/skywind3000"
                                )
                                Text(" adlı kullanıcıya açık kaynaklı ") // "开源" -> " adlı kullanıcıya açık kaynaklı "
                                LinkText(
                                    text = "ECDICT",
                                    url = "https://github.com/skywind3000/ECDICT"
                                )
                                Text(" için teşekkürler.") // "。" -> " için teşekkürler."
                            }
                            Row{
                                Text("") // "感谢 " kaldırıldı
                                LinkText(
                                    text = "libregd",
                                    url = "https://github.com/libregd"
                                )
                                Text(" adlı kullanıcıya, bu projeye bazı etkileşim tasarımları ve çok iyi özellik önerileriyle katkıda bulunduğu ve Typing Learner için Logo tasarladığı için teşekkürler.") // " 为本项目贡献了一些交互设和及非常好的功能建议，以及为 Typing Learner 设计 Logo。"
                            }
                            Row{
                                Text("") // "感谢" kaldırıldı
                                LinkText(
                                    text = "NetEase Youdao", // "网易有道" -> "NetEase Youdao" (Özel isim olduğu için İngilizce bırakıldı veya daha bilinen adı kullanıldı)
                                    url = "https://www.youdao.com/"
                                )
                                Text(" adlı platforma, bu projeye profesyonel sözlük telaffuzları sağladığı için teşekkürler.") // "为本项目提供专业的词典发音。"
                            }
                        }

                    }
                    3 -> {
                        val file = getResourcesFile("LICENSE")
                        if (file.exists()) {
                            val license = file.readText()
                            Box(Modifier.fillMaxWidth().height(550.dp)) {
                                val stateVertical = rememberScrollState(0)
                                Box(Modifier.verticalScroll(stateVertical).align(Alignment.Center)) {
                                    SelectionContainer (Modifier.align(Alignment.Center)){
                                        Text(
                                            license,
                                            modifier = Modifier.padding(start = 38.dp, top = 20.dp, end = 38.dp)
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                        .fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(stateVertical)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { close() }) {
                        Text("Tamam") // "确定" -> "Tamam"
                    }
                }
            }

        }
    }
}

@Composable
fun Dependency(
    name:String,
    url:String,
    version:String,
    license:Pair<String,String>,
){
    Row(horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {  }.fillMaxWidth().padding(bottom = 5.dp)){
        Row{
            LinkText(
                text = name,
                url = url
            )
            Spacer(Modifier.width(5.dp))
            Text(version)
        }
        LinkText(
            text = license.first,
            url = license.second
        )
    }
}