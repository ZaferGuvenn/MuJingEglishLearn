package ui.wordscreen

enum class MemoryStrategy {
    /** Normal kelime ezberleme, kelimeleri birden çok kez heceleyebilir, videoları oynatabilir, altyazıları kopyalayabilir, tüm bilgileri görüntüleyebilir. */
    Normal,

    /** Normal kelime ezberleme sırasında bir ünite kelime ezberledikten sonra yapılan dikte testi. */
    Dictation,

    /** Normal kelime ezberleme sırasında yanlış dikte edilen kelimelerin gözden geçirilmesi. */
    NormalReviewWrong ,

    /** Bağımsız dikte testi, birden fazla bölüm seçilebilir. Yan menüden açın. */
    DictationTest,

    /** Bağımsız dikte testinden sonra yanlış kelimelerin gözden geçirilmesi. */
    DictationTestReviewWrong
}