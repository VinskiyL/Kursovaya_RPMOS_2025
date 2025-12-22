package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.kafpin.data.models.ProfileEntity

@Parcelize
data class ReaderUpdateRequest(
    @SerializedName("surname") val surname: String,
    @SerializedName("name") val name: String,
    @SerializedName("patronymic") val patronymic: String?,
    @SerializedName("birthday") val birthday: String,          // String для API
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
    @SerializedName("dateIssue") val dateIssue: String,        // String для API
    @SerializedName("phone") val phone: String,
    @SerializedName("mail") val mail: String
) : Parcelable {

    // Фабричный метод из локальной модели (как в OrderCreateRequest)
    companion object {
        fun fromProfileEntity(profile: ProfileEntity): ReaderUpdateRequest {
            return ReaderUpdateRequest(
                surname = profile.surname,
                name = profile.name,
                patronymic = profile.patronymic,
                birthday = profile.birthday,
                education = profile.education,
                profession = profile.profession,
                educationalInst = profile.educationalInst,
                city = profile.city,
                street = profile.street,
                house = profile.house,
                buildingHouse = profile.buildingHouse,
                flat = profile.flat,
                passportSeries = profile.passportSeries,
                passportNumber = profile.passportNumber,
                issuedByWhom = profile.issuedByWhom,
                dateIssue = profile.dateIssue,
                phone = profile.phone,
                mail = profile.mail
            )
        }
    }
}