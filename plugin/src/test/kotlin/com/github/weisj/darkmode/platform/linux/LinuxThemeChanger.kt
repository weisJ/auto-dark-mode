package com.github.weisj.darkmode.platform.linux

import com.github.weisj.darkmode.platform.ThemeChanger

const val GSETTINGS_PATH = "org.gnome.desktop.interface"
const val GSETTINGS_KEY = "gtk-theme"
const val XFCONF_QUERY_CHANNEL = "xsettings"
const val XFCONF_QUERY_PROPERTY = "/Net/ThemeName"

class GSettingsThemeChanger : ThemeChanger {
    override var currentTheme: String
        get() = "gsettings get $GSETTINGS_PATH $GSETTINGS_KEY".runCommand().stripQuotes()
        set(value) {
            "gsettings set $GSETTINGS_PATH $GSETTINGS_KEY $value".runCommand()
        }
}

class XfConfQueryThemeChanger : ThemeChanger {
    override var currentTheme: String
        get() = "xfconf-query -c $XFCONF_QUERY_CHANNEL -p $XFCONF_QUERY_PROPERTY".runCommand().stripQuotes()
        set(value) {
            "xfconf-query -c $XFCONF_QUERY_CHANNEL -p $XFCONF_QUERY_PROPERTY -s $value".runCommand()
        }
}

class KdeThemeChanger : ThemeChanger {
    override var currentTheme: String
        // TODO find a way to get the current plasma theme
        get() = ""
        set(value) {
            "lookandfeeltool -a $value".runCommand()
        }
}
