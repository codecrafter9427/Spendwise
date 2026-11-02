package com.dins.spendwise.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(iconId: String): ImageVector = when (iconId) {
    "restaurant" -> Icons.Outlined.Restaurant
    "directions_car" -> Icons.Outlined.DirectionsCar
    "shopping_bag" -> Icons.Outlined.ShoppingBag
    "receipt" -> Icons.AutoMirrored.Outlined.ReceiptLong
    "favorite" -> Icons.Outlined.FavoriteBorder
    "sports_esports" -> Icons.Outlined.SportsEsports
    "school" -> Icons.Outlined.School
    "flight" -> Icons.Outlined.Flight
    "shopping_basket" -> Icons.Outlined.ShoppingBasket
    "home" -> Icons.Outlined.Home
    "person" -> Icons.Outlined.Person
    "more_horiz" -> Icons.Outlined.MoreHoriz
    "work" -> Icons.Outlined.WorkOutline
    "laptop" -> Icons.Outlined.Laptop
    "business" -> Icons.Outlined.Business
    "trending_up" -> Icons.AutoMirrored.Outlined.TrendingUp
    "card_giftcard" -> Icons.Outlined.CardGiftcard
    "account_balance_wallet" -> Icons.Outlined.AccountBalanceWallet
    else -> Icons.Outlined.MoreHoriz
}

@Composable
fun categoryIconContentDescription(name: String): String = "$name category"
