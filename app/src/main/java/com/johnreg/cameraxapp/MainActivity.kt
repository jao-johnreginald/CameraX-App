@file:OptIn(ExperimentalMaterial3Api::class)

package com.johnreg.cameraxapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.johnreg.cameraxapp.ui.theme.CameraXAppTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    // An active recording, an active video that is going to be recorded and contains information about that
    // Initially that's null, but as soon as we start recording we want to assign something to that
    private var recording: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If we don't have the required permissions, request them
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }

        setContent {
            CameraXAppTheme {
                val scope = rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                // Put this in a remember block so it does not get recreated on a recomposition
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        // Enable everything we want to be able to use in combination with this preview feed
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
                        )
                    }
                }
                val viewModel = viewModel<MainViewModel>()
                val bitmaps by viewModel.bitmaps.collectAsState()

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    // By default, this bottom sheet peaks a little bit which is what we don't want
                    sheetPeekHeight = 0.dp,
                    sheetContent = {
                        PhotoBottomSheetContent(
                            bitmaps = bitmaps,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(
                                onClick = {
                                    controller.cameraSelector = cameraSelector(controller)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch camera"
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = "Open gallery"
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (hasRequiredPermissions()) {
                                        takePhoto(
                                            controller = controller,
                                            onPhotoTaken = viewModel::onTakePhoto
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take photo"
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (hasRequiredPermissions()) {
                                        recordVideo(controller)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Record video"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cameraSelector(controller: LifecycleCameraController): CameraSelector {
        return if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        // This lambda will give us the photo in form of a bitmap
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            // For most camerax related functions, we need to pass an executor
            // which contains information about which thread this is going to be executed on
            ContextCompat.getMainExecutor(applicationContext),
            // React to success and error events
            object : OnImageCapturedCallback() {
                // When capturing the image was a success, we get it in the form of an ImageProxy
                // which contains some more information about the image such as the rotationDegrees
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // Used to transform an image
                    val matrix = Matrix().apply {
                        // Rotate all the numbers in that Matrix based on our rotation degrees
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    // Use this Matrix to get a rotated bitmap
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                    // Pass the rotatedBitmap to the lambda
                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)

                    Toast.makeText(
                        applicationContext, exception.localizedMessage, Toast.LENGTH_LONG
                    ).show()
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    // Android Studio does not recognize that we already checked for permissions
    @SuppressLint("MissingPermission")
    private fun recordVideo(controller: LifecycleCameraController) {
        if (recording != null) {
            // We hit record while recording was already in progress
            // In that case we want to stop the recording and save the video
            recording?.stop()
            recording = null
        } else {
            // This file will get overridden everytime we make a new recording
            val outputFile = File(filesDir, "my-recording.mp4")
            // Assign the return value to our recording object
            recording = controller.startRecording(
                // For videos there is not an in-memory data type like we have for photos (bitmaps)
                // So we can only record a video by directly saving it on the file system
                FileOutputOptions.Builder(outputFile).build(),
                // AudioConfig.AUDIO_DISABLED if you want to record without audio
                AudioConfig.create(true),
                // Executor
                ContextCompat.getMainExecutor(applicationContext),
            ) { event ->
                // When the recording is finished, we get this VideoRecordEvent
                if (event is VideoRecordEvent.Finalize) {
                    // If the event has any errors, close the recording
                    if (event.hasError()) {
                        recording?.close()
                        recording = null

                        Toast.makeText(
                            applicationContext, event.cause?.localizedMessage, Toast.LENGTH_LONG
                        ).show()
                        Log.e("Camera", "Video capture failed: ", event.cause)
                    } else {
                        Toast.makeText(
                            applicationContext, "Video capture succeeded", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Check whether we have these permissions or not
    private fun hasRequiredPermissions(): Boolean {
        // Go over all the permissions in this array and check if we have each single permission
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        // Theses are the permissions we want to request
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }

}