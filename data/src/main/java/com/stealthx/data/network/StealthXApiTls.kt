/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

object StealthXApiTls {
    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
        .add("api.stealthx.tech", "sha256/1e85xNSEj+dcImOJS0iNkfMZOrZdvJJzzPCqT1/CZDc=")
        .add("api.stealthx.tech", "sha256/nWN7PSep5XDQdge5zK24CnCRXHr3KvzhKEGxsdqCX9E=")
        .add("api.stealthx.tech", "sha256/fk6IOKit1ild5647BH06ujSIq5XbCgqlbYl6ANhhi88=")
        .build()

    fun newClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder().certificatePinner(certificatePinner)
}
