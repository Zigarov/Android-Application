package com.example.macc_test

data class User (
    var fullname: String = "",
    var email: String = "",
    var score: Int = 0,
    var nFollowers: Int = 0,
    var nFollowing: Int = 0,
)

data class Run (
    var user : User,
    val date: com.google.firebase.Timestamp,
    val steps: Int = 0,
    val timestamp: Int = 0,
   )