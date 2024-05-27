/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.user.rest

import com.bosch.pt.iot.smartsite.dataimport.user.model.Document
import java.time.Instant
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ConsentsRestClient {

  @POST("v1/documents")
  fun create(@Body createDocumentResource: CreateDocumentResource): Call<Document>

  data class CreateDocumentResource(
      val type: String,
      val country: String,
      val locale: String,
      val client: String,
      val displayName: String,
      val url: String,
      val lastChanged: Instant
  )
}
