package com.klyx.i18n.strings

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StringsFallbackTest : FunSpec({

    val partial = object : Strings {
        override val simple = "नमस्ते!"
    }

    test("translated string is used") {
        partial.simple shouldBe "नमस्ते!"
    }

    test("untranslated strings fall back to the default (English)") {
        partial.back shouldBe EnStrings.back
        partial.cancel shouldBe "Cancel"
    }

    test("untranslated parameterized strings fall back to the default (English)") {
        partial.invalidPath("/tmp/x") shouldBe EnStrings.invalidPath("/tmp/x")
        partial.filesAndFolders(1, 2) shouldBe "1 files, 2 folders"
    }

    test("generated language objects come from the JSON files") {
        HiStrings.simple shouldBe "नमस्ते!"
        HiStrings.filesAndFolders(2, 3) shouldBe "2 फ़ाइलें, 3 फ़ोल्डर"
    }

    test("nullable placeholder fallbacks survive generation") {
        EnStrings.couldNotOpenFile(null) shouldBe "Could not open file: Unknown error"
        HiStrings.exportFailed(null) shouldBe "निर्यात विफल: अज्ञात त्रुटि"
    }
})
