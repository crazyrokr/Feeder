package com.nononsenseapps.feeder.ui.compose.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.compose.text.annotatedStringResource
import com.nononsenseapps.feeder.ui.compose.theme.keyline1Padding
import kotlinx.coroutines.launch

@Composable
@Preview(showBackground = true)
fun NothingToRead(
    modifier: Modifier = Modifier,
    onOpenOtherFeed: suspend () -> Unit = {},
    onAddFeed: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = keyline1Padding)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.empty_feed_top),
                style = MaterialTheme.typography.h4.merge(
                    TextStyle(fontWeight = FontWeight.Light)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = TextFieldDefaults.MinHeight)
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            onOpenOtherFeed()
                        }
                    }
            ) {
                Text(
                    text = annotatedStringResource(id = R.string.empty_feed_open),
                    style = MaterialTheme.typography.h4.merge(
                        TextStyle(fontWeight = FontWeight.Light)
                    ),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = TextFieldDefaults.MinHeight)
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            onAddFeed()
                        }
                    }
            ) {
                Text(
                    text = annotatedStringResource(id = R.string.empty_feed_add),
                    style = MaterialTheme.typography.h4.merge(
                        TextStyle(fontWeight = FontWeight.Light)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
