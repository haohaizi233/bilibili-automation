package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bilibiliautomation.domain.model.SessionCookie

@Composable
fun PassportHitlWebView(
    cookies: List<SessionCookie> = emptyList(),
    onSuccess: (List<SessionCookie>) -> Unit,
    modifier: Modifier = Modifier,
) {
    GeeTestWebView(
        cookies = cookies,
        onLoginCookies = onSuccess,
        modifier = modifier,
    )
}
