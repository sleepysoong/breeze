package com.sleepysoong.breeze.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.sleepysoong.breeze.ui.theme.BreezeTheme

private const val TAG = "SafeGoogleMap"

sealed class MapInitState {
    object Loading : MapInitState()
    object Ready : MapInitState()
    data class Error(val message: String) : MapInitState()
}

@Composable
fun rememberMapInitState(): MapInitState {
    val context = LocalContext.current
    var state by remember { mutableStateOf<MapInitState>(MapInitState.Loading) }
    
    LaunchedEffect(Unit) {
        state = initializeGoogleMaps(context)
    }
    
    return state
}

private fun initializeGoogleMaps(context: Context): MapInitState {
    return try {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            val errorMessage = when (resultCode) {
                ConnectionResult.SERVICE_MISSING -> "Google Play Services가 설치되어 있지 않아요"
                ConnectionResult.SERVICE_UPDATING -> "Google Play Services 업데이트 중이에요"
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Google Play Services 업데이트가 필요해요"
                ConnectionResult.SERVICE_DISABLED -> "Google Play Services가 비활성화되어 있어요"
                ConnectionResult.SERVICE_INVALID -> "Google Play Services가 유효하지 않아요"
                else -> "Google Play Services 오류 (코드: $resultCode)"
            }
            Log.e(TAG, "Google Play Services unavailable: $errorMessage")
            return MapInitState.Error(errorMessage)
        }
        
        val initResult = MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { renderer ->
            Log.d(TAG, "Maps renderer: $renderer")
        }
        
        Log.d(TAG, "MapsInitializer result: $initResult")
        MapInitState.Ready
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Google Maps", e)
        MapInitState.Error("지도 초기화 실패: ${e.message ?: "알 수 없는 오류"}")
    }
}

@Composable
fun SafeGoogleMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 15f)
    },
    properties: MapProperties = MapProperties(mapType = MapType.NORMAL),
    uiSettings: MapUiSettings = MapUiSettings(),
    onMapLoaded: () -> Unit = {},
    content: @Composable @GoogleMapComposable () -> Unit = {}
) {
    val mapInitState = rememberMapInitState()
    
    when (mapInitState) {
        is MapInitState.Loading -> {
            MapPlaceholder(
                modifier = modifier,
                title = "지도 로딩 중",
                message = "잠시만 기다려주세요..."
            )
        }
        is MapInitState.Error -> {
            MapPlaceholder(
                modifier = modifier,
                title = "지도를 표시할 수 없어요",
                message = mapInitState.message
            )
        }
        is MapInitState.Ready -> {
            DisposableEffect(Unit) {
                onDispose {
                    Log.d(TAG, "SafeGoogleMap disposed")
                }
            }
            
            GoogleMap(
                modifier = modifier,
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                onMapLoaded = {
                    try {
                        onMapLoaded()
                    } catch (e: Exception) {
                        Log.e(TAG, "onMapLoaded callback error", e)
                    }
                },
                content = content
            )
        }
    }
}

@Composable
fun MapPlaceholder(
    modifier: Modifier = Modifier,
    title: String,
    message: String
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BreezeTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = BreezeTheme.typography.bodyLarge,
                color = BreezeTheme.colors.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = BreezeTheme.typography.bodySmall,
                color = BreezeTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}
