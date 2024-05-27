/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

@LibraryCandidate
object ApiDocumentationSnippets {

  const val LINK_SELF_DESCRIPTION = "Link to the resource itself"

  val ID_AND_VERSION_FIELD_DESCRIPTORS =
      arrayOf(
          fieldWithPath("id").description("ID of the resource"),
          fieldWithPath("version").description("Version of the resource"))

  val ABSTRACT_RESOURCE_FIELD_DESCRIPTORS =
      arrayOf(
          *ID_AND_VERSION_FIELD_DESCRIPTORS,
          fieldWithPath("createdBy.displayName")
              .description("Name of the user that created the resource"),
          fieldWithPath("createdBy.id").description("ID of the user that created the resource"),
          fieldWithPath("createdDate").description("Date of resource creation"),
          fieldWithPath("lastModifiedBy.displayName")
              .description("Name of the user that modified the resource last"),
          fieldWithPath("lastModifiedBy.id")
              .description("ID of the user that modified the resource last"),
          fieldWithPath("lastModifiedDate").description("Date of the last modification"))

  val LOCATION_HEADER_DESCRIPTOR: HeaderDescriptor =
      headerWithName(HttpHeaders.LOCATION).description("Location of the created resource")
}
