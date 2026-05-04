package com.kreedaankana.navigation

sealed class Screen(val route: String) {
    object Onboarding    : Screen("onboarding")
    object Home          : Screen("home")
    object Calendar      : Screen("calendar")
    object BookSlot      : Screen("book_slot")
    object ChallengeBoard: Screen("challenge_board")
    object PostChallenge : Screen("post_challenge")
    object ScoreWall     : Screen("score_wall")
    object PostResult    : Screen("post_result")
    object Leaderboard   : Screen("leaderboard")
    object Profile       : Screen("profile")
}
