package com.johnreg.cameraxapp

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

// This is how we can use the camera feed in jetpack compose
@Composable
fun CameraPreview(
    // This will be the core of our camerax feed, with this controller we can control what is displayed
    // front camera, back camera, what we do without taking a photo, capturing a video
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // AndroidView is needed when we want to have an older view from the xml view system
    AndroidView(
        // Lambda in which we create that older view, which in this case is a PreviewView
        factory = {
            // Pass 'it', the context in order to create that and 'apply' some changes to this view
            PreviewView(it).apply {
                // Link this controller we passed
                this.controller = controller
                // Listen to lifecycle changes to properly show the camera or freed up when it's not needed anymore
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        // Make sure that we pass in the modifier
        modifier = modifier
    )
}