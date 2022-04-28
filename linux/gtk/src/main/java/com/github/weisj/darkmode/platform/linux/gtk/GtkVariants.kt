/*
 * MIT License
 *
 * Copyright (c) 2020 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.github.weisj.darkmode.platform.linux.gtk

import java.util.EnumMap

/**
 * This class is effectively a conversion of the GtkVariants.js module seen [here](https://gitlab.com/rmnvgr/nightthemeswitcher-gnome-shell-extension/-/blob/0d32907de1d004a6a92f02b8e0b79302340e5244/src/modules/GtkVariants.js).
 *
 * The logic of guessFrom will be used if the user chooses to have the plugin guess which of their themes is their
 * light and dark theme (guessing is enabled by default).
 * @see GtkSettings.guessLightAndDarkThemes
 */
object GtkVariants {
    /**
     * This documentation is straight from the file mentioned in the [GtkVariants] JavaDoc.
     * The magic of guessing theme variants happens here.
     *
     * If the theme doesn't fit a particular case, we'll do the following:
     * - Remove any signs of a dark variant to the theme name to get the day
     * variant
     * - Remove any signs of a light variant to the day variant and add '-dark' to
     * get the night variant
     *
     * For themes that don't work with the general rule, a particular case must be written. Day and night variants
     * should be guessed with the most generic light and dark variants the theme offer, except if the user explicitly
     * chose a specific variant.
     *
     * Light variants, from the most to the least generic:
     * - ''
     * - '-light'
     * - '-darker'
     *
     * Dark variants, from the most the least generic:
     * - '-dark'
     * - '-darkest'
     *
     * @param themeName the name of a theme. This could be one of the many variants of your theme (i.e. the dark
     * version or whatever other variants the theme offers)
     * @return a map containing three keys: [Variant.Original] (the provided themeName), [Variant.Day] (the guessed day
     * variant), and [Variant.Night] (the guessed night variant)
     */
    @Suppress("kotlin:S1192")
    @JvmStatic
    fun guessFrom(themeName: String): Map<Variant, String> {
        val variants: MutableMap<Variant, String> = EnumMap(Variant::class.java)
        variants[Variant.Original] = themeName
        when {
            themeName.contains("Adapta") -> {
                variants[Variant.Day] = themeName.replace("-Nokto", "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace("Adapta", "Adapta-Nokto")
            }
            themeName.contains("Arc") -> {
                variants[Variant.Day] = themeName.replace(Regex("-Dark(?!er)"), "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace(Regex("Arc(-Darker)?"), "Arc-Dark")
            }
            themeName.matches(Regex(pattern = "Cabinet")) -> {
                variants[Variant.Day] = themeName.replace(Regex("-Dark(?!er)"), "-Light")
                variants[Variant.Night] = variants[Variant.Day]!!.replace(Regex("(-Light|-Darker)"), "-Dark")
            }
            themeName.matches(Regex(pattern = "^(Canta|ChromeOS|Materia|Orchis).*-compact")) -> {
                variants[Variant.Day] = themeName.replace("-dark", "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace(Regex("(-light)?-compact"), "-dark-compact")
            }
            themeName.contains("Flat-Remix-GTK") -> {
                val isSolid = themeName.contains("-Solid")
                val withoutBorder = themeName.contains("-NoBorder")
                val basename = themeName.split(Regex("-")).slice(IntRange(0, 3)).joinToString(separator = "-")
                variants[Variant.Day] =
                    basename + (if (themeName.contains("-Darker")) "-Darker" else "") + if (isSolid) "-Solid" else ""
                variants[Variant.Night] =
                    (basename + (if (themeName.contains("-Darkest")) "-Darkest" else "-Dark") + (if (isSolid) "-Solid" else "") +
                            if (withoutBorder) "-NoBorder" else "")
            }
            themeName.contains("HighContrast") -> {
                variants[Variant.Day] = "HighContrast"
                variants[Variant.Night] = "HighContrastInverse"
            }
            themeName.matches(Regex(pattern = "^(Layan|Macwaita|Matcha|Nextwaita)")) -> {
                val basename = themeName.split(Regex("-")).toTypedArray()[0]
                variants[Variant.Day] = themeName.replace("-dark", "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace("$basename(-light)?", "$basename-dark")
            }
            themeName.contains("Mojave") -> {
                variants[Variant.Day] = themeName.replace("-dark", "-light")
                variants[Variant.Night] = variants[Variant.Day]!!.replace("-light", "-dark")
            }
            themeName.contains("Plata") -> {
                variants[Variant.Day] = themeName.replace("-Noir", "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace("Plata(-Lumine)?", "Plata-Noir")
            }
            themeName.matches(Regex("^Prof-Gnome-(.+)-3(.*)")) -> {
                variants[Variant.Day] = themeName.replace(Regex("-Dark(?!er)"), "-Light")
                variants[Variant.Night] = variants[Variant.Day]!!.replace(Regex("(-Light(-DS)?|-Darker)"), "-Dark")
            }
            themeName.contains("Simply_Circles") -> {
                variants[Variant.Day] = themeName.replace("_Dark", "_Light")
                variants[Variant.Night] = themeName.replace("_Light", "_Dark")
            }
            themeName.contains("Teja") -> {
                /*
                 * If themeName was Teja_Light, potentialDarkVariant will be ['Teja'].
                 * If themeName was Teja_Darkest (or anything other than _Light), potentialDarkVariant will be
                 * ['Teja','Darkest']
                 */
                val potentialDarkVariant =
                    themeName.replace("_Light", "").split(Regex("_")).toTypedArray()
                val darkVariant =
                    '_'.toString() + if (potentialDarkVariant.size > 1) potentialDarkVariant[1] else "Dark"
                variants[Variant.Day] = themeName.replace(Regex("(_Dark(est)?|_Black)"), "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace("_Light", "") + darkVariant
            }
            themeName.contains("vimix") -> {
                variants[Variant.Day] = themeName.replace("-dark", "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace(Regex("vimix(-light)?"), "vimix-dark")
            }
            else -> {
                variants[Variant.Day] = themeName.replace(Regex("-dark(?!er)(est)?"), "")
                variants[Variant.Night] = variants[Variant.Day]!!.replace(Regex("(-light|-darker)"), "") + if (themeName.contains("-darkest")) "-darkest" else "-dark"
            }
        }
        return variants
    }

    /**
     * These enum values exist so that the keys of [.guessFrom] can be strongly typed.
     */
    enum class Variant {
        Original, Day, Night
    }
}
