package com.prismpixelvertex.palatelog.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismpixelvertex.palatelog.core.model.Channel
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.Verdict

@Composable
fun VerdictBadge(
    verdict: Verdict,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (verdict) {
        Verdict.GO -> Quadruple(VerdictGoGreenContainer, VerdictGoGreenText, Icons.Default.CheckCircle, "GO")
        Verdict.NO_GO -> Quadruple(VerdictNoGoRedContainer, VerdictNoGoRedText, Icons.Default.Block, "NO-GO")
        Verdict.CAUTION -> Quadruple(VerdictCautionAmberContainer, VerdictCautionAmberText, Icons.Default.Warning, "CAUTION")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DishVerdictBadge(
    verdict: DishVerdict,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (verdict) {
        DishVerdict.GO -> Triple(VerdictGoGreenContainer, VerdictGoGreenText, "🟢 MUST TRY")
        DishVerdict.NO_GO -> Triple(VerdictNoGoRedContainer, VerdictNoGoRedText, "🔴 AVOID")
        DishVerdict.MEH -> Triple(VerdictCautionAmberContainer, VerdictCautionAmberText, "🟡 MEH / OK")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChannelBadge(
    channel: String,
    modifier: Modifier = Modifier
) {
    val displayName = Channel.getChannelDisplayName(channel)
    val lower = displayName.lowercase()
    val icon = when {
        lower.contains("highway") || lower.contains("drive") -> Icons.Default.DirectionsCar
        lower.contains("zomato") || lower.contains("swiggy") || lower.contains("ownly") ||
        lower.contains("eatclub") || lower.contains("magicpin") || lower.contains("delivery") ||
        lower.contains("ubereats") || lower.contains("door") || lower.contains("dunzo") -> Icons.Default.DeliveryDining
        else -> Icons.Default.Restaurant
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChannelBadge(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    ChannelBadge(channel = channel.displayName, modifier = modifier)
}

@Composable
fun FamilyReactionTag(
    memberName: String,
    reaction: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AmberPrimary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$memberName: $reaction",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VerdictChoiceChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
