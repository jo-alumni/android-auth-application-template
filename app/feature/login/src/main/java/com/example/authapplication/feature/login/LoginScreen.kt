package com.example.authapplication.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.ui.preview.AppPreview

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLoginClick: (id: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            // 下端のinsetsは各画面が自分で解決する（docs/window-insets.md 参照）。
            // ナビゲーションバー分を消費してから、残りのIME分だけを追加で確保する。
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_login_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text(stringResource(R.string.feature_login_id_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Text),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username },
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.feature_login_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .semantics { contentType = ContentType.Password },
        )
        if (uiState is LoginUiState.Error) {
            Text(
                text = stringResource(uiState.messageResId),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { onLoginClick(id, password) },
            enabled = uiState !is LoginUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (uiState is LoginUiState.Loading) {
                        R.string.feature_login_submitting
                    } else {
                        R.string.feature_login_submit
                    },
                ),
            )
        }
    }
}

/**
 * [LoginScreen] の全状態を1つのPreview関数で描くための供給元。
 * 状態を追加したらここへ足す。
 */
internal class LoginUiStatePreviewParameterProvider : PreviewParameterProvider<LoginUiState> {
    override val values = sequenceOf(
        LoginUiState.Idle,
        LoginUiState.Loading,
        LoginUiState.Error(messageResId = R.string.feature_login_error_blank_input),
    )
}

@PreviewLightDark
@Composable
private fun LoginScreenPreview(
    @PreviewParameter(LoginUiStatePreviewParameterProvider::class) uiState: LoginUiState,
) {
    AppPreview {
        LoginScreen(uiState = uiState, onLoginClick = { _, _ -> })
    }
}

/**
 * 入力欄・エラー文言・ボタンが縦に積み上がる画面なので、大フォント時に最も崩れやすい。
 * エラー表示が加わる [LoginUiState.Error] でフォントスケールを確認する。
 */
@PreviewFontScale
@Composable
private fun LoginScreenFontScalePreview() {
    AppPreview {
        LoginScreen(
            uiState = LoginUiState.Error(messageResId = R.string.feature_login_error_blank_input),
            onLoginClick = { _, _ -> },
        )
    }
}
