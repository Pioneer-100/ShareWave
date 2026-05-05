package com.swiftshare.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────── Primary palette (deep indigo → electric cyan) ───────────────────────
val PrimaryDark        = Color(0xFF1A1B2E)    // deep navy background
val PrimarySurface     = Color(0xFF242640)    // card / surface
val PrimaryAccent      = Color(0xFF6C63FF)    // indigo accent
val PrimaryCyan        = Color(0xFF00E5FF)    // electric cyan
val PrimaryGradStart   = Color(0xFF6C63FF)
val PrimaryGradEnd     = Color(0xFF00E5FF)

// ─────────────────────── Secondary palette ────────────────────────────────────────────────────
val SecondaryPink      = Color(0xFFFF6B9D)    // error / destructive
val SecondaryGreen     = Color(0xFF4ADE80)    // success
val SecondaryAmber     = Color(0xFFFBBF24)    // warning / in-progress
val SecondaryPurple    = Color(0xFFA78BFA)    // AI features chip

// ─────────────────────── Neutrals ─────────────────────────────────────────────────────────────
val NeutralWhite       = Color(0xFFFFFFFF)
val NeutralGray100     = Color(0xFFF5F5F5)
val NeutralGray200     = Color(0xFFE5E5E5)
val NeutralGray400     = Color(0xFFA3A3A3)
val NeutralGray600     = Color(0xFF525252)
val NeutralGray800     = Color(0xFF262626)
val NeutralBlack       = Color(0xFF0A0A0A)

// ─────────────────────── Light theme overrides ────────────────────────────────────────────────
val LightBackground    = Color(0xFFF8F9FC)
val LightSurface       = Color(0xFFFFFFFF)
val LightOnSurface     = Color(0xFF1A1B2E)

// ─────────────────────── Dark theme defaults ─────────────────────────────────────────────────
val DarkBackground     = PrimaryDark
val DarkSurface        = PrimarySurface
val DarkOnSurface      = NeutralGray100

// ─────────────────────── Radar-specific ──────────────────────────────────────────────────────
val RadarRingColor     = PrimaryCyan.copy(alpha = 0.15f)
val RadarSweepColor    = PrimaryCyan.copy(alpha = 0.35f)
val RadarDotColor      = PrimaryCyan
val RadarDotGlow       = PrimaryCyan.copy(alpha = 0.5f)
