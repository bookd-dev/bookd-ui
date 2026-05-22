package com.bookd.app.basic.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

fun <T : Any> transitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    // Slide in from right when navigating forward
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(600)
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(600)
    )
}

fun <T : Any> popTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    // Slide in from left when navigating back
    slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(600)
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(600)
    )
}
fun <T : Any> predictivePopTransitionSpec(
): AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform= {
    // Slide in from left when navigating back
    slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(600)
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(600)
    )
}