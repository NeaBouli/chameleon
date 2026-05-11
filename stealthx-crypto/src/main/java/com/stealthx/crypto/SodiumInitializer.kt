/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.crypto

import com.goterl.lazysodium.LazySodium
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

/**
 * Singleton initializer for libsodium via lazysodium.
 *
 * MUST be called in Application.onCreate() before any crypto operation.
 * Thread-safe via @Synchronized. Idempotent — safe to call multiple times.
 *
 * CRITICAL: The :crypto process (CryptoService) must also call ensureInit()
 * in its own Service.onCreate() — each Android process needs its own JNI init.
 *
 * JVM fallback: when SodiumAndroid JNI fails (e.g. unit test runner),
 * the initializer falls back to LazySodiumJava (desktop libsodium via lazysodium-java).
 * lazysodium-java must be on testRuntimeClasspath — see stealthx-crypto/build.gradle.kts.
 */
object SodiumInitializer {

    @Volatile
    private var initialized = false

    private lateinit var _sodium: LazySodium

    val sodium: LazySodium
        get() {
            check(initialized) {
                "SodiumInitializer.ensureInit() must be called before accessing sodium. " +
                "Call it in Application.onCreate()."
            }
            return _sodium
        }

    @Synchronized
    fun ensureInit() {
        if (initialized) return
        _sodium = try {
            LazySodiumAndroid(SodiumAndroid())
        } catch (e: Throwable) {
            // On Android, libsodium must load via JNI — JVM fallback is not in the APK.
            // Only attempt JVM fallback in non-Android environments (unit test JVM runner).
            if (isAndroidRuntime()) throw RuntimeException("libsodium JNI init failed on Android", e)
            loadJvmFallback()
        }
        initialized = true
    }

    private fun isAndroidRuntime(): Boolean {
        val vmName = System.getProperty("java.vm.name") ?: ""
        val runtimeName = System.getProperty("java.runtime.name") ?: ""
        return vmName.contains("Dalvik", ignoreCase = true) ||
            runtimeName.contains("Android", ignoreCase = true)
    }

    fun isInitialized(): Boolean = initialized

    @Suppress("UNCHECKED_CAST")
    private fun loadJvmFallback(): LazySodium {
        val javaClass = Class.forName("com.goterl.lazysodium.LazySodiumJava")
        val sodiumJavaClass = Class.forName("com.goterl.lazysodium.SodiumJava")
        val sodiumJavaInstance = sodiumJavaClass.getDeclaredConstructor().newInstance()
        return javaClass.getDeclaredConstructor(sodiumJavaClass)
            .newInstance(sodiumJavaInstance) as LazySodium
    }
}
