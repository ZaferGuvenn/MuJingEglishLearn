package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import data.loadMutableVocabulary
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Rule
import org.junit.Test
import player.rememberPlayerState
import state.GlobalData
import state.GlobalState
import state.rememberAppState
import ui.wordscreen.WordScreenData
import ui.wordscreen.WordScreenState
import java.io.File

class WordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Kelime Ezberleme Arayüzünü Test Et
     */
    @OptIn(ExperimentalFoundationApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalSerializationApi::class,
        ExperimentalTestApi::class
    )
    @Test
    fun `Test WordScreen`(){

        // Test ortamını ayarla
        composeTestRule.setContent {
            val appState = rememberAppState()
            // Genel durumu başlat
            appState.global = GlobalState(GlobalData())
            // Kelime ezberleme arayüzünün durumunu başlat
            val wordState = remember{ WordScreenState(WordScreenData()) }
            // Kelime dağarcığının yolunu ayarla
            wordState.vocabularyPath = File("src/test/resources/Vocabulary.json").absolutePath
            // Kelime dağarcığını yükle
            wordState.vocabulary = loadMutableVocabulary( wordState.vocabularyPath)
            // Kelime dağarcığının adını ayarla
            wordState.vocabularyName = "Vocabulary"

            App(
                appState =appState,
                wordState = wordState,
                playerState = rememberPlayerState()
            )
        }


        // Header'ın görünmesini bekle
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("Header"),10000)

        // İlk kelimenin dizinini test et
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("1/96")

        // İlk kelimeyi test et
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("the")
            .isDisplayed()

        composeTestRule.waitForIdle()
        // Fare hareketini simüle et, kelime değiştirme düğmesini etkinleştir
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("Word"))
                .performMouseInput { click() }
        }
        composeTestRule.waitForIdle()

        // NextButton'ın görünmesini bekle
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("NextButton"),10000)
        // NextButton düğmesini test et
        composeTestRule.onNode(hasTestTag("NextButton"))
            .assertExists()
            .isDisplayed()

        // İkinci kelimeye geç
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("NextButton"))
                .assertExists()
                .performMouseInput { click() }
        }
        composeTestRule.waitForIdle()

        // İkinci kelimenin görünmesini bekle
        composeTestRule.waitUntilExactlyOneExists (hasText("2/96"),10000)
        // İkinci kelimeyi test et
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("be")
            .isDisplayed()

        // İkinci kelimenin dizinini test et
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("2/96")
            .isDisplayed()


        // PreviousButton düğmesini test et
        composeTestRule.onNode(hasTestTag("PreviousButton"))
            .assertExists()
            .isDisplayed()


        // İlk kelimeye geri dön
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("PreviousButton"))
                .assertExists()
                .performMouseInput { click() }
        }
        composeTestRule.waitForIdle()

        // İlk kelimenin görünmesini bekle
        composeTestRule.waitUntilExactlyOneExists (hasText("1/96"),10000)
        // İlk kelimeyi test et
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("the")
            .isDisplayed()

        // İlk kelimenin dizinini test et
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("1/96")
            .isDisplayed()



        // Ayarlar düğmesini test et
        composeTestRule.onNode(hasTestTag("SettingsButton"))
            .assertExists()
            .isDisplayed()

        // Yan menüyü aç
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("SettingsButton"))
                .assertExists()
                .performClick()
        }
        composeTestRule.waitForIdle()

        // Yan menünün görünmesini bekle
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("WordScreenSidebar"),10000)
        // Yan menüyü test et
        composeTestRule.onNode(hasTestTag("WordScreenSidebar"))
            .assertExists()
            .isDisplayed()

        // Yan menünün içeriğini test et
        composeTestRule.onNode(hasText("Dikte Testi")).isDisplayed()
        composeTestRule.onNode(hasText("Bölüm Seç")).isDisplayed()
        composeTestRule.onNode(hasText("Kelimeyi Göster")).isDisplayed()
        composeTestRule.onNode(hasText("Fonetiği Göster")).isDisplayed()
        composeTestRule.onNode(hasText("Morfolojiyi Göster")).isDisplayed()
        composeTestRule.onNode(hasText("İngilizce Tanım")).isDisplayed()
        composeTestRule.onNode(hasText("Türkçe Tanım")).isDisplayed()
        composeTestRule.onNode(hasText("Örnek Cümleleri Göster")).isDisplayed()
        composeTestRule.onNode(hasText("Altyazıları Göster")).isDisplayed()
        composeTestRule.onNode(hasText("Tuş Vuruşu Ses Efekti")).isDisplayed()
        composeTestRule.onNode(hasText("İpucu Ses Efekti")).isDisplayed()
        composeTestRule.onNode(hasText("Otomatik Geçiş")).isDisplayed()
        composeTestRule.onNode(hasText("Harici Altyazılar")).isDisplayed()
        composeTestRule.onNode(hasText("Altyazıları Yaz")).isDisplayed()
        composeTestRule.onNode(hasText("Ses Kontrolü")).isDisplayed()
        composeTestRule.onNode(hasText("Telaffuz Ayarları")).isDisplayed()

        // Yan menüyü kapat
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("SettingsButton"))
                .performClick()
        }
        composeTestRule.waitForIdle()

        // Yan menünün kaybolmasını bekle
        composeTestRule.waitUntilDoesNotExist(hasTestTag("WordScreenSidebar"),10000)
    }
}
