package ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.GitHubRelease
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.maven.artifact.versioning.ComparableVersion
import ui.window.windowBackgroundFlashingOnCloseFixHack

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun UpdateDialog(
    version: String,
    close: () -> Unit,
    autoUpdate:Boolean,
    setAutoUpdate:(Boolean) -> Unit,
    latestVersion:String,
    releaseNote:String,
    ignore:(String) -> Unit,
) {
    DialogWindow(
        title = "Güncellemeleri Kontrol Et", // "检查更新" -> "Güncellemeleri Kontrol Et"
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 550.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            var detecting by remember { mutableStateOf(true) }
            var downloadable by remember { mutableStateOf(latestVersion.isNotEmpty()) }
            var body by remember { mutableStateOf("") }
            var releaseTagName by remember { mutableStateOf("") }

            suspend fun detectingUpdates(version: String) {
                val client = HttpClient()
                val url = "https://api.github.com/repos/tangshimin/mujing/releases/latest"
                val headerName = "Accept"
                val headerValue = "application/vnd.github.v3+json"

                try {
                    val response: HttpResponse = client.get(url) {
                        header(headerName, headerValue)
                    }

                    detecting = false
                    when (response.status) {
                        HttpStatusCode.OK -> {
                            val string = response.bodyAsText()
                            val format = Json { ignoreUnknownKeys = true }
                            val releases = format.decodeFromString<GitHubRelease>(string)
                            val releaseVersion = ComparableVersion(releases.tag_name)
                            val currentVersion = ComparableVersion(version)
                            body = if (releaseVersion > currentVersion) {
                                downloadable = true
                                releaseTagName = releases.tag_name
                                var releaseContent = "En son sürüm: ${releases.tag_name}\n" // "最新版本：" -> "En son sürüm:"
                                val contentBody = releases.body
                                if (contentBody != null) {
                                    val end = contentBody.indexOf("---")
                                    if (end != -1) {
                                        releaseContent += contentBody.substring(0, end)
                                    }
                                }
                                releaseContent
                            } else {
                                downloadable = false
                                "Kullanılabilir güncelleme yok." // "没有可用更新" -> "Kullanılabilir güncelleme yok."
                            }
                        }
                        HttpStatusCode.NotFound -> {
                            body = "Web sayfası bulunamadı." // "网页没找到" -> "Web sayfası bulunamadı."
                        }
                        HttpStatusCode.InternalServerError -> {
                            body = "Sunucu hatası." // "服务器错误" -> "Sunucu hatası."
                        }
                        else -> {
                            body = "Bilinmeyen bir hata oluştu." // "未知错误" -> "Bilinmeyen bir hata oluştu."
                        }
                    }
                } catch (exception: Exception) {
                    detecting = false
                    body = exception.toString()
                }
            }

            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                scope.launch(Dispatchers.IO){
                    if(latestVersion.isEmpty()){
                        delay(500)
                        detectingUpdates(version)
                    }

                }
            }

            Box{
                val stateVertical = rememberScrollState(0)
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mevcut Sürüm: $version") // "当前版本：" -> "Mevcut Sürüm:"
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Güncellemeleri otomatik kontrol et") // "自动检查更新" -> "Güncellemeleri otomatik kontrol et"
                        Checkbox(
                            checked = autoUpdate,
                            onCheckedChange = { setAutoUpdate(it) }
                        )
                    }
                    if (latestVersion.isEmpty() && detecting) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Box(Modifier.width(50.dp).height(50.dp)) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Text("Kontrol ediliyor...") // "正在检查" -> "Kontrol ediliyor..."
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp,top = 10.dp,end = 20.dp)
                    ) {
                        if(latestVersion.isNotEmpty()){
                            val note = "En son sürüm: $latestVersion\n$releaseNote" // "最新版本：" -> "En son sürüm:"
                            Text(text = note)
                        }else{
                            // body içerisindeki "网页没找到", "服务器错误", "未知错误", "没有可用更新" gibi metinler detectingUpdates fonksiyonunda çevrilecek
                            Text(body)
                        }
                    }

                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    OutlinedButton(onClick = { close() }) {
                        Text("Kapat") // "关闭" -> "Kapat"
                    }
                    Spacer(Modifier.width(20.dp))
                    val uriHandler = LocalUriHandler.current
                    val latest = "https://github.com/tangshimin/mujing/releases"
                    OutlinedButton(
                        onClick = {
                            uriHandler.openUri(latest)
                            close()
                        },
                        enabled = downloadable
                    ) {
                        Text("Son Sürümü İndir") // "下载最新版" -> "Son Sürümü İndir"
                    }
                    Spacer(Modifier.width(20.dp))
                    val ignoreEnable = latestVersion.isNotEmpty() || releaseTagName.isNotEmpty()
                    OutlinedButton(
                        enabled = ignoreEnable,
                        onClick = {
                        if(latestVersion.isNotEmpty()){
                            ignore(latestVersion)
                        }else{
                            ignore(releaseTagName)
                        }
                            close()
                        }) {
                        Text("Yoksay") // "忽略" -> "Yoksay"
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

/**
 * 自动检查更新
 */
@OptIn(ExperimentalSerializationApi::class)
suspend fun autoDetectingUpdates(version: String): Triple<Boolean, String, String> {
    val client = HttpClient()
    val url = "https://api.github.com/repos/tangshimin/mujing/releases/latest"
    val headerName = "Accept"
    val headerValue = "application/vnd.github.v3+json"

    try {
        val response: HttpResponse = client.get(url) {
            header(headerName, headerValue)
        }

        if (response.status == HttpStatusCode.OK) {
            val string = response.bodyAsText()
            val format = Json { ignoreUnknownKeys = true }
            val releases = format.decodeFromString<GitHubRelease>(string)
            val releaseVersion = ComparableVersion(releases.tag_name)
            val currentVersion = ComparableVersion(version)
            if (releaseVersion > currentVersion) {
                var note = ""
                val body = releases.body
                if (body != null) {
                    val end = body.indexOf("---")
                    if (end != -1) {
                        note += body.substring(0, end)
                    }
                }
                return Triple(true, releases.tag_name, note)
            }
        }
    } catch (exception: Exception) {
        exception.printStackTrace()
        return Triple(false, "", "")
    }
    return Triple(false, "", "")
}
