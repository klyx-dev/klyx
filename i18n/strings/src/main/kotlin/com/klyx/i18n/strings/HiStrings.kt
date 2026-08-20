package com.klyx.i18n.strings

import com.klyx.i18n.I18nStrings

@I18nStrings(languageTag = "hi")
object HiStrings : Strings {
    override val simple = "नमस्ते!"

    override val projectOpened = "प्रोजेक्ट खोला गया"
    override val invalidPath = { path: String -> "अमान्य पथ: $path मौजूद नहीं है" }
    override val settingsExported = "सेटिंग्स निर्यात की गईं"
    override val exportFailed = { message: String? -> "निर्यात विफल: ${message ?: "अज्ञात त्रुटि"}" }
    override val settingsImported = "सेटिंग्स आयात और लागू की गईं"
    override val importFailedCouldNotRead = "आयात विफल: चयनित फ़ाइल नहीं पढ़ी जा सकी"
    override val importFailedInvalidSettings = "आयात विफल: चयनित फ़ाइल में मान्य सेटिंग्स नहीं हैं"
    override val terminalEnvWiped = "टर्मिनल वातावरण मिटाया गया"
    override val bootstrapInstalledFromAssets = "एसेट्स से बूटस्ट्रैप इंस्टॉल किया गया"
    override val installationFailed = { message: String? -> "इंस्टॉलेशन विफल: ${message ?: "अज्ञात त्रुटि"}" }
    override val noLogsToCopy = "कॉपी करने के लिए कोई लॉग नहीं"
    override val logsCopiedToClipboard = "लॉग क्लिपबोर्ड पर कॉपी किए गए"
    override val noLogsToShare = "साझा करने के लिए कोई लॉग नहीं"
    override val bootstrapUpdated = "बूटस्ट्रैप सफलतापूर्वक अपडेट किया गया"
    override val updateFailed = { message: String? -> "अपडेट विफल: ${message ?: "अज्ञात त्रुटि"}" }
    override val bootstrapInstalled = "बूटस्ट्रैप सफलतापूर्वक इंस्टॉल किया गया"
    override val noTextSelectedToShare = "साझा करने के लिए कोई टेक्स्ट चयनित नहीं है"

    override val screenNotRegistered = { id: String ->
        "स्क्रीन \"$id\" पंजीकृत नहीं है।\n\n" +
                "यदि आप एक प्लगइन डेवलपर हैं, तो सुनिश्चित करें कि\n" +
                "स्क्रीन पर नेविगेट करने से पहले screens.register()\n" +
                "के माध्यम से स्क्रीन पंजीकृत करें।"
    }
    override val somethingWentWrong = { name: String? ->
        "ओह! कुछ गलत हो गया।\nयह स्क्रीन अभी उपलब्ध नहीं है।\n\n($name)"
    }
    override val pluginCrashed = { id: String ->
        "स्क्रीन \"$id\" उपलब्ध नहीं है क्योंकि प्लगइन क्रैश हो गया।\n" +
                "कृपया इसे अनलोड या पुनः इंस्टॉल करने के लिए प्लगइन सेटिंग्स खोलें।"
    }

    override val developerOptions = "डेवलपर विकल्प"
    override val back = "वापस"
    override val terminalTesting = "टर्मिनल परीक्षण"
    override val wipeTerminalEnv = "टर्मिनल वातावरण मिटाएँ"
    override val wipeTerminalEnvDesc = "पुनः इंस्टॉल के लिए प्रीफ़िक्स और संस्करण फ़ाइल हटाता है"
    override val logging = "लॉगिंग"
    override val viewAppLogs = "ऐप लॉग देखें"
    override val viewAppLogsDesc = "प्लगइन्स और सिस्टम सेवाओं से इन-ऐप लॉग ब्राउज़ करें"
    override val backupAndRestore = "बैकअप और पुनर्स्थापना"
    override val exportSettings = "सेटिंग्स निर्यात करें"
    override val exportSettingsDesc = "सभी सेटिंग्स (रूप, संपादक, टर्मिनल, फ़ाइल ट्री) को JSON फ़ाइल में सहेजें"
    override val importSettings = "सेटिंग्स आयात करें"
    override val importSettingsDesc = "पहले निर्यात की गई JSON फ़ाइल से सेटिंग्स पुनर्स्थापित करें"
    override val debugTesting = "डिबग परीक्षण"
    override val installBootstrapFromAssets = "एसेट्स से बूटस्ट्रैप इंस्टॉल करें"
    override val installBootstrapFromAssetsDesc = "APK एसेट्स से बूटस्ट्रैप बाइनरी मिटाता और निकालता है"
    override val installFromAssetsQuestion = "एसेट्स से इंस्टॉल करें?"
    override val installFromAssetsDesc = { assetName: String ->
        "यह वर्तमान टर्मिनल वातावरण को मिटा देगा और APK एसेट्स निर्देशिका से $assetName निकालेगा।\n\n" +
                "यह बंडल किए गए बूटस्ट्रैप संग्रह के परीक्षण के लिए है।"
    }
    override val cancel = "रद्द करें"
    override val install = "इंस्टॉल करें"
    override val installingBootstrap = "बूटस्ट्रैप इंस्टॉल हो रहा है"
    override val starting = "शुरू हो रहा है..."

    override val newFile = "नई फ़ाइल"
    override val newFolder = "नया फ़ोल्डर"
    override val rename = "नाम बदलें"
    override val copyPath = "पथ कॉपी करें"
    override val pasteHere = "यहाँ पेस्ट करें"
    override val delete = "हटाएँ"
    override val copy = "कॉपी करें"
    override val cut = "काटें"
    override val paste = "पेस्ट करें"
    override val deleteFile = "फ़ाइल हटाएँ"
    override val openWith = "इसके साथ खोलें"
    override val share = "साझा करें"
    override val shareFile = "फ़ाइल साझा करें"
    override val noAppToOpenFile = "इस फ़ाइल को खोलने के लिए कोई एप्लिकेशन नहीं मिला"
    override val couldNotOpenFile = { message: String? -> "फ़ाइल नहीं खोली जा सकी: ${message ?: "अज्ञात त्रुटि"}" }
    override val noAppToShareFile = "साझा करने के लिए कोई एप्लिकेशन उपलब्ध नहीं है"
    override val couldNotShareFile = { message: String? -> "फ़ाइल साझा नहीं की जा सकी: ${message ?: "अज्ञात त्रुटि"}" }
    override val internalStorage = "आंतरिक संग्रहण"
    override val appData = "ऐप डेटा"
    override val terminalHome = "टर्मिनल होम"
    override val options = "विकल्प"
    override val info = "जानकारी"
    override val name = "नाम"
    override val path = "पथ"
    override val size = "आकार"
    override val lastModified = "अंतिम बार संशोधित"
    override val permissions = "अनुमतियाँ"
    override val symbolicLink = "सांकेतिक लिंक"
    override val calculating = "गणना हो रही है..."
    override val filesAndFolders = { files: Int, folders: Int -> "$files फ़ाइलें, $folders फ़ोल्डर" }
    override val closeProject = "प्रोजेक्ट बंद करें"

    override val appLogs = "ऐप लॉग"
    override val copyLogs = "लॉग कॉपी करें"
    override val shareLogs = "लॉग साझा करें"
    override val clearLogs = "लॉग साफ़ करें"
    override val noMatchingLogs = "कोई मेल खाता लॉग नहीं"
    override val noLogsYet = "अभी तक कोई लॉग नहीं"
    override val all = "सभी"
    override val searchByTagOrMessage = "टैग या संदेश द्वारा खोजें..."
    override val clearSearch = "खोज साफ़ करें"
    override val shareLogsTitle = "लॉग साझा करें"
}
