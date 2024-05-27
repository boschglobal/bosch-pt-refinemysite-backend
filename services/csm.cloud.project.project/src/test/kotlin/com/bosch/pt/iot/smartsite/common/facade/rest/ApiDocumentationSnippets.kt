/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

object ApiDocumentationSnippets {

  const val ETAG_HEADER = "ETag"
  const val LINK_SELF_DESCRIPTION = "Link to the resource itself"
  const val LINK_CREATE_DESCRIPTION = "Link to create the resource"
  const val LINK_DELETE_DESCRIPTION = "Link to delete the resource"
  const val LINK_NEXT_DESCRIPTION = "Link to the next page/slice"
  const val LINK_PREVIOUS_DESCRIPTION = "Link to the previous page/slice"

  val IF_MATCH_HEADER_DESCRIPTOR: HeaderDescriptor =
      headerWithName(IF_MATCH)
          .description("Entity tag of the resource needed for update or delete.")

  val ETAG_HEADER_DESCRIPTOR: HeaderDescriptor =
      headerWithName(ETAG_HEADER)
          .description(
              "Entity tag of the created or updated resource, needed for further updates of the resource")

  val LOCATION_HEADER_DESCRIPTOR: HeaderDescriptor =
      headerWithName(LOCATION).description("Location of the created resource")

  val ABSTRACT_RESOURCE_FIELD_DESCRIPTORS =
      arrayOf(
          fieldWithPath("id").description("ID of the resource"),
          fieldWithPath("version").description("Version of the resource"),
          fieldWithPath("createdBy.displayName")
              .description("Name of the user that created the resource"),
          fieldWithPath("createdBy.id").description("ID of the user that created the resource"),
          fieldWithPath("createdBy.picture")
              .description("The link to get the created user picture")
              .type(STRING)
              .optional(),
          fieldWithPath("createdDate").description("Date of resource creation"),
          fieldWithPath("lastModifiedBy.displayName")
              .description("Name of the user that modified the resource last"),
          fieldWithPath("lastModifiedBy.id")
              .description("ID of the user that modified the resource last"),
          fieldWithPath("lastModifiedDate").description("Date of the last modification"))

  val PROJECT_REFERENCE_FIELD_DESCRIPTORS =
      arrayOf(
          fieldWithPath("project.id").description("ID of the referenced project").type(STRING),
          fieldWithPath("project.displayName")
              .description("Name of the referenced project")
              .type(STRING))

  val CRAFT_REFERENCE_FIELD_DESCRIPTORS =
      arrayOf(
          fieldWithPath("craft.id")
              .description("ID of the referenced project craft")
              .optional()
              .type(STRING),
          fieldWithPath("craft.displayName")
              .description("Name of the referenced craft")
              .optional()
              .type(STRING),
          fieldWithPath("craft.color")
              .description("Color of the referenced craft")
              .optional()
              .type(STRING))

  val WORK_AREA_REFERENCE_FIELD_DESCRIPTORS =
      arrayOf(
          fieldWithPath("workArea.id")
              .description("ID of the referenced work area")
              .optional()
              .type(STRING),
          fieldWithPath("workArea.displayName")
              .description("Name of the referenced work area")
              .optional()
              .type(STRING))

  val CREATOR_PARTICIPANT_REFERENCE_FIELD_DESCRIPTORS =
      arrayOf(
          fieldWithPath("creator.id")
              .description("ID of the participant that created the resource")
              .optional()
              .type(STRING),
          fieldWithPath("creator.displayName")
              .description("Name of the participant that created the resource")
              .optional()
              .type(STRING),
          fieldWithPath("creator.picture")
              .description("URI of the participants picture that created the resource")
              .optional()
              .type(STRING))
}
