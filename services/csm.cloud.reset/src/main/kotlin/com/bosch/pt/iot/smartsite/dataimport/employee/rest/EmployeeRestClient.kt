/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.employee.rest

import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.request.CreateEmployeeResource
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.response.EmployeeListResource
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.response.EmployeeResource
import java.util.UUID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmployeeRestClient {

  @POST("v2/companies/{companyId}/employees")
  fun create(
      @Path("companyId") companyId: UUID,
      @Body createEmployeeResource: CreateEmployeeResource
  ): Call<EmployeeResource>

  @GET("v2/companies/{companyId}/employees")
  fun existingEmployees(
      @Path("companyId") companyId: UUID,
      @Query("page") page: Int
  ): Call<EmployeeListResource>
}
