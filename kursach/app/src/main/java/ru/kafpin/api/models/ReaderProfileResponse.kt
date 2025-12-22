package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.kafpin.data.models.ProfileEntity

@Parcelize
data class ReaderProfileResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("surname") val surname: String,
    @SerializedName("name") val name: String,
    @SerializedName("patronymic") val patronymic: String?,
    @SerializedName("birthday") val birthday: String,          // String из JSON
    @SerializedName("education") val education: String,
    @SerializedName("profession") val profession: String?,
    @SerializedName("educationalInst") val educationalInst: String?,
    @SerializedName("city") val city: String,
    @SerializedName("street") val street: String,
    @SerializedName("house") val house: Int,
    @SerializedName("buildingHouse") val buildingHouse: String?,
    @SerializedName("flat") val flat: Int?,
    @SerializedName("passportSeries") val passportSeries: Int,
    @SerializedName("passportNumber") val passportNumber: Int,
    @SerializedName("issuedByWhom") val issuedByWhom: String,
    @SerializedName("dateIssue") val dateIssue: String,        // String из JSON
    @SerializedName("consistsOf") val consistsOf: String,      // String из JSON
    @SerializedName("reRegistration") val reRegistration: String?,
    @SerializedName("phone") val phone: String,
    @SerializedName("login") val login: String,
    @SerializedName("mail") val mail: String,
    @SerializedName("admin") val admin: Boolean,
    @SerializedName("isActive") val isActive: Boolean
) : Parcelable {

    // Метод для конвертации в локальную модель (как в бронированиях)
    fun toProfileEntity(userId: Long): ProfileEntity {
        return ProfileEntity(
            userId = userId,
            surname = surname,
            name = name,
            patronymic = patronymic,
            birthday = birthday,
            education = education,
            profession = profession,
            educationalInst = educationalInst,
            city = city,
            street = street,
            house = house,
            buildingHouse = buildingHouse,
            flat = flat,
            passportSeries = passportSeries,
            passportNumber = passportNumber,
            issuedByWhom = issuedByWhom,
            dateIssue = dateIssue,
            consistsOf = consistsOf,
            reRegistration = reRegistration,
            phone = phone,
            login = login,
            mail = mail,
            admin = admin,
            isActive = isActive
        )
    }
}