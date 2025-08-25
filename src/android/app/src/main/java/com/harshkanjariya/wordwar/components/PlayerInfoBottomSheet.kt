import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harshkanjariya.wordwar.network.service.GameData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerInfoBottomSheet(
    activeGame: GameData,
    showSheet: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showSheet) {
        if (showSheet) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Players", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            activeGame.players.forEach { player ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(player.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Claimed Words:", fontSize = 14.sp)
                        player.claimedWords.forEach { word ->
                            Text("- $word", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
