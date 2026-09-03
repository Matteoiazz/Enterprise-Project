package com.tripify.tripify_android.notification.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.communication.data.model.NotificationModel
import com.tripify.tripify_android.notification.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onBackClick: () -> Unit = {}
) {
    val notifications = viewModel.notifications
    val isLoading = viewModel.isLoading

    LaunchedEffect(notifications) {
        notifications.filter { !it.isRead }.forEach { notification ->
            viewModel.markAsRead(notification.id)
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text("NOTIFICHE", style = CatalogType.Wordmark, color = CatalogColors.Ink)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Torna indietro",
                                tint = CatalogColors.Ink
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = CatalogColors.Surface
                    )
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CatalogColors.AccentDark
                )
            } else if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nessuna notifica",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sei perfettamente aggiornato su tutto!",
                        style = CatalogType.Body,
                        color = CatalogColors.InkMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = CatalogSpacing.Gutter,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onItemClick = {
                                if (!notification.isRead) {
                                    viewModel.markAsRead(notification.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationModel,
    onItemClick: () -> Unit
) {
    val backgroundColor = if (notification.isRead) {
        CatalogColors.Surface
    } else {
        CatalogColors.SurfaceMuted
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CatalogShapes.Card)
            .clickable { onItemClick() },
        color = backgroundColor,
        shape = CatalogShapes.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CatalogShapes.Badge)
                    .background(if (notification.isRead) CatalogColors.SurfaceMuted else CatalogColors.AccentDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (notification.isRead) CatalogColors.InkSubtle else CatalogColors.AccentDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    style = CatalogType.LabelStrong,
                    color = CatalogColors.Ink
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    style = CatalogType.Body,
                    color = CatalogColors.InkSubtle
                )
                Spacer(modifier = Modifier.height(6.dp))

                val formattedDate = try {
                    val parsed = java.time.LocalDateTime.parse(notification.createdAt)
                    parsed.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                } catch (e: Exception) {
                    notification.createdAt
                }

                Text(
                    text = formattedDate,
                    style = CatalogType.Caption,
                    color = CatalogColors.InkMuted
                )
            }
        }
    }
}