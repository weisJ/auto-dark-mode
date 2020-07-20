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
package com.github.weisj.darkmode.platform.linux.gnome;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is effectively a conversion of the GtkVariants.js module seen <a href=
 * "https://gitlab.com/rmnvgr/nightthemeswitcher-gnome-shell-extension/-/blob/0d32907de1d004a6a92f02b8e0b79302340e5244/src/modules/GtkVariants.js">here</a>.
 * <p>
 * This class should only be used as a temporary solution until settings are implemented which allow the user to
 * denote which of their themes they consider to be their light and dark themes. This settings should be applicable to
 * all distributions (not just ones using Gnome) as most distributions have different themes which provide a dark and
 * light variant. Very few distributions will simply have a "dark <i>mode</i>" and a "light <i>mode</i>" like Windows
 * and Mac OS.
 */
public class GtkVariants {
    /**
     * This documentation is straight from the file mentioned in the {@link GtkVariants} JavaDoc.
     * <p/>
     * The magic of guessing theme variants happens here.
     * <p>
     * If the theme doesn't fit a particular case, we'll do the following:
     * <p>
     * - Remove any signs of a dark variant to the theme name to get the day
     * variant
     * <p>
     * - Remove any signs of a light variant to the day variant and add '-dark' to
     * get the night variant
     * <p>
     * <p>
     * <p>
     * For themes that don't work with the general rule, a particular case must be written. Day and night variants
     * should be guessed with the most generic light and dark variants the theme offer, except if the user explicitly
     * chose a specific variant.
     * <p>
     * <p>
     * <p>
     * Light variants, from the most to the least generic:
     * <p>
     * - ''
     * <p>
     * - '-light'
     * <p>
     * - '-darker'
     * <p>
     * <p>
     * Dark variants, from the most the least generic:
     * <p>
     * - '-dark'
     * <p>
     * - '-darkest'
     *
     * @param  themeName the name of a theme. This could be one of the many variants of your theme (i.e. the dark
     *                   version or whatever other variants the theme offers)
     * @return           a map containing three keys: {@link Variant#original} (the provided themeName), {@link Variant#day} (the guessed day
     *                   variant), and {@link Variant#night} (the guessed night variant)
     */
    public static Map<Variant, String> guessFrom(String themeName) {
        Map<Variant, String> variants = new HashMap<>();
        variants.put(Variant.original, themeName);

        if (themeName.contains("Adapta")) {
            variants.put(Variant.day, themeName.replace("-Nokto", ""));
            variants.put(Variant.night, variants.get(Variant.day).replace("Adapta", "Adapta-Nokto"));
        } else if (themeName.contains("Arc")) {
            variants.put(Variant.day, themeName.replaceAll("-Dark(?!er)", ""));
            variants.put(Variant.night, variants.get(Variant.day).replaceAll("Arc(-Darker)?", "Arc-Dark"));
        } else if (themeName.matches("Cabinet")) {
            variants.put(Variant.day, themeName.replaceAll("-Dark(?!er)", "-Light"));
            variants.put(Variant.night, variants.get(Variant.day).replaceAll("(-Light|-Darker)", "-Dark"));
        } else if (themeName.matches("^(Canta|ChromeOS|Materia|Orchis).*-compact")) {
            variants.put(Variant.day, themeName.replace("-dark", ""));
            variants.put(Variant.night, variants.get(Variant.day).replaceAll("(-light)?-compact", "-dark-compact"));
        } else if (themeName.contains("Flat-Remix-GTK")) {
            boolean isSolid = themeName.contains("-Solid");
            boolean withoutBorder = themeName.contains("-NoBorder");
            String basename = String.join("-", getSliceOfArray(themeName.split("-"), 0, 4));
            variants.put(Variant.day,
                    basename + (themeName.contains("-Darker") ? "-Darker" : "") + (isSolid ? "-Solid" : ""));
            variants.put(Variant.night,
                    basename + (themeName.contains("-Darkest") ? "-Darkest" : "-Dark") + (isSolid ? "-Solid" : "")
                            + (withoutBorder ? "-NoBorder" : ""));
        } else if (themeName.contains("HighContrast")) {
            variants.put(Variant.day, "HighContrast");
            variants.put(Variant.night, "HighContrastInverse");
        } else if (themeName.matches("^(Layan|Macwaita|Matcha|Nextwaita)")) {
            String basename = themeName.split("-")[0];
            variants.put(Variant.day, themeName.replace("-dark", ""));
            variants.put(Variant.night, variants.get(Variant.day).replace(basename + "(-light)?", basename + "-dark"));
        } else if (themeName.contains("Mojave")) {
            variants.put(Variant.day, themeName.replace("-dark", "-light"));
            variants.put(Variant.night, variants.get(Variant.day).replace("-light", "-dark"));
        } else if (themeName.contains("Plata")) {
            variants.put(Variant.day, themeName.replace("-Noir", ""));
            variants.put(Variant.night, variants.get(Variant.day).replace("Plata(-Lumine)?", "Plata-Noir"));
        } else if (themeName.matches("^Prof-Gnome-(.+)-3(.*)")) {
            variants.put(Variant.day, themeName.replaceAll("-Dark(?!er)", "-Light"));
            variants.put(Variant.night, variants.get(Variant.day).replaceAll("(-Light(-DS)?|-Darker)", "-Dark"));
        } else if (themeName.contains("Simply_Circles")) {
            variants.put(Variant.day, themeName.replace("_Dark", "_Light"));
            variants.put(Variant.night, themeName.replace("_Light", "_Dark"));
        } else if (themeName.contains("Teja")) {
            /*
             * If themeName was Teja_Light, potentialDarkVariant will be ['Teja'].
             * If themeName was Teja_Darkest (or anything other than _Light), potentialDarkVariant will be
             * ['Teja','Darkest']
             */
            String[] potentialDarkVariant = themeName.replace("_Light", "").split("_");
            String dark_variant = '_' + (potentialDarkVariant.length > 1 ? potentialDarkVariant[1] : "Dark");
            variants.put(Variant.day, themeName.replaceAll("(_Dark(est)?|_Black)", ""));
            variants.put(Variant.night, variants.get(Variant.day).replace("_Light", "") + dark_variant);
        } else if (themeName.contains("vimix")) {
            variants.put(Variant.day, themeName.replace("-dark", ""));
            variants.put(Variant.night, variants.get(Variant.day).replaceAll("vimix(-light)?", "vimix-dark"));
        } else {
            variants.put(Variant.day, themeName.replaceAll("-dark(?!er)(est)?", ""));
            variants.put(Variant.night, variants.get(Variant.day).replaceAll("(-light|-darker)", "") + (themeName.contains(
                    "-darkest") ? "-darkest" : "-dark"));
        }
        return variants;
    }

    /**
     * These enum values exist so that the keys of {@link #guessFrom(String)} can be strongly typed.
     */
    enum Variant {
        original,
        day,
        night
    }

    /**
     * Equivalent to
     * <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice">Array.prototype.slice()</a>
     * from JavaScript.
     *
     * @param  arr   the array to slice
     * @param  start a zero-based index at which to start extraction
     * @param  end   a zero-based index before which to end extraction
     * @return       the sliced array
     */
    private static String[] getSliceOfArray(String[] arr,
            int start, int end) {
        return (String[]) Arrays.stream(arr, start, end).toArray();
    }
}
