package com.gltech.guardianwatch.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.gltech.guardianwatch.model.Company
import com.gltech.guardianwatch.model.DemoData
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.ui.theme.GwColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Tactical Map powered by OpenStreetMap (osmdroid).
 * Free, no API key. Satellite tiles via USGS or standard OSM.
 *
 * Soldiers are rendered as colored dots at their GPS positions.
 * Active/alert soldiers get a larger pulsing ring (handled via scale).
 *
 * Default center: IDF training area, Negev (Tze'elim region).
 * Override centerLat/centerLon for your real AO.
 */
@Composable
fun OsmTacMap(
    company: Company,
    activeSquadId: String?,
    alertSoldierId: String?,
    /** GPS positions keyed by soldier ID. If null, demo scatter positions are used. */
    soldierPositions: Map<String, GeoPoint>? = null,
    /** Map center — override for your real AO */
    centerLat: Double = DEFAULT_LAT,
    centerLon: Double = DEFAULT_LON,
    /** Zoom level: 15=city-block, 16=street, 17=building */
    zoomLevel: Double = 16.0,
    onPinClicked: (Soldier) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Initialize osmdroid config once
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "GuardianWatch/1.0"
            // Use app cache dir — no extra permissions needed
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
    }

    // Build stable position map: use provided positions or scatter demo positions
    val positions = remember(soldierPositions, centerLat, centerLon) {
        soldierPositions ?: buildDemoPositions(company, centerLat, centerLon)
    }

    // MapView lifecycle
    val mapView = remember {
        MapView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            // ── TILE SOURCE ───────────────────────────────────────────────────
            // Google Maps Satellite = free global high-res satellite
            val googleSat = object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                "GoogleSat",
                0, 20, 256, ".png",
                arrayOf("https://mt0.google.com", "https://mt1.google.com", "https://mt2.google.com", "https://mt3.google.com")
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val z = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                    return "$baseUrl/vt/lyrs=s&x=$x&y=$y&z=$z"
                }
            }
            setTileSource(googleSat)

            // ── MAP SETTINGS ──────────────────────────────────────────────────
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled   = false
            minZoomLevel = 12.0
            maxZoomLevel = 20.0

            // Initial camera
            controller.setZoom(zoomLevel)
            controller.setCenter(GeoPoint(centerLat, centerLon))
        }
    }

    // Rebuild overlays whenever positions or data changes
    DisposableEffect(positions, activeSquadId, alertSoldierId) {
        mapView.overlays.clear()

        // Close any open info windows
        InfoWindow.closeAllInfoWindowsOn(mapView)

        val density = context.resources.displayMetrics.density

        // 1. Draw Tactical Routes (Tzirim)
        val routes = buildTacticalRoutes(centerLat, centerLon)
        routes.forEach { route ->
            // Draw the route line
            val line = Polyline(mapView).apply {
                setPoints(route.points)
                outlinePaint.color = route.color
                outlinePaint.strokeWidth = 4f * density
                outlinePaint.alpha = 180
            }
            mapView.overlays.add(line)

            // Draw the route name label
            val labelMarker = Marker(mapView).apply {
                position = route.points[1] // Place label near the middle point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = buildTextIcon(context, route.name, density, route.color)
                setOnMarkerClickListener { _, _ -> true } // Consume clicks, do nothing
            }
            mapView.overlays.add(labelMarker)
        }

        // 2. Draw Soldiers
        val allSoldiers = DemoData.allSoldiers()

        allSoldiers.forEach { soldier ->
            val geoPoint = positions[soldier.id] ?: return@forEach
            val isAlert  = soldier.id == alertSoldierId
            val inActiveSquad = soldier.squadId == activeSquadId
            val isCritical = soldier.status == SoldierStatus.CRITICAL

            val marker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "${soldier.last} · ${soldier.role.display}"
                snippet = "HR ${soldier.hr} · SpO₂ ${soldier.spo2} · Risk ${String.format("%.1f", soldier.risk)}"

                // Build custom dot icon
                icon = buildSoldierIcon(
                    context     = context,
                    color       = soldier.status.color,
                    isLarge     = isAlert || isCritical,
                    hasRing     = inActiveSquad || isAlert,
                    ringColor   = if (isAlert) GwColors.critRed else GwColors.infoCyan,
                )

                setOnMarkerClickListener { m, _ ->
                    onPinClicked(soldier)
                    m.showInfoWindow()
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // Draw a bounding polygon for the active squad's area
        if (activeSquadId != null) {
            val squad = DemoData.findSquad(activeSquadId)
            if (squad != null) {
                val squadPoints = squad.soldiers.mapNotNull { positions[it.id] }
                if (squadPoints.size >= 2) {
                    val line = Polyline(mapView).apply {
                        setPoints(squadPoints + squadPoints.first())
                        outlinePaint.color = GwColors.infoCyan.copy(alpha = 0.6f).toArgb()
                        outlinePaint.strokeWidth = 3f
                    }
                    mapView.overlays.add(0, line)
                }
            }
        }

        mapView.invalidate()

        onDispose { /* mapView is reused */ }
    }

    AndroidView(
        factory  = { mapView },
        modifier = modifier.clipToBounds(),
        update   = { /* handled by DisposableEffect */ },
    )

    // Clean up MapView when composable leaves
    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** AO: Khiam area, South Lebanon — 33.3451°N, 35.6124°E */
const val DEFAULT_LAT = 33.3451
const val DEFAULT_LON = 35.6124

/**
 * Scatter 96 demo soldiers in a ~600m × 600m grid around the center.
 * 3 platoons, 4 squads each, 8 soldiers per squad.
 * Each squad forms a loose cluster.
 */
private fun buildDemoPositions(company: Company, centerLat: Double, centerLon: Double): Map<String, GeoPoint> {
    val result = mutableMapOf<String, GeoPoint>()

    // ~0.005 degrees ≈ 500m
    val platoonOffsets = listOf(
        Pair(-0.003, -0.004),   // PLT-1: NW
        Pair( 0.000,  0.000),   // PLT-2: Center
        Pair( 0.003,  0.004),   // PLT-3: SE
    )
    val squadOffsets = listOf(
        Pair(-0.0015, -0.0015),  // Squad A: NW of platoon
        Pair( 0.0015, -0.0015),  // Squad B: NE
        Pair(-0.0015,  0.0015),  // Squad C: SW
        Pair( 0.0015,  0.0015),  // Squad D: SE
    )

    company.platoons.forEachIndexed { pi, platoon ->
        val (pLat, pLon) = platoonOffsets.getOrElse(pi) { Pair(0.0, 0.0) }
        platoon.squads.forEachIndexed { si, squad ->
            val (sLat, sLon) = squadOffsets.getOrElse(si) { Pair(0.0, 0.0) }
            squad.soldiers.forEachIndexed { idx, soldier ->
                // 8 soldiers in a 3m-spaced line formation
                val offset = (idx - 3.5) * 0.00003
                result[soldier.id] = GeoPoint(
                    centerLat + pLat + sLat + offset * 0.5,
                    centerLon + pLon + sLon + offset,
                )
            }
        }
    }
    return result
}

/** Build a bitmap icon for a soldier marker. */
private fun buildSoldierIcon(
    context: android.content.Context,
    color: androidx.compose.ui.graphics.Color,
    isLarge: Boolean,
    hasRing: Boolean,
    ringColor: androidx.compose.ui.graphics.Color,
): android.graphics.drawable.BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val dotRadius  = (if (isLarge) 14f else 8f) * density
    val ringRadius = dotRadius + (4f * density)
    val size       = ((if (hasRing) ringRadius else dotRadius) * 2 + (4f * density)).toInt()

    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx     = size / 2f
    val cy     = size / 2f

    if (hasRing) {
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = ringColor.toArgb()
            style      = Paint.Style.STROKE
            strokeWidth = 3f * density
        }
        canvas.drawCircle(cx, cy, ringRadius, ringPaint)
    }

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, dotRadius, dotPaint)

    // Dark border on dot
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(120, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    canvas.drawCircle(cx, cy, dotRadius, borderPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}

// ─── Route Helpers ────────────────────────────────────────────────────────────

private data class RouteData(val name: String, val points: List<GeoPoint>, val color: Int)

private fun buildTacticalRoutes(centerLat: Double, centerLon: Double): List<RouteData> {
    return listOf(
        RouteData("ציר עכביש", listOf(
            GeoPoint(centerLat + 0.008, centerLon - 0.006),
            GeoPoint(centerLat + 0.003, centerLon - 0.002),
            GeoPoint(centerLat - 0.002, centerLon - 0.001),
            GeoPoint(centerLat - 0.007, centerLon + 0.004)
        ), android.graphics.Color.parseColor("#EABF3A")), // Yellow warning

        RouteData("ציר לקס", listOf(
            GeoPoint(centerLat + 0.005, centerLon + 0.007),
            GeoPoint(centerLat + 0.001, centerLon + 0.003),
            GeoPoint(centerLat - 0.001, centerLon - 0.004),
            GeoPoint(centerLat - 0.004, centerLon - 0.008)
        ), android.graphics.Color.parseColor("#EE343B")), // Red critical

        RouteData("ציר שקד", listOf(
            GeoPoint(centerLat - 0.005, centerLon - 0.005),
            GeoPoint(centerLat - 0.004, centerLon + 0.000),
            GeoPoint(centerLat - 0.005, centerLon + 0.005),
            GeoPoint(centerLat - 0.008, centerLon + 0.009)
        ), android.graphics.Color.parseColor("#43AFC1"))  // Cyan info
    )
}

private fun buildTextIcon(
    context: android.content.Context, 
    text: String, 
    density: Float, 
    borderColor: Int
): android.graphics.drawable.BitmapDrawable {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 11f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val textWidth = paint.measureText(text)
    val textHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
    
    val paddingX = 8f * density
    val paddingY = 4f * density
    
    val width = (textWidth + paddingX * 2).toInt()
    val height = (textHeight + paddingY * 2).toInt()
    
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    
    val rect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
    
    // Background pill (dark)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 12, 8, 6)
    }
    canvas.drawRoundRect(rect, 4f * density, 4f * density, bgPaint)
    
    // Colored Border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawRoundRect(rect, 4f * density, 4f * density, borderPaint)
    
    // Text
    canvas.drawText(text, paddingX, height - paddingY - paint.fontMetrics.descent, paint)
    
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}
