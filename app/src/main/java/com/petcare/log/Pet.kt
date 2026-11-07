package com.petcare.log

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Pet(
    var id: String = "",
    var name: String = "",
    var type: String = "Dog",
    var age: Int = 0,
    var nextVetVisit: String = "",
    var notes: String = "",
    var createdAt: Long = 0L
)
