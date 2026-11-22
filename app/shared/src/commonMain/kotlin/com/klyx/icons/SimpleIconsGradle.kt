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

val SimpleIconsGradle: ImageVector
    get() {
        if (_SimpleIconsGradle != null) return _SimpleIconsGradle!!

        _SimpleIconsGradle = ImageVector.Builder(
            name = "gradle",
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
                moveTo(22.695f, 4.297f)
                arcToRelative(3.807f, 3.807f, 0f, false, false, -5.29f, -0.09f)
                arcToRelative(0.368f, 0.368f, 0f, false, false, 0f, 0.533f)
                lineToRelative(0.46f, 0.47f)
                arcToRelative(0.363f, 0.363f, 0f, false, false, 0.474f, 0.032f)
                arcToRelative(2.182f, 2.182f, 0f, false, true, 2.86f, 3.291f)
                curveToRelative(-3.023f, 3.02f, -7.056f, -5.447f, -16.211f, -1.083f)
                arcToRelative(1.24f, 1.24f, 0f, false, false, -0.534f, 1.745f)
                lineToRelative(1.571f, 2.713f)
                arcToRelative(1.238f, 1.238f, 0f, false, false, 1.681f, 0.461f)
                lineToRelative(0.037f, -0.02f)
                lineToRelative(-0.029f, 0.02f)
                lineToRelative(0.688f, -0.384f)
                arcToRelative(16.083f, 16.083f, 0f, false, false, 2.193f, -1.635f)
                arcToRelative(0.384f, 0.384f, 0f, false, true, 0.499f, -0.016f)
                arcToRelative(0.357f, 0.357f, 0f, false, true, 0.016f, 0.534f)
                arcToRelative(16.435f, 16.435f, 0f, false, true, -2.316f, 1.741f)
                horizontalLineTo(8.77f)
                lineToRelative(-0.696f, 0.39f)
                arcToRelative(1.958f, 1.958f, 0f, false, true, -0.963f, 0.25f)
                arcToRelative(1.987f, 1.987f, 0f, false, true, -1.726f, -0.989f)
                lineTo(3.9f, 9.696f)
                curveTo(1.06f, 11.72f, -0.686f, 15.603f, 0.26f, 20.522f)
                arcToRelative(0.363f, 0.363f, 0f, false, false, 0.354f, 0.296f)
                horizontalLineToRelative(1.675f)
                arcToRelative(0.363f, 0.363f, 0f, false, false, 0.37f, -0.331f)
                arcToRelative(2.478f, 2.478f, 0f, false, true, 4.915f, 0f)
                arcToRelative(0.36f, 0.36f, 0f, false, false, 0.357f, 0.317f)
                horizontalLineToRelative(1.638f)
                arcToRelative(0.363f, 0.363f, 0f, false, false, 0.357f, -0.317f)
                arcToRelative(2.478f, 2.478f, 0f, false, true, 4.914f, 0f)
                arcToRelative(0.363f, 0.363f, 0f, false, false, 0.358f, 0.317f)
                horizontalLineToRelative(1.627f)
                arcToRelative(0.363f, 0.363f, 0f, false, false, 0.363f, -0.357f)
                curveToRelative(0.037f, -2.294f, 0.656f, -4.93f, 2.42f, -6.25f)
                curveToRelative(6.108f, -4.57f, 4.502f, -8.486f, 3.088f, -9.9f)
                close()
                moveToRelative(-6.229f, 6.901f)
                lineToRelative(-1.165f, -0.584f)
                arcToRelative(0.73f, 0.73f, 0f, true, true, 1.165f, 0.587f)
                close()
            }
        }.build()

        return _SimpleIconsGradle!!
    }

private var _SimpleIconsGradle: ImageVector? = null
