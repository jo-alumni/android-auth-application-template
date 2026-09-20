package com.example.authapplication.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * アプリ全体で共有する [SnackbarHostState]。
 *
 * `SnackbarHost` は `:app` の骨組み（`AppNavigationScaffold`）に1つだけ置き、各featureの
 * Navigationファイルはこの CompositionLocal 経由で `showSnackbar` を呼ぶ
 * （詳しくは .claude/rules/error-handling.md 参照）。
 *
 * 既定値を持たせると未提供に気付けないため、Providerが無いときは例外にする。
 */
val LocalSnackBarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackBarHostState is not provided.")
}
