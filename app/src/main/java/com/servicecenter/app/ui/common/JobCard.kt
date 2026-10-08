package com.servicecenter.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servicecenter.app.core.Money
import com.servicecenter.app.data.local.dao.JobSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSummaryCard(summary: JobSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${summary.job.jobNo} · ${summary.job.model}", fontWeight = FontWeight.Medium)
                Pill(summary.statusName, statusTone(summary.job.statusId))
            }
            Text(
                "${summary.customerName} · ${formatDateTime(summary.job.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!summary.statusIsFinal && summary.balance > 0) {
                Text("Balance ${Money.format(summary.balance)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
