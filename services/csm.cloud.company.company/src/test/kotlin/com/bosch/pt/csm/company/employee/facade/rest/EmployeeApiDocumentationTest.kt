/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithStreetAddress
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.FEMALE
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.csm.common.facade.rest.ApiDocumentationSnippets.ID_AND_VERSION_FIELD_DESCRIPTORS
import com.bosch.pt.csm.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.common.i18n.Key.USER_DELETED
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEES_BY_COMPANY_ID_AND_EMPLOYEE_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.PATH_VARIABLE_COMPANY_ID
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.PATH_VARIABLE_EMPLOYEE_ID
import com.bosch.pt.csm.company.employee.facade.rest.resource.request.SaveEmployeeResource
import com.bosch.pt.csm.company.employee.query.EmployeeQueryService.Companion.EMPLOYEE_BY_COMPANY_ALLOWED_SORTING_PROPERTIES
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import java.util.Locale
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.MediaTypes
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Test and Document Employee API")
class EmployeeApiDocumentationTest : AbstractApiDocumentationTest() {

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
    eventStreamGenerator
        .submitCompanyWithStreetAddress("company") { it.name = "My company" }
        .submitUser("user1") {
          it.firstName = FIRST_NAME_1
          it.lastName = LAST_NAME_1
          it.gender = MALE
        }
        .submitEmployee("employee1") { it.roles = listOf(CSM) }
        .submitUser("user2") {
          it.firstName = FIRST_NAME_2
          it.lastName = LAST_NAME_2
          it.gender = FEMALE
        }
        .submitEmployee("employee2") { it.roles = listOf(FM) }
        .submitUser("user3") {
          it.firstName = FIRST_NAME_3
          it.lastName = LAST_NAME_3
          it.gender = MALE
        }
        .submitEmployee("employee3") { it.roles = listOf(FM) }
        .submitUser("user4") {
          it.firstName = FIRST_NAME_4
          it.lastName = LAST_NAME_4
          it.gender = MALE
        }
        .submitEmployee("employee4") { it.roles = listOf(FM) }

    companyEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create company employee`() {
    eventStreamGenerator.submitUser("user_employee") {
      it.firstName = "FirstName"
      it.lastName = "LastName"
      it.gender = MALE
    }

    val companyIdentifier = eventStreamGenerator.getIdentifier("company").asCompanyId()
    val userEmployee =
        repositories.userProjectionRepository.findOneById(
            eventStreamGenerator.getIdentifier("user_employee").asUserId())!!

    val saveEmployeeResource = SaveEmployeeResource(userEmployee.id, listOf(EmployeeRoleEnum.FM))

    val expectedUrl =
        latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .replace("{$PATH_VARIABLE_COMPANY_ID}", companyIdentifier.toString())

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH),
                    companyIdentifier.toString()),
                saveEmployeeResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, containsString(expectedUrl)))
        .andDo(
            document(
                "companies/document-create-company-employee",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("companyId").description("ID of the company")),
                employeeRequestFields,
                responseHeaders(
                    headerWithName(LOCATION).description("Location of created employee resource")),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(LINK_DELETE).description(LINK_DELETE_DESCRIPTION)),
                responseFields(EMPLOYEE_RESPONSE_FIELDS_DESCRIPTORS)))

    companyEventStoreUtils
        .verifyContainsAndGet(EmployeeEventAvro::class.java, EmployeeEventEnumAvro.CREATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier).isNotEmpty
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
            assertThat(aggregate.roles[0]).isEqualTo(FM)
            assertThat(aggregate.user.type).isEqualTo(USER.value)
            assertThat(aggregate.user.identifier).isEqualTo(userEmployee.id.toString())
            assertThat(aggregate.user.version).isEqualTo(userEmployee.version)
            assertThat(aggregate.company.type).isEqualTo(COMPANY.value)
            assertThat(aggregate.company.identifier).isEqualTo(companyIdentifier.toString())
            assertThat(aggregate.company.version).isEqualTo(0)
          }
        }
  }

  @Test
  fun `verify and document create company employee with identifier`() {
    eventStreamGenerator.submitUser("user_employee") {
      it.firstName = "FirstName"
      it.lastName = "LastName"
      it.gender = MALE
    }

    val companyIdentifier = eventStreamGenerator.getIdentifier("company").asCompanyId()
    val userEmployee =
        repositories.userProjectionRepository.findOneById(
            eventStreamGenerator.getIdentifier("user_employee").asUserId())!!

    val saveEmployeeResource = SaveEmployeeResource(userEmployee.id, listOf(EmployeeRoleEnum.FM))

    val employeeIdentifier = randomUUID()
    val expectedUrl =
        latestVersionOf(EMPLOYEES_BY_COMPANY_ID_AND_EMPLOYEE_ID_ENDPOINT_PATH)
            .replace("{$PATH_VARIABLE_COMPANY_ID}", companyIdentifier.toString())
            .replace("{$PATH_VARIABLE_EMPLOYEE_ID}", employeeIdentifier.toString())

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(EMPLOYEES_BY_COMPANY_ID_AND_EMPLOYEE_ID_ENDPOINT_PATH),
                    companyIdentifier.toString(),
                    employeeIdentifier),
                saveEmployeeResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, containsString(expectedUrl)))
        .andDo(
            document(
                "companies/document-create-company-employee-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("companyId").description("ID of the company"),
                    parameterWithName("employeeId")
                        .description("The new ID of the employee")
                        .optional()),
                employeeRequestFields,
                responseHeaders(
                    headerWithName(LOCATION).description("Location of created employee resource")),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(LINK_DELETE).description(LINK_DELETE_DESCRIPTION)),
                responseFields(EMPLOYEE_RESPONSE_FIELDS_DESCRIPTORS)))

    companyEventStoreUtils
        .verifyContainsAndGet(EmployeeEventAvro::class.java, EmployeeEventEnumAvro.CREATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isEqualTo(employeeIdentifier.toString())
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
            assertThat(aggregate.roles[0]).isEqualTo(FM)
            assertThat(aggregate.user.type).isEqualTo(USER.value)
            assertThat(aggregate.user.identifier).isEqualTo(userEmployee.id.toString())
            assertThat(aggregate.user.version).isEqualTo(userEmployee.version)
            assertThat(aggregate.company.type).isEqualTo(COMPANY.value)
            assertThat(aggregate.company.identifier).isEqualTo(companyIdentifier.toString())
            assertThat(aggregate.company.version).isEqualTo(0)
          }
        }
  }

  @Test
  fun `verify create company employee with no role`() {
    eventStreamGenerator.submitUser("user_employee") {
      it.firstName = "FirstName"
      it.lastName = "LastName"
      it.gender = MALE
    }

    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!
    val userEmployee =
        repositories.userProjectionRepository.findOneById(
            eventStreamGenerator.getIdentifier("user_employee").asUserId())!!

    val saveEmployeeResource = SaveEmployeeResource(userEmployee.id, emptyList())

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH), company.id?.toString()),
                saveEmployeeResource))
        .andExpect(status().isBadRequest)
  }

  @Test
  fun `verify create company employee with non existing user`() {
    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    val saveEmployeeResource = SaveEmployeeResource(UserId(), listOf(EmployeeRoleEnum.FM))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH),
                    company.identifier.toString()),
                saveEmployeeResource))
        .andExpect(status().isConflict)
  }

  @Test
  fun `verify and document find company employees`() {
    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    mockMvc
        .perform(
            requestBuilder(
                get(
                        latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH),
                        company.identifier.toString())
                    .param("size", "3")
                    .param("page", "0")))
        .andExpect(status().isOk)
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("items").isArray,
            jsonPath("items.length()").value(3),
            jsonPath("items[0].user.displayName").value("$FIRST_NAME_1 $LAST_NAME_1"),
            jsonPath("items[1].user.displayName").value("$FIRST_NAME_4 $LAST_NAME_4"),
            jsonPath("items[2].user.displayName").value("$FIRST_NAME_3 $LAST_NAME_3"))
        .andDo(
            document(
                "companies/document-get-company-employees",
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("companyId").description("ID of the company")),
                sortingAndPagingRequestParameter(
                    EMPLOYEE_BY_COMPANY_ALLOWED_SORTING_PROPERTIES.keys),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION)),
                buildPagedItemsListResponseFields(EMPLOYEE_RESPONSE_FIELDS_DESCRIPTORS)))
        .andReturn()
        .response
        .contentAsString
        .apply { println(this) }
  }

  @Test
  fun `verify find company employees of deleted users`() {
    eventStreamGenerator.submitUserTombstones(reference = "user1")

    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    mockMvc
        .perform(
            requestBuilder(
                get(
                        latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH),
                        company.identifier.toString())
                    .param("size", "3")
                    .param("page", "0")))
        .andExpect(status().isOk)
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("items").isArray,
            jsonPath("items.length()").value(3),
            jsonPath("items[0].user.displayName")
                .value(messageSource.getMessage(USER_DELETED, arrayOfNulls(0), Locale.UK)),
            jsonPath("items[1].user.displayName").value("$FIRST_NAME_4 $LAST_NAME_4"),
            jsonPath("items[2].user.displayName").value("$FIRST_NAME_3 $LAST_NAME_3"))
  }

  @Test
  fun `verify find empty list of company employee`() {
    eventStreamGenerator
        .submitEmployee(asReference = "employee1", eventType = EmployeeEventEnumAvro.DELETED)
        .submitEmployee(asReference = "employee2", eventType = EmployeeEventEnumAvro.DELETED)
        .submitEmployee(asReference = "employee3", eventType = EmployeeEventEnumAvro.DELETED)
        .submitEmployee(asReference = "employee4", eventType = EmployeeEventEnumAvro.DELETED)

    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    mockMvc
        .perform(
            requestBuilder(
                get(
                        latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH),
                        company.identifier.toString())
                    .param("size", "3")
                    .param("page", "0")))
        .andExpect(status().isOk)
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpectAll(jsonPath("items").isArray, jsonPath("items.length()").value(0))
  }

  @Test
  fun `verify find list of company employees with company not found`() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH),
                    randomUUID().toString())))
        .andExpect(status().isNotFound)
  }

  @Test
  fun `verify and document find company employee by id`() {
    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!
    val employee2 =
        repositories.employeeRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("employee2").asEmployeeId())!!

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH),
                    employee2.getIdentifierUuid().toString())))
        .andExpect(status().isOk)
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("user.displayName").value("$FIRST_NAME_2 $LAST_NAME_2"),
            jsonPath("company.displayName").value(company.name),
            jsonPath("roles").isArray,
            jsonPath("roles.length()").value(1),
            jsonPath("roles[0]").value(EmployeeRoleEnum.FM.name),
        )
        .andDo(
            document(
                "companies/document-get-employee",
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("employeeId").description("ID of the employee")),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(LINK_DELETE).description(LINK_DELETE_DESCRIPTION)),
                responseFields(EMPLOYEE_RESPONSE_FIELDS_DESCRIPTORS)))
  }

  @Test
  fun `verify find company employee by id not found`() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH),
                    randomUUID().toString())))
        .andExpect(status().isNotFound)
  }

  @Test
  fun `verify and document update company employee`() {
    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!
    val user2 =
        repositories.userProjectionRepository.findOneById(
            eventStreamGenerator.getIdentifier("user2").asUserId())!!
    val employee2 =
        repositories.employeeRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("employee2").asEmployeeId())!!

    val saveEmployeeResource = SaveEmployeeResource(user2.id, listOf(EmployeeRoleEnum.CR))

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH),
                    employee2.getIdentifierUuid()),
                saveEmployeeResource,
                0L))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "companies/document-update-employee",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("employeeId").description("The new ID of the employee")),
                employeeRequestFields,
                requestHeaders(
                    headerWithName(IF_MATCH)
                        .description(
                            "Mandatory entity tag of the company to be updated " +
                                "(previously received value of the response header field `ETag`)")),
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the updated company, needed for possible further updates of the task")),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(LINK_DELETE).description(LINK_DELETE_DESCRIPTION)),
                responseFields(EMPLOYEE_RESPONSE_FIELDS_DESCRIPTORS)))

    companyEventStoreUtils
        .verifyContainsAndGet(EmployeeEventAvro::class.java, EmployeeEventEnumAvro.UPDATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isEqualTo(employee2.getIdentifierUuid().toString())
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(1)
            assertThat(aggregate.roles[0]).isEqualTo(EmployeeRoleEnumAvro.CR)
            assertThat(aggregate.user.type).isEqualTo(USER.value)
            assertThat(aggregate.user.identifier).isEqualTo(user2.id.toString())
            assertThat(aggregate.user.version).isEqualTo(user2.version)
            assertThat(aggregate.company.type).isEqualTo(COMPANY.value)
            assertThat(aggregate.company.identifier).isEqualTo(company.identifier.toString())
            assertThat(aggregate.company.version).isEqualTo(0)
          }
        }
  }

  @Test
  fun `verify and document delete company employee`() {
    val employee2 =
        repositories.employeeRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("employee2").asEmployeeId())!!

    mockMvc
        .perform(
            requestBuilder(
                RestDocumentationRequestBuilders.delete(
                    latestVersionOf(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH),
                    employee2.getIdentifierUuid().toString())))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "companies/document-delete-employee",
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("employeeId").description("ID of the employee"))))

    companyEventStoreUtils.verifyContains(
        EmployeeEventAvro::class.java, CompanyEventEnumAvro.DELETED, 1)
  }

  companion object {

    private const val FIRST_NAME_1 = "Alfred"

    private const val FIRST_NAME_2 = "Sabine"

    private const val FIRST_NAME_3 = "Bernd"

    private const val FIRST_NAME_4 = "Anton"

    private const val LAST_NAME_1 = "Berthold"

    private const val LAST_NAME_2 = "Maier"

    private const val LAST_NAME_3 = "Bauer"

    private const val LAST_NAME_4 = "Thorsten"

    private const val LINK_NEXT_DESCRIPTION = "Link to the next employee page"

    private const val LINK_SELF_DESCRIPTION = "Link to the employee resource itself"

    private const val LINK_DELETE = "delete"

    private const val LINK_DELETE_DESCRIPTION = "Link to delete employee resource"

    private val saveEmployeeResourceFields = ConstrainedFields(SaveEmployeeResource::class.java)

    private val employeeRequestFields =
        requestFields(
            saveEmployeeResourceFields
                .withPath("roles")
                .description("Roles of the employee")
                .type(JsonFieldType.ARRAY),
            saveEmployeeResourceFields
                .withPath("userId")
                .description("ID of the employee's user")
                .type(JsonFieldType.STRING))

    private val EMPLOYEE_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            *ID_AND_VERSION_FIELD_DESCRIPTORS,
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("user.displayName").description("Full name of the employee's user"),
            fieldWithPath("user.id").description("ID of the employee's user"),
            fieldWithPath("user.email")
                .type(JsonFieldType.STRING)
                .description("Email address of the employee's user")
                .optional(),
            fieldWithPath("company.displayName").description("Name of the employee's company"),
            fieldWithPath("company.id").description("ID of the employee's company"),
            fieldWithPath("roles").description("Roles of the employee"),
            subsectionWithPath("_links").ignored())
  }
}
