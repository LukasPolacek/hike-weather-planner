package cz.polacek.hikeweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.weather.WeatherCondition
import cz.polacek.hikeweather.ui.theme.AlertRed
import cz.polacek.hikeweather.ui.theme.DifficultyEasy
import cz.polacek.hikeweather.ui.theme.DifficultyHard
import cz.polacek.hikeweather.ui.theme.DifficultyMedium

// --- Pocasi ---

fun WeatherCondition.icon(): ImageVector = when (this) {
    WeatherCondition.CLEAR -> Icons.Filled.WbSunny
    WeatherCondition.PARTLY_CLOUDY -> Icons.Filled.FilterDrama
    WeatherCondition.CLOUDY -> Icons.Filled.Cloud
    WeatherCondition.FOG -> Icons.Filled.Dehaze
    WeatherCondition.DRIZZLE -> Icons.Filled.Grain
    WeatherCondition.RAIN -> Icons.Filled.WaterDrop
    WeatherCondition.SNOW -> Icons.Filled.AcUnit
    WeatherCondition.THUNDERSTORM -> Icons.Filled.Thunderstorm
}

fun WeatherCondition.tint(): Color = when (this) {
    WeatherCondition.CLEAR -> Color(0xFFF2A516)
    WeatherCondition.PARTLY_CLOUDY -> Color(0xFFE59A3A)
    WeatherCondition.CLOUDY, WeatherCondition.FOG -> Color(0xFF8A9590)
    WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> Color(0xFF3D7FC2)
    WeatherCondition.SNOW -> Color(0xFF7FB6D9)
    WeatherCondition.THUNDERSTORM -> Color(0xFF6B4FA0)
}

@Composable
fun WeatherIcon(condition: WeatherCondition, modifier: Modifier = Modifier, size: Int = 28) {
    Icon(
        imageVector = condition.icon(),
        contentDescription = null,
        tint = condition.tint(),
        modifier = modifier.size(size.dp)
    )
}

// --- Narocnost ---

fun Difficulty.color(): Color = when (this) {
    Difficulty.EASY -> DifficultyEasy
    Difficulty.MEDIUM -> DifficultyMedium
    Difficulty.HARD -> DifficultyHard
}

@Composable
fun DifficultyBadge(difficulty: Difficulty, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(difficulty.color().copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(difficulty.color()))
        Spacer(Modifier.width(6.dp))
        Text(difficulty.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- Karta vyletu ---

/** Karta vyletu v seznamu: barevny pruh podle narocnosti, datum, pocasi a pripadne upozorneni */
@Composable
fun TripCard(trip: Trip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(trip.difficulty.color())
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(trip.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (trip.detail.isNotBlank()) {
                    Text(
                        trip.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyBadge(trip.difficulty)
                    trip.plannedDate?.let { date ->
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "${Formatters.shortDay(date)} · ${Formatters.relativeDay(date)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                trip.weatherSummary?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trip.alertTitle?.let { AlertLine(it, trip.alertDetail) }
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun AlertLine(title: String, detail: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AlertRed.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, null, tint = AlertRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (detail.isNullOrBlank()) title else "$title · $detail",
            style = MaterialTheme.typography.labelMedium,
            color = AlertRed
        )
    }
}

// --- Obecne ---

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Prazdny stav s ikonou, nadpisem a popisem */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
