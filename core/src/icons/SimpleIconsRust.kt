package com.klyx.core.icons

/*
# CC0 1.0 Universal

## Statement of Purpose

The laws of most jurisdictions throughout the world automatically confer exclusive Copyright and Related Rights (defined below) upon the creator and subsequent owner(s) (each and all, an “owner”) of an original work of authorship and/or a database (each, a “Work”).

Certain owners wish to permanently relinquish those rights to a Work for the purpose of contributing to a commons of creative, cultural and scientific works (“Commons”) that the public can reliably and without fear of later claims of infringement build upon, modify, incorporate in other works, reuse and redistribute as freely as possible in any form whatsoever and for any purposes, including without limitation commercial purposes. These owners may contribute to the Commons to promote the ideal of a free culture and the further production of creative, cultural and scientific works, or to gain reputation or greater distribution for their Work in part through the use and efforts of others.

For these and/or other purposes and motivations, and without any expectation of additional consideration or compensation, the person associating CC0 with a Work (the “Affirmer”), to the extent that he or she is an owner of Copyright and Related Rights in the Work, voluntarily elects to apply CC0 to the Work and publicly distribute the Work under its terms, with knowledge of his or her Copyright and Related Rights in the Work and the meaning and intended legal effect of CC0 on those rights.

1. Copyright and Related Rights. A Work made available under CC0 may be protected by copyright and related or neighboring rights (“Copyright and Related Rights”). Copyright and Related Rights include, but are not limited to, the following:
    1. the right to reproduce, adapt, distribute, perform, display, communicate, and translate a Work;
    2. moral rights retained by the original author(s) and/or performer(s);
    3. publicity and privacy rights pertaining to a person’s image or likeness depicted in a Work;
    4. rights protecting against unfair competition in regards to a Work, subject to the limitations in paragraph 4(i), below;
    5. rights protecting the extraction, dissemination, use and reuse of data in a Work;
    6. database rights (such as those arising under Directive 96/9/EC of the European Parliament and of the Council of 11 March 1996 on the legal protection of databases, and under any national implementation thereof, including any amended or successor version of such directive); and
    7. other similar, equivalent or corresponding rights throughout the world based on applicable law or treaty, and any national implementations thereof.

2. Waiver. To the greatest extent permitted by, but not in contravention of, applicable law, Affirmer hereby overtly, fully, permanently, irrevocably and unconditionally waives, abandons, and surrenders all of Affirmer’s Copyright and Related Rights and associated claims and causes of action, whether now known or unknown (including existing as well as future claims and causes of action), in the Work (i) in all territories worldwide, (ii) for the maximum duration provided by applicable law or treaty (including future time extensions), (iii) in any current or future medium and for any number of copies, and (iv) for any purpose whatsoever, including without limitation commercial, advertising or promotional purposes (the “Waiver”). Affirmer makes the Waiver for the benefit of each member of the public at large and to the detriment of Affirmer’s heirs and successors, fully intending that such Waiver shall not be subject to revocation, rescission, cancellation, termination, or any other legal or equitable action to disrupt the quiet enjoyment of the Work by the public as contemplated by Affirmer’s express Statement of Purpose.

3. Public License Fallback. Should any part of the Waiver for any reason be judged legally invalid or ineffective under applicable law, then the Waiver shall be preserved to the maximum extent permitted taking into account Affirmer’s express Statement of Purpose. In addition, to the extent the Waiver is so judged Affirmer hereby grants to each affected person a royalty-free, non transferable, non sublicensable, non exclusive, irrevocable and unconditional license to exercise Affirmer’s Copyright and Related Rights in the Work (i) in all territories worldwide, (ii) for the maximum duration provided by applicable law or treaty (including future time extensions), (iii) in any current or future medium and for any number of copies, and (iv) for any purpose whatsoever, including without limitation commercial, advertising or promotional purposes (the “License”). The License shall be deemed effective as of the date CC0 was applied by Affirmer to the Work. Should any part of the License for any reason be judged legally invalid or ineffective under applicable law, such partial invalidity or ineffectiveness shall not invalidate the remainder of the License, and in such case Affirmer hereby affirms that he or she will not (i) exercise any of his or her remaining Copyright and Related Rights in the Work or (ii) assert any associated claims and causes of action with respect to the Work, in either case contrary to Affirmer’s express Statement of Purpose.

4. Limitations and Disclaimers.
    1. No trademark or patent rights held by Affirmer are waived, abandoned, surrendered, licensed or otherwise affected by this document.
    2. Affirmer offers the Work as-is and makes no representations or warranties of any kind concerning the Work, express, implied, statutory or otherwise, including without limitation warranties of title, merchantability, fitness for a particular purpose, non infringement, or the absence of latent or other defects, accuracy, or the present or absence of errors, whether or not discoverable, all to the greatest extent permissible under applicable law.
    3. Affirmer disclaims responsibility for clearing rights of other persons that may apply to the Work or any use thereof, including without limitation any person’s Copyright and Related Rights in the Work. Further, Affirmer disclaims responsibility for obtaining any necessary consents, permissions or other rights required for any use of the Work.
    4. Affirmer understands and acknowledges that Creative Commons is not a party to this document and has no duty or obligation with respect to this CC0 or use of the Work.

For more information, please see <https://creativecommons.org/publicdomain/zero/1.0>.
*/
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SimpleIconsRust: ImageVector
    get() {
        if (_SimpleIconsRust != null) return _SimpleIconsRust!!

        _SimpleIconsRust = ImageVector.Builder(
            name = "rust",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
            }
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(23.8346f, 11.7033f)
                lineToRelative(-1.0073f, -0.6236f)
                arcToRelative(13.7268f, 13.7268f, 0f, false, false, -0.2936f, 0f)
                arcToRelative(0.8656f, -0.8069f, 0f, false, false, 0f, 0f)
                arcToRelative(-0.1154f, -0.578f, 0f, false, false, 0f, 8.4958f)
                arcToRelative(8.4958f, 0f, 0f, false, false, 0f, 0.6904f)
                arcToRelative(-0.9587f, 0f, 0.3462f, false, false, 0f, -0.2257f)
                arcToRelative(-0.5446f, 0f, -1.1663f, false, false, 9.3574f, 9.3574f)
                arcToRelative(0f, 0f, -0.1407f, false, false, 0.49f, -1.0761f)
                arcToRelative(0.3437f, 0.3437f, 0f, false, false, -0.3361f, 0.3486f)
                arcToRelative(0.3486f, 0f, 0f, false, false, 0f, -1.1845f)
                arcToRelative(0.0416f, 0f, 6.7444f, false, false, 0f, -0.1873f)
                arcToRelative(-0.2268f, 0f, 0.2723f, false, false, 0.3472f, 0.3472f)
                arcToRelative(0f, 0f, -0.417f, false, false, -1.1532f, 0.2724f)
                arcToRelative(14.0183f, 14.0183f, 0f, false, false, -0.1873f, 0f)
                arcToRelative(0.0415f, -1.1845f, 0f, false, false, 0f, 0f)
                arcToRelative(-0.49f, -0.328f, 0f, false, false, 0f, -0.0872f)
                arcToRelative(-0.0476f, -0.1742f, -0.0952f, false, false, 0f, -0.1903f)
                arcToRelative(-1.1673f, 0f, 0.3483f, false, false, 16.256f, 0.955f)
                lineToRelative(-0.9597f, 0.6905f)
                arcToRelative(8.4867f, 8.4867f, 0f, false, false, -0.086f, 0f)
                arcToRelative(-0.414f, -1.1066f, 0f, false, false, 0f, 0f)
                arcToRelative(-0.5781f, -0.1154f, 0f, false, false, 0f, 9.2936f)
                arcToRelative(9.2936f, 0f, 0f, false, false, 0f, 12.2946f)
                arcToRelative(0.1683f, 0f, 0.3462f, false, false, 0f, -0.5892f)
                arcToRelative(0f, 0f, -0.6236f, false, false, 13.7383f, 13.7383f)
                arcToRelative(0f, 0f, -0.2936f, false, false, 9.9803f, 0.3374f)
                arcToRelative(0.3462f, 0.3462f, 0f, false, false, 0.1154f, 0f)
                arcToRelative(-0.4141f, 1.1065f, 0f, false, false, -0.1903f, 0.0567f)
                arcToRelative(-0.2855f, 0.086f, 0f, false, false, 0f, 0.3483f)
                arcToRelative(0.3483f, 0f, 0f, false, false, 0f, 7.009f)
                arcToRelative(2.348f, 0f, 9.3574f, false, false, 0f, -0.2622f)
                arcToRelative(0.1407f, 0f, -1.0762f, false, false, 0.3462f, 0.3462f)
                arcToRelative(0f, 0f, -0.49f, false, false, 0.0416f, 1.1845f)
                arcToRelative(7.9826f, 7.9826f, 0f, false, false, 0.1873f, 0f)
                arcToRelative(3.8413f, 3.425f, 0f, false, false, 0f, 0f)
                arcToRelative(-0.4171f, 0.4171f, 0f, false, false, 0f, -0.0628f)
                arcToRelative(0.075f, -0.1255f, 0.1509f, false, false, 0f, -1.1845f)
                arcToRelative(-0.0415f, 0f, 0.3462f, false, false, 0f, -0.328f)
                arcToRelative(0.49f, 0f, 0.491f, false, false, 9.167f, 9.167f)
                arcToRelative(0f, 0f, -0.1407f, false, false, -1.1662f, 0.1894f)
                arcToRelative(0.3483f, 0.3483f, 0f, false, false, 0.5446f, 0f)
                arcToRelative(0.6904f, 0.9587f, 0f, false, false, 0f, 0f)
                arcToRelative(-0.087f, 0.2855f, 0f, false, false, 0f, 0.3483f)
                arcToRelative(0.3483f, 0f, 0f, false, false, 0f, 0.8656f)
                arcToRelative(0.807f, 0f, 9.2936f, false, false, 0f, -0.0283f)
                arcToRelative(0.2935f, 0f, -1.0073f, false, false, 0.3442f, 0.3442f)
                arcToRelative(0f, 0f, 0.5892f, false, false, 0.6236f, 0f)
                arcToRelative(0.008f, 0.0982f, 0.0182f, false, false, 0.2936f, 0f)
                arcToRelative(-0.8656f, 0.8079f, 0f, false, false, 0f, 0.1155f)
                arcToRelative(0.578f, 0f, 1.1065f, false, false, 0.0273f, 0.0962f)
                arcToRelative(0.0567f, 0.1914f, 0.087f, false, false, -0.6904f, 0.9587f)
                arcToRelative(0.3452f, 0.3452f, 0f, false, false, 0f, 1.1662f)
                arcToRelative(0.1893f, 0f, 0.0456f, false, false, 0.1751f, 0.1408f)
                arcToRelative(0.2622f, 0f, -0.491f, false, false, 0.3462f, 0.3462f)
                arcToRelative(0f, 0.328f, 0.49f, false, false, -0.0415f, 0f)
                arcToRelative(0.0618f, 0.0769f, 0.1235f, false, false, 0.2277f, 0f)
                arcToRelative(-0.2713f, 1.1541f, 0f, false, false, 0f, 0.4171f)
                arcToRelative(0.4161f, 0f, 1.153f, false, false, 0.075f, 0.0638f)
                arcToRelative(0.151f, 0.1255f, 0.2279f, false, false, -0.0415f, 1.1845f)
                arcToRelative(0.3442f, 0.3442f, 0f, false, false, 0f, 1.0761f)
                arcToRelative(-0.49f, 0f, 0.087f, false, false, 0.0951f, 0.2622f)
                arcToRelative(0.1407f, 0f, 0.1903f, false, false, 0.3483f, 0.3483f)
                arcToRelative(0f, 0.5447f, 0.2268f, false, false, -0.6904f, 0f)
                arcToRelative(9.299f, 9.299f, 0f, false, false, 0f, 0.414f)
                arcToRelative(1.1066f, 0f, 0.3452f, false, false, 0.5781f, 0.1154f)
                lineToRelative(0.8079f, -0.8656f)
                curveToRelative(0.0972f, 0.0111f, 0.1954f, 0.0203f, 0.2936f, 0.0294f)
                lineToRelative(0.6236f, 1.0073f)
                arcToRelative(0.3472f, 0.3472f, 0f, false, false, 0f, 0.6236f)
                arcToRelative(-1.0073f, 0f, 0.0982f, false, false, -0.0183f, 0.2936f)
                arcToRelative(-0.0294f, 0f, 0.8069f, false, false, 0.3483f, 0.3483f)
                arcToRelative(0f, 0.578f, -0.1154f, false, false, -1.1066f, 0f)
                arcToRelative(8.4626f, 8.4626f, 0f, false, false, 0f, 0.9587f)
                arcToRelative(0.6904f, 0f, 0.3452f, false, false, 0.5447f, -0.2268f)
                lineToRelative(0.1903f, -1.1662f)
                curveToRelative(0.088f, -0.0456f, 0.1751f, -0.0931f, 0.2622f, -0.1407f)
                lineToRelative(1.0762f, 0.49f)
                arcToRelative(0.3472f, 0.3472f, 0f, false, false, 0f, -0.0415f)
                arcToRelative(-1.1845f, 0f, 6.7267f, false, false, 0.2267f, -0.1863f)
                lineToRelative(1.1531f, 0.2713f)
                arcToRelative(0.3472f, 0.3472f, 0f, false, false, 0f, -0.2713f)
                arcToRelative(-1.1542f, 0f, 0.0628f, false, false, -0.1508f, 0.1863f)
                arcToRelative(-0.2278f, 0f, 1.1845f, false, false, 0.3442f, 0.3442f)
                arcToRelative(0f, 0.328f, -0.49f, false, false, -1.076f, 0f)
                arcToRelative(0.0475f, -0.0872f, 0.0951f, false, false, -0.2623f, 0f)
                arcToRelative(1.1662f, -0.1893f, 0f, false, false, 0f, 0.2258f)
                arcToRelative(-0.5447f, 0f, -0.6904f, false, false, -0.2855f, 1.1066f)
                arcToRelative(-0.414f, 0f, 0.3462f, false, false, 0.1154f, -0.5781f)
                lineToRelative(-0.8656f, -0.8079f)
                curveToRelative(0.0101f, -0.0972f, 0.0202f, -0.1954f, 0.0283f, -0.2936f)
                lineToRelative(1.0073f, -0.6236f)
                arcToRelative(0.3442f, 0.3442f, 0f, false, false, 0f, 0f)
                arcToRelative(-6.7413f, 8.3551f, 0f, false, false, 0f, 1.2986f)
                arcToRelative(-1.396f, 0.714f, 0.714f, false, false, -0.2997f, 1.396f)
                close()
                moveToRelative(-0.3422f, -2.3142f)
                arcToRelative(0.649f, 0.649f, 0f, false, false, 0.5f, 0f)
                arcToRelative(-0.3573f, 1.6685f, 0f, false, false, -2.3285f, 0.7795f)
                arcToRelative(-3.6193f, 0.7795f, 0f, false, false, 0f, 1f)
                arcToRelative(-3.6951f, -0.814f, 0f, false, false, 0f, 0.648f)
                arcToRelative(0.648f, 0f, 0f, false, false, 0f, -1.473f)
                arcToRelative(0.3158f, 0f, 8.7216f, false, false, 1f, -0.7613f)
                arcToRelative(-0.898f, 0f, 7.1676f, false, false, 0f, 0.1356f)
                arcToRelative(-0.0141f, 0.1356f, -0.088f, false, false, 0f, 0f)
                arcToRelative(-0.074f, -0.0536f, -0.0881f, false, false, 0f, -2.0966f)
                verticalLineToRelative(-1.6077f)
                horizontalLineToRelative(2.2677f)
                curveToRelative(0.2065f, 0f, 1.1065f, 0.0587f, 1.394f, 1.2088f)
                curveToRelative(0.0901f, 0.3533f, 0.2875f, 1.5044f, 0.4232f, 1.8729f)
                curveToRelative(0.1346f, 0.413f, 0.6833f, 1.2381f, 1.2685f, 1.2381f)
                horizontalLineToRelative(3.5716f)
                arcToRelative(0.7492f, 0.7492f, 0f, false, false, 8.7874f, 8.7874f)
                arcToRelative(0f, 1f, -0.8119f, false, false, 0f, 6.8369f)
                arcToRelative(20.024f, 0f, 0.714f, false, false, 11f, -0.2997f)
                arcToRelative(-1.396f, 0.714f, 0.714f, false, false, 1.396f, 0f)
                moveTo(4.1177f, 8.9972f)
                arcToRelative(0.7137f, 0.7137f, 0f, false, false, 0.5791f, 0.7137f)
                arcToRelative(0.7137f, 0f, 11.304f, false, false, 0f, -0.8352f)
                arcToRelative(1.9813f, 0f, 1.5347f, false, false, 0.65f, 0.65f)
                arcToRelative(0f, 0.33f, -0.8585f, false, false, -0.7147f, 0f)
                arcToRelative(1.2432f, 0f, 5.6025f, false, false, 0f, 8.7753f)
                arcToRelative(8.7753f, 0f, 1f, false, false, 0f, 0f)
                arcToRelative(6.7343f, -0.5437f, 0f, false, false, 2.9601f, 0f)
                arcToRelative(0.153f, 0f, 1.0792f, false, false, 0.8697f, 0f)
                arcToRelative(0.575f, -0.7107f, 0.7815f, false, false, 0f, 0f)
                arcToRelative(10.7574f, 1.4862f, 0f, false, false, -0.008f, 0.4363f)
                arcToRelative(-0.0243f, 0.651f, 0f, false, false, -0.09f, 0f)
                arcToRelative(-0.1265f, 0.0586f, -0.1265f, false, false, 0.413f, 0f)
                arcToRelative(0f, 0.973f, -0.5487f, false, false, 1.2382f, -0.4576f)
                arcToRelative(0.0517f, -0.9648f, -0.1913f, false, false, -0.2704f, -1.5186f)
                arcToRelative(-0.7198f, -1.8436f, -1.4305f, false, false, -0.5599f, 1.799f)
                arcToRelative(-1.386f, 1.799f, -2.4915f, false, false, -0.819f, -1.9458f)
                arcToRelative(-1.3769f, -2.3153f, -0.7825f, false, false, -0.6195f, -1.883f)
                arcToRelative(-0.6195f, 0f, 5.4682f, false, false, 8.7651f, 0f)
                arcToRelative(14.907f, -2.7699f, 0f, false, false, 0f, 0.648f)
                arcToRelative(0.648f, 0f, 0.9182f, false, false, 1.227f, -1.1743f)
                arcToRelative(8.7753f, 8.7753f, 0f, false, false, 0f, -0.8403f)
                arcToRelative(1.8982f, 0f, 0.652f, false, false, 0.33f, 0.8585f)
                lineToRelative(1.6178f, 0.7188f)
                curveToRelative(0.0283f, 0.2875f, 0.0425f, 0.577f, 0.0425f, 0.8717f)
                close()
                moveToRelative(-9.3006f, -9.5993f)
                arcToRelative(0.7128f, 0.7128f, 0f, false, false, 0.7137f, 0.7137f)
                arcToRelative(0f, 1f, -0.984f, false, false, 0f, 8.3389f)
                arcToRelative(6.71f, 0f, 0.7107f, false, false, 1.9395f, -0.3625f)
                arcToRelative(0.7137f, 0.7137f, 0f, false, false, 0.3635f, 0f)
            }
        }.build()

        return _SimpleIconsRust!!
    }

private var _SimpleIconsRust: ImageVector? = null
