/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
#include "com_github_weisj_darkmode_platform_linux_gtk_GtkThemeUtils.h"
#include "GioUtils.hpp"

#include <glibmm-2.4/glibmm.h>
#include <giomm-2.4/giomm.h>
#include <gtk/gtk.h>
#include <utility>

/**
 * Get all the directories of the system for a resource.
 * @param resource The resource to get the directories.
 * @returns An array of paths.
 */
std::vector<std::string> get_resources_dirs_paths(const std::string &resource) {
    std::vector < std::string > first_two_paths;

    first_two_paths.push_back(Glib::build_filename(Glib::get_home_dir(), "." + resource));
    first_two_paths.push_back(Glib::build_filename(Glib::get_user_data_dir(), resource));

    std::vector < std::string > system_data_dirs = Glib::get_system_data_dirs();
    std::vector < std::string > system_data_dirs_processed;
    system_data_dirs_processed.resize(system_data_dirs.size());
    std::transform(system_data_dirs.begin(), system_data_dirs.end(), system_data_dirs_processed.begin(),
            [&](const std::string &i) {
                return Glib::build_filename(i, resource);
            });

    std::vector < std::string > paths(first_two_paths);
    paths.insert(paths.end(), system_data_dirs_processed.begin(), system_data_dirs_processed.end());

    return paths;
}

void get_resources_for_path(const std::string &path, std::vector<std::map<std::string, std::string>> &out) {
    Glib::RefPtr < Gio::File > resources_dir = Gio::File::create_for_path(path);

    if (resources_dir->query_file_type(Gio::FileQueryInfoFlags::FILE_QUERY_INFO_NONE) != Gio::FileType::FILE_TYPE_DIRECTORY) {
        return;
    }

    Glib::RefPtr < Gio::FileEnumerator > resources_dirs_enumerator = resources_dir->enumerate_children("",
            Gio::FileQueryInfoFlags::FILE_QUERY_INFO_NONE);

    while (true) {
        Glib::RefPtr < Gio::FileInfo > resource_dir_info = resources_dirs_enumerator->next_file();

        if (!resource_dir_info) {
            break;
        }

        Glib::RefPtr < Gio::File > resource_dir = resources_dirs_enumerator->get_child(resource_dir_info);

        if (!resource_dir) {
            break;
        }
        std::map < std::string, std::string > resource;
        resource.insert(std::pair<std::string, std::string>("name", resource_dir->get_basename()));
        resource.insert(std::pair<std::string, std::string>("path", resource_dir->get_path()));
        out.push_back(resource);
    }
    resources_dirs_enumerator->close();
}

/**
 * Get all the resources installed on the system of a certain type.
 * @param type The resources to get.
 * @returns A set of installed resources.
 */
std::vector<std::map<std::string, std::string>> get_installed_resources(const std::string &type) {
    std::vector<std::map<std::string, std::string>> installed_resources;
    std::vector < std::string > resources_dirs_paths = get_resources_dirs_paths(type);
    std::for_each(resources_dirs_paths.begin(), resources_dirs_paths.end(), [&](const std::string &i) {
        return get_resources_for_path(i, installed_resources);
    });
    return installed_resources;
}

bool gtk_3_css_exists(unsigned int gtk_version, std::map<std::string, std::string> &theme) {
    if (gtk_version % 2) {
        gtk_version += 1;
    }
    std::string full_gtk_version = "gtk-3." + std::to_string(gtk_version);
    Glib::RefPtr < Gio::File > css_file = Gio::File::create_for_path(
            Glib::build_filename(theme["path"], full_gtk_version, "gtk.css"));
    if (css_file->query_exists()) {
        return true;
    } else {
        Glib::RefPtr < Gio::File > css_file = Gio::File::create_for_path(
                Glib::build_filename(theme["path"], "gtk-3.0", "gtk.css"));
        return css_file->query_exists();
    }
}

void verify_css_for_theme(std::map<std::string, std::string> theme, std::vector<std::string> &out) {
    std::vector<unsigned int> v { 0, gtk_get_minor_version() };
    bool version = std::any_of(v.begin(), v.end(), [&](unsigned int i) {
        return gtk_3_css_exists(i, theme);
    });
    if (version) {
        out.push_back(theme["name"]);
    }
}

/**
 * Get all the installed GTK themes on the system.
 * @returns A set containing all the installed GTK themes names.
 */
std::vector<std::string> get_installed_gtk_themes() {
    ensure_gio_init();
    //These themes come by default
    std::vector < std::string > themes( { "Adwaita", "HighContrast", "HighContrastInverse" });
    std::vector<std::map<std::string, std::string>> installed_resources = get_installed_resources("themes");
    std::for_each(installed_resources.begin(), installed_resources.end(), [&](std::map<std::string, std::string> i) {
        return verify_css_for_theme(std::move(i), themes);
    });
    return themes;
}

JNIEXPORT jobject JNICALL Java_com_github_weisj_darkmode_platform_linux_gtk_GtkThemeUtils_getInstalledThemes(
        JNIEnv *env, jclass) {
    std::vector < std::string > vector = get_installed_gtk_themes();
    jclass java_util_ArrayList = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    jmethodID java_util_ArrayList_ = env->GetMethodID(java_util_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_size = env->GetMethodID(java_util_ArrayList, "size", "()I");
    jmethodID java_util_ArrayList_get = env->GetMethodID(java_util_ArrayList, "get", "(I)Ljava/lang/Object;");
    jmethodID java_util_ArrayList_add = env->GetMethodID(java_util_ArrayList, "add", "(Ljava/lang/Object;)Z");

    jobject result = env->NewObject(java_util_ArrayList, java_util_ArrayList_, vector.size());
    for (std::string s : vector) {
        jstring element = env->NewStringUTF(s.c_str());
        env->CallBooleanMethod(result, java_util_ArrayList_add, element);
        env->DeleteLocalRef(element);
    }
    return result;
}
