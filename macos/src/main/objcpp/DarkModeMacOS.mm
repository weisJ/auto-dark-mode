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

#define NSRequiresAquaSystemAppearance CFSTR("NSRequiresAquaSystemAppearance")

#define KEY_APPLE_INTERFACE_STYLE @"AppleInterfaceStyle"
#define KEY_SWITCHES_AUTOMATICALLY @"AppleInterfaceStyleSwitchesAutomatically"

#define KEY_RECEIVED_EVENT @"AutoDarkModeReceivedEvent"

#define EVENT_THEME_CHANGE @"AppleInterfaceThemeChangedNotification"
#define EVENT_HIGH_CONTRAST @"AXInterfaceIncreaseContrastStatusDidChange"
#define VALUE_DARK @"Dark"

BOOL isPatched = NO;

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

    NSDistributedNotificationCenter *distributedCenter = [NSDistributedNotificationCenter defaultCenter];
    [self listenToKey:EVENT_THEME_CHANGE onCenter:distributedCenter];
    [self listenToKey:EVENT_HIGH_CONTRAST onCenter:distributedCenter];

    NSNotificationCenter defaultCenter = [NSNotificationCenter defaultCenter];
    [defaultCenter addObserver:self
                      selector:@selector(dispatchEvent:)
                          name:KEY_RECEIVED_EVENT
                        object:nil];
    return self;
}

- (void)dealloc {
    NSDistributedNotificationCenter *sharedCenter = [NSDistributedNotificationCenter defaultCenter];
    [sharedCenter removeObserver:self]; // Removes all registered notifications.
    NSNotificationCenter defaultCenter = [NSNotificationCenter defaultCenter];
    [defaultCenter removeObserver:self];
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
    NSLog(@"Received Event");
    [JNFRunLoop performOnMainThreadWaiting:NO withBlock:^{
        NSNotificationCenter defaultCenter = [NSNotificationCenter defaultCenter];
        [defaultCenter postNotificationName:KEY_RECEIVED_EVENT
                                     object:self];
    }];
}

- (void)dispatchEvent:(NSNotification *)notification {
    NSLog(@"Dispatching to IDEA.");
    [JNFRunLoop performOnMainThreadWaiting:NO withBlock:^{
            [self runCallback];
    }];
}

@end

BOOL isDarkModeCatalina() {
    NSAppearance *appearance = NSApp.effectiveAppearance;
    NSAppearanceName appearanceName = [appearance bestMatchFromAppearancesWithNames:@[NSAppearanceNameAqua,
                                                                                      NSAppearanceNameDarkAqua]];
    return [appearanceName isEqualToString:NSAppearanceNameDarkAqua];
}

BOOL isDarkModeMojave() {
    NSString *interfaceStyle = [[NSUserDefaults standardUserDefaults] stringForKey:KEY_APPLE_INTERFACE_STYLE];
    return [VALUE_DARK caseInsensitiveCompare:interfaceStyle] == NSOrderedSame;
}

BOOL isAutoMode() {
    return [[NSUserDefaults standardUserDefaults] boolForKey:KEY_SWITCHES_AUTOMATICALLY];
}

JNIEXPORT jboolean JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_isDarkThemeEnabled(JNIEnv *env, jclass obj) {
JNF_COCOA_ENTER(env);
    NSLog(isPatched ? @"Patched: Yes" : @"Patched: No");
    if(@available(macOS 10.15, *)) {
        if (isPatched) {
            NSLog(isDarkModeCatalina() ? @"Catalina-Dark: Yes" : @"Catalina-Dark: No");
            return (jboolean) isDarkModeCatalina();
        }
    }
    if (@available(macOS 10.14, *)) {
        NSLog(isDarkModeMojave() ? @"Mojave-Dark: Yes" : @"Mojave-Dark: No");
        return (jboolean) isDarkModeMojave();
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
    if (env->GetJavaVM(&jvm) == JNI_OK) {
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

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_patchAppBundle(JNIEnv *env, jclass obj) {
JNF_COCOA_ENTER(env);
    if (@available(macOS 10.15, *)) {
        NSString *name = [[NSBundle mainBundle] bundleIdentifier];
        if ([name containsString:@"jetbrains"]) {
            CFStringRef bundleName = (__bridge CFStringRef)name;

            Boolean exists = false;
            CFPreferencesGetAppBooleanValue(NSRequiresAquaSystemAppearance, bundleName, &exists);
            isPatched = exists ? YES : NO;

            if (!isPatched) {
                CFPreferencesSetAppValue(NSRequiresAquaSystemAppearance, kCFBooleanFalse, bundleName);
                CFPreferencesAppSynchronize(bundleName);
            }
        }
    } else {
        isPatched = false;
    }
JNF_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_macos_MacOSNative_unpatchAppBundle(JNIEnv *env, jclass obj) {
JNF_COCOA_ENTER(env);
    if (@available(macOS 10.15, *)) {
        NSString *name = [[NSBundle mainBundle] bundleIdentifier];
        if ([name containsString:@"jetbrains"]) {
            CFStringRef bundleName = (__bridge CFStringRef)name;
            CFPreferencesSetAppValue(NSRequiresAquaSystemAppearance, nil, bundleName);
            CFPreferencesAppSynchronize(bundleName);
        }
    }
JNF_COCOA_EXIT(env);
}
