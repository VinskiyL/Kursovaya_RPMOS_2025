package ru.kafpin.api.models

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    @SerializedName("surname")
    val surname: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("patronymic")
    val patronymic: String? = null,

    @SerializedName("birthday")
    val birthday: String,

    @SerializedName("education")
    val education: String,

    @SerializedName("profession")
    val profession: String? = null,

    @SerializedName("educationalInst")
    val educationalInst: String? = null,

    @SerializedName("city")
    val city: String,

    @SerializedName("street")
    val street: String,

    @SerializedName("house")
    val house: Int,

    @SerializedName("buildingHouse")
    val buildingHouse: String? = null,

    @SerializedName("flat")
    val flat: Int? = null,

    @SerializedName("passportSeries")
    val passportSeries: Int,

    @SerializedName("passportNumber")
    val passportNumber: Int,

    @SerializedName("issuedByWhom")
    val issuedByWhom: String,

    @SerializedName("dateIssue")
    val dateIssue: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("login")
    val login: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String,

    @SerializedName("mail")
    val mail: String
)