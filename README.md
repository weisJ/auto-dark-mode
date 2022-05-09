### Note for Windows and macOS users:
In IDEA versions >= 2020.3 the functionality of this plugin already exists out of the box.

# Auto Dark Mode IDEA Plugin

Plugin that automatically switches the IDEA theme based on
operating system settings. The plugin distinguishes between `Light`, `Dark` and `High Contrast` mode and
the theme used for each mode can be customized.
This plugin currently works for Windows and macOS.

Linux support is both limited and experimental. At the moment, Linux desktop environments that have an [XSettings](https://www.freedesktop.org/wiki/Specifications/xsettings-spec/) daemon running are supported.
Gtk based desktop environments such as Gnome and Xfce ship with such a daemon out of the box. Users of minimalist window managers (such as i3) can choose to run a daemon shipped with Gnome (`gsd-xsettings`) or Xfce (`xfsettingsd`), among many choices.

By default, the following themes are used:

| Mode          | Theme         |
|:--------------|:--------------|
| Light         | IntelliJ      |
| Dark          | Darcula       |
| High Contrast | High Contrast |

## Building
````
./gradlew build
./gradlew buildPlugin
````

### Architecture support
| Operating System | x86 Support        | x86_64 Support     | arm64 Support          |
|------------------|--------------------|--------------------|------------------------|
| Windows          | :heavy_check_mark: | :heavy_check_mark: | :x:                    |
| macOS            | :x:                | :heavy_check_mark: | :heavy_check_mark:(M1) |
| Linux            | :x:                | :heavy_check_mark: | :x:                    |

### OS-dependent build components
When Gradle builds the plugin, it will only be able to compile
native components for the operating system running the build.
For example, macOS toolchains won't be available to someone
who is compiling on Windows. For this reason, this plugin depends on
artifacts built by a [custom GitHub Actions workflow](.github/workflows/libs.yml) for the platforms which cannot be compiled
in the given environment. [A custom Gradle plugin](buildSrc/src/main/kotlin/UsePrebuiltBinariesWhenUnbuildablePlugin.kt)
downloads these artifacts during the build if necessary.

##### This step requires a GitHub access token
For Gradle to be able to retrieve the pre-built artifacts, you need to provide a [personal access token](https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token) using the `githubAccessToken` property in [`gradle.properties`](gradle.properties) file. The access token only needs to have the permission to read repositories. Be sure to **not** commit your token.

##### Requirements for building

With exception to Linux, this plugin only requires that a standard
C++ toolchain be installed when building on Windows (i.e. VisualCpp)
and an Objective-C++ toolchain when building on macOS (i.e. Gcc or Clang).

At the moment, Linux requires a standard C++ toolchain like Gcc
as well as the following packages.
```
libsigc++-2.0-dev libglibmm-2.4-dev libgtk-3-dev libgtkmm-3.0-dev
```


## Running
You can use the standard `runIde` task to run this plugin
in a sandbox IDE. If you encounter errors like `"Directory '[project-folder]/auto-dark-mode/base/build/idea-sandbox/plugins' specified for property 'pluginsDirectory' does not exist."`, you might want
to try running the task `:auto-dark-mode-plugin:runIde` instead.

If you experience other issues, you can try a clean
environment by running the following command.
```
./gradlew clean build :auto-dark-mode-plugin:runIde
```
