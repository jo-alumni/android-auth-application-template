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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(uiState = LoginUiState.Idle, onLoginClick = { _, _ -> })
}
