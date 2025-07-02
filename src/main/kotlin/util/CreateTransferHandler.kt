package util

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import javax.swing.TransferHandler

/** Sürükle ve bırak işleyicisi oluştur
 * @param singleFile yalnızca tek bir dosya mı kabul edilsin
 * @param parseImportFile içe aktarılan dosyaları işleme fonksiyonu
 * @param showWrongMessage ipucu mesajını gösterme fonksiyonu
 */
fun createTransferHandler(
    singleFile: Boolean = true,
    parseImportFile: (List<File>) -> Unit,
    showWrongMessage: (String) -> Unit,
): TransferHandler {
    return object : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val transferable = support.transferable
            try {
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                if (singleFile) {
                    if (files.size == 1) {
                        parseImportFile(files)
                    } else {
                        showWrongMessage("Aynı anda yalnızca bir dosya okunabilir")
                    }
                } else {
                    parseImportFile(files)
                }


            } catch (exception: UnsupportedFlavorException) {
                return false
            } catch (exception: IOException) {
                return false
            }
            return true
        }
    }
}