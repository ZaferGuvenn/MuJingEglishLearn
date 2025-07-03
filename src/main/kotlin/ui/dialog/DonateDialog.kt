package ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ui.window.windowBackgroundFlashingOnCloseFixHack

@Composable
fun DonateDialog(close: () -> Unit) {
    DialogWindow(
        title = "Bağış Yap", // "捐赠" -> "Bağış Yap"
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(645.dp, 650.dp)
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
                TabRow(
                    selectedTabIndex = state,
                    backgroundColor = Color.Transparent
                ) {
                    Tab(
                        text = { Text("WeChat Pay") }, // "微信支付" -> "WeChat Pay" (Özel isim olduğu için orijinal bırakıldı)
                        selected = state == 0,
                        onClick = { state = 0 }
                    )
                    Tab(
                        text = { Text("Alipay") }, // "支付宝" -> "Alipay" (Özel isim olduğu için orijinal bırakıldı)
                        selected = state == 1,
                        onClick = { state = 1 }
                    )
                }

                when (state) {
                    0 -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 75.dp)
                        ) {
                            Image(
                                painter = painterResource("donate/WeChat Payment.png"),
                                contentDescription = "donate",
                                modifier = Modifier.width(400.dp).height(400.dp)
                            )
                        }
                    }
                    1 -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 75.dp)
                        ) {
                            Image(
                                painter = painterResource("donate/Alipay.png"),
                                contentDescription = "donate",
                                modifier = Modifier.width(400.dp).height(400.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}