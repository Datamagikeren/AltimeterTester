package com.example.myapplication.blescanner.model

import com.google.gson.annotations.SerializedName

data class TestResponseObject(
    @SerializedName("JSON Version") val jsonVersion: String,
    val Model: String,
    @SerializedName("Firmware Build Date") val firmwareBuildDate: String,
    @SerializedName("Firmware Version") val firmwareVersion: String,
    @SerializedName("MAC-adresse") val macAddress: String,
    val Measurement: String,
    @SerializedName("Jump Counter") val jumpCounter: String,
    val MBar: String,
    val Temperature: String,
    val Volt: String,
    val Date: String,
    val Time: String,
    var TestPassed: Int = 1,
    @SerializedName("CheckSum.Bytes") val checkSumBytes: String,
    @SerializedName("CheckSum.Sum") val checkSumSum: String
)
