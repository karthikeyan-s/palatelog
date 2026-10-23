package com.prismpixelvertex.palatelog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.PalateLogTheme
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.update.InAppUpdateHelper
import com.prismpixelvertex.palatelog.feature.navigation.PalateLogNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var updateHelper: InAppUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateHelper = InAppUpdateHelper(applicationContext)
        updateHelper.registerListener()
        updateHelper.checkForAppUpdate(this@MainActivity)

        setContent {
            PalateLogTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions handled */ }

                val isUpdateDownloaded by updateHelper.isUpdateDownloaded.collectAsState()

                LaunchedEffect(Unit) {
                    val fineLocationGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!fineLocationGranted) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PalateLogNavGraph()

                        AnimatedVisibility(
                            visible = isUpdateDownloaded,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DownloadDone,
                                            contentDescription = null,
                                            tint = VerdictGoGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "PalateLog update downloaded! 🟢",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Tap to restart & apply latest version",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { updateHelper.completeUpdate() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Restart App", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHelper.checkForAppUpdate(this@MainActivity)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHelper.unregisterListener()
    }
}
