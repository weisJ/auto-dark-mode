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
#import <Foundation/Foundation.h>
#import <jni.h>

__BEGIN_DECLS

#ifndef jlong_to_ptr
#define jlong_to_ptr(a) ((void *)(uintptr_t)(a))
#endif

#define OBJC(jl) ((id)jlong_to_ptr(jl))

#define JNF_COCOA_DURING(env)                                   \
@try {

#define JNF_COCOA_HANDLE(env)									\
} @catch(NSException *localException) {							\
    [JNF_Exception throwToJava:env exception:localException];	\
}

#define JNF_COCOA_ENTER(env)									\
@autoreleasepool {                                              \
    @try {                                                      \
        @autoreleasepool {

#define JNF_COCOA_EXIT(env)										\
        } /*@autoreleasepool*/                                  \
    JNF_COCOA_HANDLE(env)                                       \
} /*@autoreleasepool*/

@interface JNF_RunLoop : NSObject {}
+ (void)performOnMainThread:(SEL)aSelector on:(id)target withObject:(id)arg waitUntilDone:(BOOL)waitUntilDone;
+ (void)performOnMainThreadWaiting:(BOOL)waitUntilDone withBlock:(void (^)(void))block;
@end

@interface JNF_Exception : NSException
+ (void)throwToJava:(JNIEnv *)env exception:(NSException *)exception;
@end

__END_DECLS
