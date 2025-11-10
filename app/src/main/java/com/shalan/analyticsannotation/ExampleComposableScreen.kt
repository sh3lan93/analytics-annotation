package com.shalan.analyticsannotation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shalan.analytics.annotation.Param
import com.shalan.analytics.annotation.Track
import com.shalan.analytics.annotation.Trackable

@Composable
fun ExampleComposableScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "This is a composable screen",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Example of tracked composable components
            UserActionCard()

            Spacer(modifier = Modifier.height(16.dp))

            ProductCard(
                productId = "product_123",
                productName = "Sample Product",
            )
        }
    }
}

/**
 * Example of a Composable component that uses @Track annotation
 * to track user interactions within the component.
 */
@Composable
fun UserActionCard() {
    Card(
        modifier = Modifier.padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "User Actions",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    ComposableScreenTracker.handleLikeAction("post_123", "double_tap")
                },
            ) {
                Text("Like Post")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    ComposableScreenTracker.handleShareAction("social_media", "external")
                },
            ) {
                Text("Share")
            }
        }
    }
}

/**
 * Example of a Composable component that tracks when it's displayed
 * using @Track annotation with LaunchedEffect.
 */
@Composable
fun ProductCard(
    productId: String,
    productName: String,
) {
    // Track when this component is displayed
    LaunchedEffect(productId) {
        ComposableScreenTracker.trackProductCardDisplayed(productId, productName, "composable_screen")
    }

    Card(
        modifier = Modifier.padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    ComposableScreenTracker.handleProductClick(productId, productName, "card_button")
                },
            ) {
                Text("View Product")
            }
        }
    }
}

// Tracked functions that can be called from Composables
// Note: @Track functions need to be inside a class annotated with @Trackable

@Trackable
object ComposableScreenTracker {
    @Track(eventName = "user_liked_post", includeGlobalParams = true)
    fun handleLikeAction(
        @Param("post_id") postId: String,
        @Param("interaction_type") interactionType: String,
    ) {
        // Like action logic
        println("User liked post: $postId via $interactionType")
    }

    @Track(eventName = "content_shared", includeGlobalParams = true)
    fun handleShareAction(
        @Param("share_platform") platform: String,
        @Param("share_type") shareType: String,
    ) {
        // Share action logic
        println("Content shared to $platform as $shareType")
    }

    @Track(eventName = "product_card_displayed", includeGlobalParams = true)
    fun trackProductCardDisplayed(
        @Param("product_id") productId: String,
        @Param("product_name") productName: String,
        @Param("display_context") context: String,
    ) {
        // Product display tracking logic
        println("Product card displayed: $productName ($productId) in $context")
    }

    @Track(eventName = "product_clicked", includeGlobalParams = true)
    fun handleProductClick(
        @Param("product_id") productId: String,
        @Param("product_name") productName: String,
        @Param("click_source") clickSource: String,
    ) {
        // Product click logic
        println("Product clicked: $productName ($productId) from $clickSource")
    }
}
