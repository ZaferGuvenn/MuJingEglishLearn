#### Wix Eğitimi
- Resmi [WiX Araç Seti Eğitimi](https://www.firegiant.com/wix/tutorial/)
- Github'daki [WiX Yükleyici Örnekleri](https://github.com/kurtanr/WiXInstallerExamples)
- Cnblogs'daki Çince [Wix Kurulum ve Dağıtım Eğitimi](https://www.cnblogs.com/stoneniqiu/category/522235.html)

#### Shortcut (Kısayol) Elemanı
Wix resmi dokümantasyonu: [Shortcut Element](https://wixtoolset.org/documentation/manual/v3/xsd/wix/shortcut.html)
- Advertised shortcuts (Tanıtılan kısayollar), Windows Installer'ın bir özelliğidir ve geleneksel kısayollardan farklıdırlar. Tanıtılan kısayollar doğrudan uygulamanın çalıştırılabilir dosyasına değil, Windows Installer bileşenine işaret eder. Kullanıcı, tanıtılan bir kısayol aracılığıyla uygulamayı başlattığında, Windows Installer önce uygulamanın kurulum durumunu kontrol eder ve gerekirse kurulumu otomatik olarak onarır veya tamamlar.

  Yazılım kaldırıldığında, tanıtılan kısayollar otomatik olarak silinir.

- Non-Advertised shortcuts (Normal kısayollar) için sağ tıklama menüsündeki "Dosya konumunu aç" seçeneği kullanılarak hedef klasör açılabilir.
  Yazılım kaldırıldığında, normal kısayollar otomatik olarak silinmez.
```xml
            <File Id="Learna.exe" KeyPath="yes" Source="$(var.SourceDir)\Learna.exe">
              <Shortcut Advertise="yes" Directory="DesktopFolder" Icon="icon.ico" IconIndex="0" Id="DesktopShortcut" Name="Learna" WorkingDirectory="INSTALLDIR"/>
              <Shortcut Advertise="yes" Directory="ProgramMenuDir" Icon="icon.ico" IconIndex="0" Id="startMenuShortcut" Name="Learna" WorkingDirectory="INSTALLDIR"/>
            </File>
```
  Masaüstü Kısayolunu Silme Yöntemi:
```xml
  <Directory Id="DesktopFolder" Name="Desktop">
    <Component Id="DeleteDesktopShortcut" Guid="{CAC2B592-1B6D-4439-AA42-D6DBFAB4D302}" Win64="yes">
      <Shortcut Advertise="no" Directory="DesktopFolder" Target="[INSTALLDIR]Learna.exe" Icon="icon.ico" IconIndex="0"
                Id="DesktopShortcut" Name="Learna" WorkingDirectory="INSTALLDIR"/>
      <RemoveFile Id="DesktopShortcut" On="uninstall" Name="Learna.lnk" Directory="DesktopFolder"/>
      <RegistryValue Id="DesktopShortcutReg" Key="Software\Learna" KeyPath="yes" Name="ProductCode" Root="HKCU"
                     Type="string" Value="[ProductCode]"/>
    </Component>
  </Directory>
```
  Başlat Menüsü Kısayolunu Silme Yöntemi:
```xml
<Directory Id="ProgramMenuDir" Name="Learna">
    <Component Guid="{A37549DB-C288-3FE3-B8E5-8530D25A07B5}" Id="ProgramMenuDirComponent" Win64="yes">
        <Shortcut Advertise="no" Directory="ProgramMenuDir" Target = "[INSTALLDIR]Learna.exe" Icon="icon.ico" IconIndex="0" Id="startMenuShortcut" Name="Learna" WorkingDirectory="INSTALLDIR"/>
        <RemoveFolder Directory="ProgramMenuDir" Id="CleanUpShortCutDir" On="uninstall"/>
        <RegistryValue Id="ProgramMenuShortcutReg" Key="Software\Learna" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
    </Component>
    <Component Guid="{A37549DB-C288-3FE3-B8E5-8530D25A08B5}" Id="RemoveShortcutComponent" Win64="yes">
        <RemoveFile Id="RemoveMenuShortcut" On="uninstall" Name="Learna.lnk" Directory="ProgramMenuDir"/>
        <RegistryValue Id="RemoveMenuShortcutReg" Key="Software\Learna" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
    </Component>
</Directory>
```

#### KeyPath
Resmi dokümantasyondaki açıklama:
The KeyPath attribute is set to yes to tell the Windows Installer that this particular file should be used to determine whether the component is installed. If you do not set the KeyPath attribute, WiX will look at the child elements under the component in sequential order and try to automatically select one of them as a key path. Allowing WiX to automatically select a key path can be dangerous because adding or removing child elements under the component can inadvertantly cause the key path to change, which can lead to installation problems. In general, you should always set the KeyPath attribute to yes to ensure that the key path will not inadvertantly change if you update your setup authoring in the future.

KeyPath, bir bileşenin kurulu olup olmadığını belirlemek için Windows Installer'a bu belirli dosyanın kullanılması gerektiğini bildirmek üzere 'yes' olarak ayarlanır. KeyPath özniteliğini ayarlamazsanız, WiX bileşen altındaki alt öğelere sırayla bakar ve bunlardan birini otomatik olarak anahtar yol olarak seçmeye çalışır. WiX'in otomatik olarak bir anahtar yol seçmesine izin vermek tehlikeli olabilir, çünkü bileşen altındaki alt öğeleri eklemek veya kaldırmak, anahtar yolun yanlışlıkla değişmesine neden olabilir, bu da kurulum sorunlarına yol açabilir. Genel olarak, gelecekte kurulum yazarlığınızı güncellerseniz anahtar yolun yanlışlıkla değişmemesini sağlamak için KeyPath özniteliğini her zaman 'yes' olarak ayarlamanız gerekir.

KeyPath, Windows Installer'da bileşenleri tanımlamak için kullanılan kritik bir yoldur. Bileşenin benzersizliğini ve bütünlüğünü sağlamak için kullanılır. Her bileşenin, genellikle bir dosya veya kayıt defteri anahtarı olan benzersiz bir KeyPath'i olmalıdır. Kullanıcı profiline yüklenen bileşenler için KeyPath, bir dosya değil, bir kayıt defteri anahtarı olmalıdır.

#### RegistryValue
Windows Installer'da, birden fazla RegistryValue öğesi aynı Name ve Value özniteliklerine sahipse ancak farklı bileşenlere veya yollara aitse sorun olmaz. Ancak, aynı bileşen içindelerse çakışmaya veya üzerine yazmaya neden olabilirler.
