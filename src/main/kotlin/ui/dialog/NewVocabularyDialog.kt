package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import data.Vocabulary
import data.VocabularyType
import data.saveVocabulary
import ui.window.windowBackgroundFlashingOnCloseFixHack
import java.io.File
import javax.swing.JOptionPane

@Composable
fun NewVocabularyDialog(
    close: () -> Unit,
    setEditPath: (String) -> Unit,
    colors: Colors,
) {

    DialogWindow(
        title = "Yeni Kelime Listesi", // "新建词库" -> "Yeni Kelime Listesi"
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(610.dp, 320.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        MaterialTheme(colors = colors) {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {

                var name by remember { mutableStateOf("") }
                var vocabularyDir by remember { mutableStateOf("") }
                var path by remember { mutableStateOf("") }

                Column (Modifier.fillMaxSize().padding(20.dp)){
                    val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)){
                        Text("Kelime Listesi Adı: ") // "词库名称：" -> "Kelime Listesi Adı: "
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                color = MaterialTheme.colors.onBackground
                            ),
                            modifier = Modifier
                                .width(270.dp)
                                .border(border = border)
                                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()){
                        Text("Kaydedilecek Konum: ") // "词库位置：" -> "Kaydedilecek Konum: "
                        BasicTextField(
                            value = vocabularyDir,
                            onValueChange = { vocabularyDir = it },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                color = MaterialTheme.colors.onBackground
                            ),
                            modifier = Modifier
                                .width(400.dp)
                                .border(border = border)
                                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
                        )
                        var showFileChooser by remember { mutableStateOf(false) }
                        IconButton(onClick = { showFileChooser = true }) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = "Dizin Aç", // "open directory" -> "Dizin Aç"
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        if(showFileChooser){
                            DirectoryPicker(
                                show = showFileChooser,
                                initialDirectory = ""){path ->
                                if(path != null){
                                    if(path.isNotEmpty()){
                                        vocabularyDir = path
                                    }
                                }
                                showFileChooser = false
                            }
                        }
                    }
                    LaunchedEffect(vocabularyDir){
                        if(vocabularyDir.isNotEmpty()){
                            path = "$vocabularyDir${File.separator}$name.json"
                        }
                    }
                    Row{
                        Text("Kelime listesi şuraya kaydedilecek: $path") // "词库将会保存到：" -> "Kelime listesi şuraya kaydedilecek: "
                    }

                    Row(horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(top =60.dp)){
                        OutlinedButton(
                            enabled = name.isNotEmpty() && vocabularyDir.isNotEmpty(),
                            onClick = {
                                val vocabulary = Vocabulary(
                                    name = name,
                                    type = VocabularyType.DOCUMENT,
                                    language = "", // Dil "english" veya "turkish" olarak ayarlanabilir, şimdilik boş bırakıyorum.
                                    size = 0,
                                    relateVideoPath = "",
                                    subtitlesTrackId = 0,
                                    wordList = mutableListOf()
                                )
                                try{
                                    saveVocabulary(vocabulary, path)
                                    setEditPath(path)
                                    close()
                                }catch(e:Exception){
                                    e.printStackTrace()
                                    JOptionPane.showMessageDialog(window, "Kelime listesi kaydedilemedi. Hata:\n${e.message}") // "保存词库失败,错误信息：\n${e.message}"
                                }

                            },
                            modifier = Modifier
                        ) {
                            Text("Onayla") // "确定" -> "Onayla"
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        OutlinedButton(
                            onClick = { close() },
                            modifier = Modifier
                        ) {
                            Text("İptal") // "取消" -> "İptal"
                        }
                    }

                }

            }
        }
    }
}
