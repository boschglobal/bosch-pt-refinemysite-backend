/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.rest

import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.request.CreateCompanyResource
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.response.CompanyListResource
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.response.CompanyResource
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CompanyRestClient {

  @POST("v2/companies")
  fun create(@Body createCompanyResource: CreateCompanyResource): Call<CompanyResource>

  @GET("v2/companies") fun existingCompanies(@Query("page") page: Int): Call<CompanyListResource>
}
