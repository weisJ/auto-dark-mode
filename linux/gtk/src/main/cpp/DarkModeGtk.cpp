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
#include "com_github_weisj_darkmode_platform_linux_gtk_GtkNative.h"
#include "GioUtils.hpp"

#include <chrono>
#include <condition_variable>
#include <thread>

#include <iostream>
#include <fstream>

#include <gtkmm.h>

Glib::RefPtr<Gtk::Settings> settings;

// to sync the intialization of `settings`
std::condition_variable cv;
std::mutex cv_mutex;

JNIEXPORT jstring JNICALL
Java_com_github_weisj_darkmode_platform_linux_gtk_GtkNative_getCurrentTheme(JNIEnv *env, jclass) {
    std::unique_lock<std::mutex> lock(cv_mutex);
    if (!cv.wait_for(lock, std::chrono::seconds(3), [] {return bool(settings);})) {
        return env->NewStringUTF("");
    }

    auto themeStr = settings->property_gtk_theme_name().get_value();
    return env->NewStringUTF(themeStr.c_str());
}

struct EventHandler {
    JavaVM *jvm;
    JNIEnv *env;
    jobject callback;

    Glib::RefPtr<Gtk::Application> app;
    std::shared_ptr<Glib::Dispatcher> quitDispatcher;
    sigc::connection settingsChangedSignalConnection;
    std::thread loopThread;

    void runCallBack() {
        bool detachNecessary = false;
        int getEnvStat = jvm->GetEnv((void**) &env, JNI_VERSION_1_6);
        if (getEnvStat == JNI_EDETACHED) {
            detachNecessary = true;
            if (jvm->AttachCurrentThread((void**) &env, NULL) != 0) return;
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
        if (detachNecessary) jvm->DetachCurrentThread();
    }

    void stop() {
        settingsChangedSignalConnection.disconnect();
        quitDispatcher->emit();
        loopThread.join();
    }

    /*
     * quit the Gtk application `app`
     * MUST be called from the Gtk thread
     */
    void quit() {
        app->release();
        app->quit();

        // let the Glib::Dispatcher destruct in the app thread
        // makes the assertion in Glib::DispatchNotifier::unreference_instance happy
        quitDispatcher.reset();
    }

    /*
     * creates and runs the event loop (app->run()) of a mini Gtk application
     * the event loop is needed to receive signals when the theme changes
     */
    void runGtkApp() {
        {
            std::lock_guard < std::mutex > lock(cv_mutex);

            app = Gtk::Application::create();
            settings = Gtk::Settings::get_default();
            settingsChangedSignalConnection = settings->property_gtk_theme_name().signal_changed().connect(
                    sigc::mem_fun(this, &EventHandler::runCallBack));
        }
        cv.notify_all();

        // Glib::Dispatcher runs the callback in the thread where it was instantiated
        quitDispatcher = std::make_shared<Glib::Dispatcher>();
        quitDispatcher->connect(sigc::mem_fun(*this, &EventHandler::quit));
        app->hold();
        app->run();
    }

    EventHandler(JavaVM *jvm_, jobject callback_) {
        jvm = jvm_;
        callback = callback_;

        loopThread = std::thread(&EventHandler::runGtkApp, this);
    }
};

JNIEXPORT jlong JNICALL
Java_com_github_weisj_darkmode_platform_linux_gtk_GtkNative_createEventHandler(JNIEnv *env, jclass obj, jobject callback) {
    JavaVM *jvm;
    if (env->GetJavaVM(&jvm) == JNI_OK) {
        jobject callbackRef = env->NewGlobalRef(callback);
        EventHandler* eventHandler = new EventHandler(jvm, callbackRef);
        return reinterpret_cast<jlong>(eventHandler);
    }
    return (jlong) 0;
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_linux_gtk_GtkNative_deleteEventHandler(JNIEnv *env, jclass obj, jlong eventHandler)
{
    EventHandler *handler = reinterpret_cast<EventHandler *>(eventHandler);
    if (handler) {
        env->DeleteGlobalRef(handler->callback);
        handler->stop();
        delete handler;
    }
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_linux_gtk_GtkNative_init(JNIEnv *env, jclass obj) {
    ensure_gio_init();
}
