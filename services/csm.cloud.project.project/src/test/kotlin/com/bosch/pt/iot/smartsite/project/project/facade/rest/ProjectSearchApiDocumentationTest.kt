/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectApiDocumentationTest.Companion.PROJECT_RESPONSE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SearchProjectListResource
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService.Companion.PROJECT_SEARCH_ALLOWED_SORTING_PROPERTIES
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectSearchApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val user1 by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val user2 by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val project1 by lazy {
    repositories.findProject(getIdentifier("project1").asProjectId())!!
  }
  private val project2 by lazy {
    repositories.findProject(getIdentifier("project2").asProjectId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .setUserContext(name = "system")
        .submitCompany(asReference = "company", eventType = CompanyEventEnumAvro.UPDATED) {
          it.name = "company"
        }
        .setUserContext(name = "userCsm1")
        .submitProject(asReference = "project1") {
          it.title = "aProject"
          it.category = ProjectCategoryEnumAvro.OB
          it.client = "Client"
          it.description = "Description"
        }
        .submitParticipantG3(asReference = randomString()) {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .setUserContext(name = "userCsm2")
        .submitProject(asReference = "project2") {
          it.title = "bProject"
          it.category = ProjectCategoryEnumAvro.OB
          it.client = "Client"
          it.description = "Description"
        }
        .submitParticipantG3(asReference = randomString()) {
          it.user = getByReference("userCsm2")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    projectEventStoreUtils.reset()
  }

  @Test
  fun verifyAndDocumentSearchProjects() {
    setAuthentication("admin")
    val searchResource = SearchProjectListResource("project", "c", "e")

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/search"))
                    .param("sort", *PROJECT_SEARCH_ALLOWED_SORTING_PROPERTIES.keys.toTypedArray())
                    .param("size", "20")
                    .param("page", "0"),
                searchResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.projects.length()").value(2),
            jsonPath("$.projects[0].title").value(project1.title),
            jsonPath("$.projects[0].createdBy.displayName").value(user1.getDisplayName()),
            jsonPath("$.projects[1].title").value(project2.title),
            jsonPath("$.projects[1].createdBy.displayName").value(user2.getDisplayName()),
        )
        .andDo(
            document(
                "projects/document-search-project",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    buildSortingAndPagingParameterDescriptors(
                        PROJECT_SEARCH_ALLOWED_SORTING_PROPERTIES.keys)),
                PROJECT_SEARCH_REQUEST_FIELDS,
                PROJECT_SEARCH_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyEmpty()
  }

  companion object {

    private fun buildPagedProjectListResponseFields(itemsFieldDescriptors: List<FieldDescriptor>) =
        responseFields(
                fieldWithPath("projects[]").description("List of page items").type(ARRAY),
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                fieldWithPath("totalPages").description("Total number of available pages"),
                fieldWithPath("totalElements").description("Total number of items available"),
                fieldWithPath("userActivated")
                    .description("Flag indicating if a user was assigned to a company yet."),
                subsectionWithPath("_links").optional().ignored())
            .andWithPrefix("projects[].", itemsFieldDescriptors)

    private val PROJECT_SEARCH_REQUEST_CONSTRAINED_FIELD =
        ConstrainedFields(SearchProjectListResource::class.java)

    private val PROJECT_SEARCH_REQUEST_FIELDS =
        requestFields(
            listOf(
                PROJECT_SEARCH_REQUEST_CONSTRAINED_FIELD.withPath("title")
                    .description("Title of the project to be searched for")
                    .optional()
                    .type(STRING),
                PROJECT_SEARCH_REQUEST_CONSTRAINED_FIELD.withPath("company")
                    .description("Name of the company of the participant")
                    .optional()
                    .type(STRING),
                PROJECT_SEARCH_REQUEST_CONSTRAINED_FIELD.withPath("creator")
                    .description(
                        "Firstname and/or Lastname of the creator of the project to be searched for")
                    .optional()
                    .type(STRING)))

    private val PROJECT_SEARCH_RESPONSE_FIELDS =
        buildPagedProjectListResponseFields(PROJECT_RESPONSE_FIELD_DESCRIPTORS)
  }
}
