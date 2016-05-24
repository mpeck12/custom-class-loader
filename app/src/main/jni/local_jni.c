/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <sys/mman.h>

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */

/* This version should be packaged with the package */

extern int ashmem_open();

jstring
Java_com_example_dex_MainActivity_stringFromLocalJni( JNIEnv* env,
                                                  jobject thiz )
{
#if defined(__arm__)
  #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a/NEON (hard-float)"
      #else
        #define ABI "armeabi-v7a/NEON"
      #endif
    #else
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a (hard-float)"
      #else
        #define ABI "armeabi-v7a"
      #endif
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
   #define ABI "x86"
#elif defined(__x86_64__)
   #define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
   #define ABI "mips64"
#elif defined(__mips__)
   #define ABI "mips"
#elif defined(__aarch64__)
   #define ABI "arm64-v8a"
#else
   #define ABI "unknown"
#endif

    /* Deliberately map memory at an explicit address and with both PROT_WRITE and PROT_EXEC */
    int page_size;
    void *ptr;

    /* do something that will trigger -fstack-protector-strong to add stack protection code */
    /* see https://outflux.net/blog/archives/2014/01/27/fstack-protector-strong/ for more info */
    /* With this code added: strings ./app/build/intermediates/ndk/debug/lib/<arch>/libapp.so |grep stack
     *                       __stack_chk_guard
     *                       __stack_chk_fail
     * (using default build options)
     */
    char array[300];

    page_size = getpagesize();
    ptr = mmap((void*) 32026624, 10 * page_size, PROT_READ | PROT_WRITE | PROT_EXEC,
               MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, 0, 0);
    sprintf(array, "Hello from (Bundled) JNI ! Compiled with ABI " ABI " location %x", ptr);
    return (*env)->NewStringUTF(env, array);
}
