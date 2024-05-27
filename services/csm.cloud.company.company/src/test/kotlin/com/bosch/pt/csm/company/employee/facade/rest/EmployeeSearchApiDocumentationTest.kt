/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.facade.rest

import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.common.facade.rest.ApiDocumentationSnippets.LINK_SELF_DESCRIPTION
import com.bosch.pt.csm.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEE_SEARCH_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.resource.request.SearchEmployeesFilterResource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Test and Document Employee Search API")
class EmployeeSearchApiDocumentationTest : AbstractApiDocumentationTest() {

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
  }

  @Test
  fun `verify and document search employees`() {
    eventStreamGenerator
        .submitCompany("company") { it.name = "My company" }
        .submitUser("user1") {
          it.firstName = "Max"
          it.lastName = "Mustermann"
          it.email = "max.mustermann@web.de"
        }
        .submitEmployee("employee1") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitUser("user2") {
          it.firstName = "Maya"
          it.lastName = "Mustermann"
          it.email = "maya.mustermann@web.de"
        }
        .submitEmployee("employee2") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitUser("user3") {
          it.firstName = "Mac and no"
          it.lastName = "Admin"
          it.email = "mac.admin@gmail.com"
        }

    val filter = SearchEmployeesFilterResource(email = "ma")

    mockMvc
        .perform(requestBuilder(post(latestVersionOf(EMPLOYEE_SEARCH_ENDPOINT_PATH)), filter))
        .andExpect(status().isOk)
        .andExpect(jsonPath("items.size()").value("3"))
        .andDo(
            document(
                "companies/document-search-company-employees",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                sortingAndPagingRequestParameter(
                    SEARCH_USERS_WITH_EMPLOYEES_ALLOWED_SORTING_PROPERTIES.keys),
                requestFields(SEARCH_EMPLOYEES_REQUEST_FIELD_DESCRIPTORS),
                buildPagedItemsListResponseFields(EMPLOYEE_SEARCH_RESPONSE_FIELDS_DESCRIPTORS),
                links(EMPLOYEE_LIST_LINK_DESCRIPTORS)))
  }

  companion object {
    private val searchEmployeesFilterResourceField =
        ConstrainedFields(SearchEmployeesFilterResource::class.java)

    private val SEARCH_EMPLOYEES_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            searchEmployeesFilterResourceField
                .withPath("name")
                .description(
                    "Search term for the name of the user. A \"starts-with\" logic is applied here.")
                .type(JsonFieldType.STRING)
                .optional(),
            searchEmployeesFilterResourceField
                .withPath("email")
                .description(
                    "Search term for the email of the user. A \"starts-with\" logic is applied here.")
                .type(JsonFieldType.STRING)
                .optional(),
            searchEmployeesFilterResourceField
                .withPath("companyName")
                .description(
                    "Search term for the name of the company, the user is assigned to. " +
                        "A \"starts-with\" logic is applied here.")
                .type(JsonFieldType.STRING)
                .optional())

    private val EMPLOYEE_SEARCH_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            fieldWithPath("user.id").description("ID of the employee's user"),
            fieldWithPath("user.displayName").description("Full name of the user"),
            fieldWithPath("user.admin").description("Flag if user has administrative rights"),
            fieldWithPath("user.locked").description("Flag if user is locked"),
            fieldWithPath("user.createdAt").description("Date the user was registered"),
            fieldWithPath("user.email").description("Email of the user"),
            fieldWithPath("user.gender").description("Gender of the user"),
            fieldWithPath("employee.id").description("ID of the employee").optional(),
            fieldWithPath("employee.displayName").description("Name of the employee").optional(),
            fieldWithPath("company.id").description("ID of the employee's company").optional(),
            fieldWithPath("company.displayName")
                .description("Name of the employee's company")
                .optional(),
            subsectionWithPath("_links").ignored())

    private val EMPLOYEE_LIST_LINK_DESCRIPTORS =
        listOf(linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION))

    private val SEARCH_USERS_WITH_EMPLOYEES_ALLOWED_SORTING_PROPERTIES =
        mapOf(
            Pair("user.createdAt", "createdDate"),
            Pair("user.email", "email"),
            Pair("user.firstName", "firstName"),
            Pair("user.lastName", "lastName"),
            Pair("company.displayName", "c.name"))
  }
}
