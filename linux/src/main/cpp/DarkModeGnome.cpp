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
 */
#include "com_github_weisj_darkmode_platform_linux_gnome_GnomeNative.h"

#include <string>
#include <thread>
#include <glibmm-2.4/glibmm.h>
#include <giomm-2.4/giomm.h>

JNIEXPORT jstring JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_getCurrentTheme(JNIEnv *env, jclass) {
  Gio::init();
  Glib::RefPtr<Gio::Settings> settings = Gio::Settings::create("org.gnome.desktop.interface");
  return env->NewStringUTF(settings->get_string("gtk-theme").c_str());
}

struct EventHandler {
  std::string themeSettingName = "gtk-theme";
  JavaVM *jvm;
  JNIEnv *env;
  jobject callback;
  Glib::RefPtr<Glib::MainLoop> mainLoop;
  Glib::RefPtr<Gio::Settings> settings;
  std::thread notificationLoop;

  void settingChanged(const Glib::ustring &name) {
    runCallBack();
  }

  void runCallBack() {
    bool detachNecessary = false;
    int getEnvStat = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
      detachNecessary = true;
      if (jvm->AttachCurrentThread((void **) &env, NULL) != 0) return;
    } else if (getEnvStat == JNI_EVERSION) {
      return;
    }

    jclass runnableClass = env->GetObjectClass(callback);
    jmethodID runMethodId = env->GetMethodID(runnableClass, "run", "()V");
    if (runMethodId) {
      env->CallVoidMethod(callback, runMethodId);
    }
    if (env->ExceptionCheck()) {
      env->ExceptionDescribe();
    }
    if (detachNecessary)
      jvm->DetachCurrentThread();
  }

  void run() {
    mainLoop->run();
  }

  void stop() {
    mainLoop->quit();
    notificationLoop.join();
  }

  EventHandler(JavaVM *jvm_, jobject callback_) {
    jvm = jvm_;
    callback = callback_;
    Gio::init();
    mainLoop = Glib::MainLoop::create(false);
    settings = Gio::Settings::create("org.gnome.desktop.interface");
    settings->signal_changed(themeSettingName).connect(sigc::mem_fun(this, &EventHandler::settingChanged));
    notificationLoop = std::thread(&EventHandler::run, this);
  }
};

JNIEXPORT jlong JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_createEventHandler(JNIEnv *env, jclass obj, jobject callback) {
  JavaVM *jvm;
  if (env->GetJavaVM(&jvm) == 0) {
    jobject callbackRef = env->NewGlobalRef(callback);
    EventHandler* eventHandler = new EventHandler(jvm, callbackRef);
    return reinterpret_cast<jlong>(eventHandler);
  }
  return (jlong) 0;
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_deleteEventHandler(JNIEnv *env, jclass obj, jlong eventHandler)
{
  EventHandler *handler = reinterpret_cast<EventHandler *>(eventHandler);
  if (handler) {
    env->DeleteGlobalRef(handler->callback);
    handler->stop();
    delete handler;
  }
}
