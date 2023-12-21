package com.example.myapplication.blescanner.model

import com.google.gson.annotations.SerializedName

data class ExpectedResults(
    val jsonVersion: String = "",
    val Model: String = "",
    val firmwareBuildDate: String = "",
    val firmwareVersion: String = "",
    val macAddress: String = "",
    val Measurement: String = "",
    val jumpCounter: String = "",
    var MBar: String = "",
    val Temperature: String = "",
    val Volt: String = "",
    val Date: String = "",
    val Time: String = "",
    val checkSumBytes: String = "",
    val checkSumSum: String = ""
)

