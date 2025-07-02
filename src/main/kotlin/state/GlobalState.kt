package state

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** Genel veri sınıfı */
@ExperimentalSerializationApi
@Serializable
data class GlobalData(
    val type: ScreenType = ScreenType.WORD,
    val isDarkTheme: Boolean = true,
    val isFollowSystemTheme: Boolean = false,
    val audioVolume: Float = 0.8F,
    val videoVolume: Float = 80F,
    val keystrokeVolume: Float = 0.75F,
    val isPlayKeystrokeSound: Boolean = false,
    val primaryColorValue: ULong = 18377412168996880384UL,
    val backgroundColorValue:ULong = 18446744069414584320UL,
    val onBackgroundColorValue:ULong = 18374686479671623680UL,
    val wordTextStyle: String = "H2",
    val detailTextStyle: String = "Body1",
    val letterSpacing: Float = 5F,
    val x:Float = 100F,
    val y:Float = 100F,
    val width:Float = 1030F,
    val height:Float = 862F,
    val placement:WindowPlacement = WindowPlacement.Maximized,
    val autoUpdate:Boolean = true,
    val ignoreVersion:String = "",
    val bnc:Int = 1000,
    val frq:Int = 1000,
    val maxSentenceLength:Int = 25,
)

/** Genel durumun kalıcı olması gereken kısmı */
@OptIn(ExperimentalSerializationApi::class)
class GlobalState(globalData: GlobalData) {
    /**
     * Alıştırma türü
     */
    var type by mutableStateOf(globalData.type)

    /**
     * Koyu mod mu
     */
    var isDarkTheme by mutableStateOf(globalData.isDarkTheme)

    /**
     * Sistem temasını takip etsin mi
     */
    var isFollowSystemTheme by mutableStateOf(globalData.isFollowSystemTheme)

    /**
     * Kelime telaffuzunun ses seviyesi
     */
    var audioVolume by mutableStateOf(globalData.audioVolume)

    /**
     * Video oynatmanın ses seviyesi
     */
    var videoVolume by mutableStateOf(globalData.videoVolume)

    /**
     * Tuş vuruşu ses efekti ses seviyesi
     */
    var keystrokeVolume by mutableStateOf(globalData.keystrokeVolume)

    /**
     * Tuş vuruşu ses efekti oynatılsın mı
     */
    var isPlayKeystrokeSound by mutableStateOf(globalData.isPlayKeystrokeSound)

    /**
     * Ana renk tonu, varsayılan olarak yeşil
     */
    var primaryColor by mutableStateOf(Color(globalData.primaryColorValue))

    /**
     * Açık tema için arka plan rengi
     */
    var backgroundColor by mutableStateOf(Color(globalData.backgroundColorValue))

    /**
     * Açık tema için arka plan rengi
     */
    var onBackgroundColor by mutableStateOf(Color(globalData.onBackgroundColorValue))

    /**
     * Kelimenin yazı tipi stili, kalıcı olması gerekir
     */
    var wordTextStyle by mutableStateOf(globalData.wordTextStyle)

    /**
     * Ayrıntılı bilgilerin yazı tipi stili, kalıcı olması gerekir
     */
    var detailTextStyle by mutableStateOf(globalData.detailTextStyle)

    /**
     * Kelimenin yazı tipi boyutu, kalıcı olması gerekmez
     */
    var wordFontSize by mutableStateOf(TextUnit.Unspecified)

    /**
     * Ayrıntılı bilgilerin yazı tipi boyutu, kalıcı olması gerekmez
     */
    var detailFontSize by mutableStateOf(TextUnit.Unspecified)

    /**
     *  Harf aralığı
     */
    var letterSpacing by mutableStateOf((globalData.letterSpacing).sp)

    /**
     * Ana pencerenin konumu
     */
    var position by mutableStateOf(WindowPosition(globalData.x.dp,globalData.y.dp))

    /**
     * Ana pencerenin boyutu
     */
    var size by mutableStateOf(DpSize(globalData.width.dp,globalData.height.dp))

    /**
     * Pencerenin ekranda nasıl yerleştirileceğini açıklar
     */
    var placement by mutableStateOf(globalData.placement)

    /**
     * Güncellemeleri otomatik kontrol et
     */
    var autoUpdate by mutableStateOf(globalData.autoUpdate)

    /**
     * Yoksayılan sürüm
     */
    var ignoreVersion by mutableStateOf(globalData.ignoreVersion)

    /**
     * En yaygın BNC kelime sıklığına sahip kelime sayısını filtrele, varsayılan 1000
     */
    var bncNum by mutableStateOf(globalData.bnc)

    /**
     * En yaygın COCA kelime sıklığına sahip kelime sayısını filtrele, varsayılan 1000
     */
    var frqNum by mutableStateOf(globalData.frq)

    /**
     * Kelimenin bulunduğu cümlenin maksimum kelime sayısı, varsayılan 25
     */
    var maxSentenceLength by mutableStateOf(globalData.maxSentenceLength)

}
@Composable
 fun computeFontSize(textStyle: String): TextUnit {
   return when(textStyle){
        "H1" ->{
            MaterialTheme.typography.h1.fontSize
        }
        "H2" ->{
            MaterialTheme.typography.h2.fontSize
        }
        "H3" ->{
            MaterialTheme.typography.h3.fontSize
        }
        "H4" ->{
            MaterialTheme.typography.h4.fontSize
        }
        "H5" ->{
            MaterialTheme.typography.h5.fontSize
        }
        "H6" ->{
            MaterialTheme.typography.h6.fontSize
        }
        "Subtitle1" ->{
            MaterialTheme.typography.subtitle1.fontSize
        }
        "Subtitle2" ->{
            MaterialTheme.typography.subtitle2.fontSize
        }
        "Body1" ->{
            MaterialTheme.typography.body1.fontSize
        }
        "Body2" ->{
            MaterialTheme.typography.body2.fontSize
        }
        "Caption" ->{
            MaterialTheme.typography.caption.fontSize
        }
        "Overline" ->{
            MaterialTheme.typography.overline.fontSize
        }
        else ->{ MaterialTheme.typography.h2.fontSize
        }

    }
}
