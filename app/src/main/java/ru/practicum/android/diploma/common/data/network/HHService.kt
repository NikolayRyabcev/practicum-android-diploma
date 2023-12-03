package ru.practicum.android.diploma.common.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import ru.practicum.android.diploma.common.data.dto.HHSearchResponce

interface HHService {

    @GET("vacancies")
    suspend fun searchVacancies(
        @Header("Authorization") token: String,
        @Query("text") text: String = "",
        @Query("page") page: Int = 0,
        @Query("per_page") perPage: Int = 20,
        @Query("area") area: Int? = null,
        @Query("industry") industry: Int? = null,
        @Query("salary") salary: Int? = null,
        @Query("only_with_salary") onlyWithSalary: Boolean = false
    ): HHSearchResponce
}
