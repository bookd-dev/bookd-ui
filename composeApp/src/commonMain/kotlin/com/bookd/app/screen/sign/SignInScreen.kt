package com.bookd.app.screen.sign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bookd.app.screen.LocalNavBackStack
import com.bookd.app.screen.main.RouteSettings


@Composable
fun SignInScreen() {
    val navBackStack = LocalNavBackStack.current

    Column(
        modifier = Modifier.fillMaxSize().clickable {
            navBackStack.add(RouteSettings)
        },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sign In",
        )
    }
}