package org.khpylon.ringcontrol

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

// Composable to display control switches and their descriptions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSwitchRow(
    tooltip: String,
    desc: AnnotatedString,
    isChecked: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick:  () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    )
    {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip =
                {
                    PlainTooltip {
                        Text(tooltip)
                    }
                },
            state = rememberTooltipState()
        ) {
            Text(
                text = desc,
                modifier = Modifier
                    .padding(10.dp)
            )
        }
        Spacer(Modifier.weight(1f))  // separate text and toggle switch
        Box(
            modifier = Modifier
            .combinedClickable(
                onClick = {onClick(!isChecked)},
                onLongClick = onLongClick,
            )
        ) {
            Switch(
                checked = isChecked,
                onCheckedChange = null
            )
        }
    }
}
