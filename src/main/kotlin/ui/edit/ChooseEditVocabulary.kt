package ui.edit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.RecentItem
import player.isMacOS
import ui.wordscreen.rememberWordState
import java.io.File
import javax.swing.JOptionPane

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChooseEditVocabulary(
    close: () -> Unit,
    recentList: List<RecentItem>,
    removeRecentItem:(RecentItem) -> Unit,
    openEditVocabulary: (String) -> Unit,
    colors: Colors,
) {

    Window(
        title = "Düzenlenecek Kelime Listesini Seç", // "选择要编辑词库" -> "Düzenlenecek Kelime Listesini Seç"
        icon = painterResource("logo/logo.png"),
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
        ),
        onCloseRequest = close,
    ) {
        MaterialTheme(colors = colors) {
            Surface {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val wordState = rememberWordState()
                    if (recentList.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 10.dp)
                        ) { Text("Son Kullanılanlar") } // "最近词库" -> "Son Kullanılanlar"
                        Box(
                            Modifier.fillMaxWidth().height(400.dp).padding(10.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                        ) {
                            val stateVertical = rememberScrollState(0)
                            Column(Modifier.verticalScroll(stateVertical)) {
                                if(wordState.vocabularyName.isNotEmpty()){
                                    val name = when (wordState.vocabularyName) {
                                        "FamiliarVocabulary" -> {
                                            "Tanıdık Kelimeler" // "熟悉词库" -> "Tanıdık Kelimeler"
                                        }
                                        "HardVocabulary" -> {
                                            "Zor Kelimeler" // "困难词库" -> "Zor Kelimeler"
                                        }
                                        else -> {
                                            wordState.vocabularyName // Bu zaten JSON'dan Türkçe gelecek
                                        }
                                    }
                                    ListItem(
                                        text = {
                                            Text(
                                                name,
                                                color = MaterialTheme.colors.onBackground
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            if(wordState.vocabularyPath.isNotEmpty()){
                                                openEditVocabulary(wordState.vocabularyPath)
                                            }
                                        },
                                        trailing = {
                                            Text("Mevcut Kelime Listesi    ", color = MaterialTheme.colors.primary) // "当前词库    " -> "Mevcut Kelime Listesi    "
                                        }
                                    )
                                }

                                recentList.forEach { item ->
                                    if (wordState.vocabularyName != item.name) {
                                        ListItem(
                                            text = { Text(item.name, color = MaterialTheme.colors.onBackground) }, // item.name zaten Türkçe olacak
                                            modifier = Modifier.clickable {

                                                val recentFile = File(item.path)
                                                if (recentFile.exists()) {
                                                    openEditVocabulary(item.path)
                                                } else {
                                                    removeRecentItem(item)
                                                    JOptionPane.showMessageDialog(window, "Dosya yolu hatası:\n${item.path}") // "文件地址错误：\n" -> "Dosya yolu hatası:\n"
                                                }



                                            }
                                        )
                                    }

                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateVertical)
                            )
                        }
                    }
                    var showFilePicker by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showFilePicker = true }) {
                        Text(
                            text = "Kelime Listesi Seç", // "选择词库" -> "Kelime Listesi Seç"
                        )
                    }


                    val extensions = if (isMacOS()) listOf("public.json") else listOf("json")
                    FilePicker(
                        show = showFilePicker,
                        fileExtensions = extensions,
                        initialDirectory = ""
                    ) { pickFile ->
                        if (pickFile != null) {
                            openEditVocabulary(pickFile.path)
                        }

                        showFilePicker = false
                    }
                }

            }
        }


    }
}

