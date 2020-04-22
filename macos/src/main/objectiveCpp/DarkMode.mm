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
#import "com_github_weisj_darkmode_platform_macos_MacOSNative.h"
#import <JavaNativeFoundation/JavaNativeFoundation.h>
#import <AppKit/AppKit.h>

#define OBJC(jl) ((id)jlong_to_ptr(jl))

#define KEY_APPLE_INTERFACE_STYLE @"AppleInterfaceStyle"
#define KEY_SWITCHES_AUTOMATICALLY @"AppleInterfaceStyleSwitchesAutomatically"

#define EVENT_THEME_CHANGE @"AppleInterfaceThemeChangedNotification"
#define EVENT_HIGH_CONTRAST @"AXInterfaceIncreaseContrastStatusDidChange"
#define VALUE_DARK @"Dark"

@interface PreferenceChangeListener:NSObject {
    @public JavaVM *jvm;
    @public jobject callback;
}
@end

@implementation PreferenceChangeListener
- (id)initWithJVM:(JavaVM *)jvm_ andCallBack:(jobject)callback_ {
    self = [super init];
    self->jvm = jvm_;
    self->callback = callback_;

    NSDistributedNotificationCenter *center = [NSDistributedNotificationCenter defaultCenter];
    [self listenToKey:EVENT_THEME_CHANGE onCenter:center];
    [self listenToKey:EVENT_HIGH_CONTRAST onCenter:center];
    return self;
}

- (void)dealloc {
    NSDistributedNotificationCenter *center = [NSDistributedNotificationCenter defaultCenter];
    [center removeObserver:self]; // Removes all registered notifications.
    [super dealloc];
}

- (void)listenToKey:(NSString *)key onCenter:(NSDistributedNotificationCenter *)center {
     [center addObserver:self
                selector:@selector(notificationEvent:)
                    name:key
                  object:nil];
}

- (void)runCallback {
    if (!jvm) return;
    JNIEnv *env;
    BOOL detach = NO;
    int getEnvStat = jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        detach = YES;
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
    if (detach) jvm->DetachCurrentThread();
}

- (void)notificationEvent:(NSNotification *)notification {
    [self runCallback];
}

@end

JNIEXPORT jboolean JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_isDarkThemeEnabled(JNIEnv *env, jclass obj) {
JNF_COCOA_ENTER(env);
    if(@available(macOS 10.14, *)) {
        NSApplication *app = [NSApplication sharedApplication];
        NSAppearance *current = app.appearance;
        [JNFRunLoop performOnMainThreadWaiting:YES withBlock:^{
            app.appearance = nil; // Make sure the system appearance is used.
        }];
        NSAppearance *appearance = app.effectiveAppearance;
        NSAppearanceName appearanceName = [appearance bestMatchFromAppearancesWithNames:@[NSAppearanceNameAqua,
                                                                                          NSAppearanceNameDarkAqua]];
        app.appearance = current; // Restore original appearance.
        NSLog(@"%@", appearance.name);
        return (jboolean) [appearanceName isEqualToString:NSAppearanceNameDarkAqua];
    } else {
        return (jboolean) NO;
    }
JNF_COCOA_EXIT(env);
    return NO;
}

JNIEXPORT jboolean JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_isHighContrastEnabled(JNIEnv *env, jclass obj) {
JNF_COCOA_ENTER(env);
    return (jboolean) NSWorkspace.sharedWorkspace.accessibilityDisplayShouldIncreaseContrast;
JNF_COCOA_EXIT(env);
    return (jboolean) NO;
}

JNIEXPORT jlong JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_createPreferenceChangeListener(JNIEnv *env, jclass obj, jobject callback) {
JNF_COCOA_DURING(env); // We dont want an auto release pool.
    JavaVM *jvm;
    if (env->GetJavaVM(&jvm) == 0) {
        jobject callbackRef = env->NewGlobalRef(callback);
        PreferenceChangeListener *listener = [[PreferenceChangeListener alloc] initWithJVM:jvm andCallBack: callbackRef];
        [listener retain];
        return reinterpret_cast<jlong>(listener);
    }
    return (jlong) 0;
JNF_COCOA_HANDLE(env);
    return (jlong) 0;
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_deletePreferenceChangeListener(JNIEnv *env, jclass obj, jlong listenerPtr) {
JNF_COCOA_ENTER(env);
    PreferenceChangeListener *listener = OBJC(listenerPtr);
    if (listener) {
        env->DeleteGlobalRef(listener->callback);
        [listener release];
        [listener dealloc];
    }
JNF_COCOA_EXIT(env);
}
