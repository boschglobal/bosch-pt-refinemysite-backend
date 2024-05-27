/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.INVITATION
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.RESENT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.ASSIGNABLE_PARTICIPANTS_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.COMPANIES_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANTS_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANTS_BY_PROJECT_SEARCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANT_BY_PARTICIPANT_ID_RESEND_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PARTICIPANT_BY_PROJECT_ID_AND_PARTICIPANT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PATH_VARIABLE_PARTICIPANT_ID
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.ParticipantController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.AssignParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.SearchParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.UpdateParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource.Companion.LINK_RESEND
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService.Companion.PARTICIPANT_ALLOWED_SORTING_PROPERTIES
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.testdata.invitationForUnregisteredUser
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import java.time.ZoneOffset
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.IanaLinkRelations.PREV
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ParticipantApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @MockkBean(relaxed = true) private lateinit var participantMailService: ParticipantMailService

  private val assignmentConstrainedField = ConstrainedFields(AssignParticipantResource::class.java)
  private val searchConstrainedField = ConstrainedFields(SearchParticipantResource::class.java)

  private val assignParticipantRequestFields =
      requestFields(
          assignmentConstrainedField
              .withPath("email")
              .description("Email address of participant")
              .type(STRING),
          assignmentConstrainedField
              .withPath("role")
              .description("Role to assign to the participant [FM, CR or CSM]")
              .type(STRING))

  private val updateParticipantRequestFields =
      requestFields(
          assignmentConstrainedField
              .withPath("role")
              .description("Role to assign to the participant [FM, CR or CSM]")
              .type(STRING))

  private val searchParticipantRequestFields =
      requestFields(
          searchConstrainedField
              .withPath("status")
              .description(
                  "Set of statuses of the participant to be filtered [INVITED, VALIDATION, ACTIVE or INACTIVE]")
              .type(ARRAY)
              .optional(),
          searchConstrainedField
              .withPath("company")
              .description("Company of the participant to be filtered")
              .type(STRING)
              .optional(),
          searchConstrainedField
              .withPath("roles")
              .description("Set of roles of the participant to be filtered [FM, CR or CSM]")
              .type(ARRAY)
              .optional())

  private val participantsFields =
      listOf(
          fieldWithPath("project.id").description("ID of the project"),
          fieldWithPath("project.displayName").description("Name of the project"),
          fieldWithPath("projectRole").description("Role of the participant in project"),
          fieldWithPath("company.id").description("Identifier of the company"),
          fieldWithPath("company.displayName")
              .description("Name of the company the participant belongs to"),
          fieldWithPath("user.id").description("ID of the participant user"),
          fieldWithPath("user.displayName").description("Name of the participant user"),
          fieldWithPath("user.picture")
              .description("URL of the project participant's profile picture"),
          fieldWithPath("id").description("Id of the participant"),
          fieldWithPath("version").description("Version of the participant"),
          fieldWithPath("gender").description("Gender of the participant"),
          fieldWithPath("email").description("Email address of the participant"),
          fieldWithPath("status")
              .description(
                  "Status of the participant in the project [INVITED, VALIDATION, ACTIVE or INACTIVE]"),
          fieldWithPath("createdDate").description("Date of creation"),
          fieldWithPath("lastModifiedDate").description("Date of last modification"),
          fieldWithPath("createdBy.displayName").description("Name who has created this record"),
          fieldWithPath("createdBy.id").description("Identifier who has created this record"),
          fieldWithPath("lastModifiedBy.displayName")
              .description("Name who has modified this record"),
          fieldWithPath("lastModifiedBy.id").description("Identifier who has modified this record"),
          subsectionWithPath("phoneNumbers").description("Phone numbers of the participant"),
          subsectionWithPath("crafts").description("Crafts of the participant"),
          subsectionWithPath("_links").ignored().optional())

  private val participantCompaniesFields =
      listOf(
          fieldWithPath("id").description("ID of the company"),
          fieldWithPath("displayName").description("Name of the company"))

  private val participantFields = responseFields(participantsFields)

  private val participantsResponseFieldsSnippet =
      responseFields(
              fieldWithPath("pageNumber").description("Number of this page"),
              fieldWithPath("pageSize").description("Size of this page"),
              fieldWithPath("totalPages")
                  .description("Total number of available pages of project participants"),
              fieldWithPath("totalElements")
                  .description("Total number of project participants available"),
              subsectionWithPath("_links").ignored().optional())
          .andWithPrefix("items[].", participantsFields)

  private val participantLinks =
      links(
          linkWithRel(LINK_DELETE).description(LINK_DELETE_DESCRIPTION).optional(),
          linkWithRel(LINK_UPDATE).description(LINK_UPDATE_DESCRIPTION).optional(),
          linkWithRel(LINK_RESEND).description(LINK_RESEND_DESCRIPTION).optional())

  private val secondUserSortedByLastName by lazy {
    repositories.findUser(getIdentifier("userCsm1"))!!
  }
  private val employeeCsm by lazy { repositories.findEmployee(getIdentifier("employeeCsm2"))!! }
  private val companyA by lazy { repositories.findCompany(getIdentifier("company"))!! }
  private val companyB by lazy { repositories.findCompany(getIdentifier("company2"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val participantFm by lazy { repositories.findParticipant(getIdentifier("participant"))!! }
  private val participantCsm by lazy {
    repositories.findParticipant(getIdentifier("participantCsm2"))!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitCraft("craft1")
        .submitCraft("craft2")
        .submitUser("user") {
          it.lastName = "1"
          it.crafts = listOf(getByReference("craft1"), getByReference("craft2"))
        }
        .submitUser("userCsm1") {
          it.lastName = "2"
          it.crafts = listOf(getByReference("craft1"), getByReference("craft2"))
        }
        .submitUser("userCsm2") {
          it.lastName = "3"
          it.crafts = listOf(getByReference("craft1"), getByReference("craft2"))
        }
        .submitCompany("company2")
        .submitUser("userCr") {
          it.lastName = "4"
          it.crafts = listOf(getByReference("craft1"), getByReference("craft2"))
        }
        .submitEmployee("employeeCr") { it.roles = listOf(EmployeeRoleEnumAvro.CR) }
        .submitParticipantG3("participantCr") { it.role = ParticipantRoleEnumAvro.CR }
        .setLastIdentifierForType(COMPANY.name, getByReference("company"))

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun documentFindSingleParticipant() {
    val responseFields = ArrayList(participantsFields)
    responseFields.add(fieldWithPath("id").description("ID of the project participant"))

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT),
                    participantCsm.identifier)))
        .andExpect(status().isOk)
        .andExpect(header().doesNotExist(LOCATION))
        .andDo(
            document(
                "projects/document-single-project-participant",
                pathParameters(
                    parameterWithName("participantId")
                        .description("ID of the project participant"))))
        .andDo(
            document(
                "projects/document-single-project-participant",
                preprocessResponse(prettyPrint()),
                responseFields(responseFields),
                participantLinks))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun documentFindAllParticipants() {
    val searchResource = SearchParticipantResource(null, null, null)
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(PARTICIPANTS_BY_PROJECT_SEARCH_ENDPOINT), project.identifier)
                    .param("sort", "user.lastName,user.firstName,asc")
                    .param("size", "1")
                    .param("page", "1"),
                searchResource))
        .andExpect(status().isOk)
        .andExpect(header().doesNotExist(LOCATION))
        .andExpect(jsonPath("$.items").isArray)
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(
            jsonPath("$.items[?(@.project.id == '" + project.identifier.toString() + "')]")
                .exists())
        .andExpect(
            jsonPath(
                    "$.items[?(@.user.id == '" +
                        secondUserSortedByLastName.identifier.toString() +
                        "')]")
                .exists())
        .andDo(
            document(
                "projects/document-search-project-participants",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("projectId").description("The id of the project")),
                queryParameters(REQUEST_PARAMETER_DESCRIPTORS),
                searchParticipantRequestFields,
                participantsResponseFieldsSnippet,
                PARTICIPANTS_LINKS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun documentFindAllAssignableCompanies() {
    SecurityContextHolder.getContext().authentication =
        TestingAuthenticationToken(employeeCsm.user, "n/a", createAuthorityList("ROLE_ADMIN"))
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf("$COMPANIES_BY_PROJECT_ID_ENDPOINT?includeInactive=false"),
                    project.identifier)))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.companies").isArray)
        .andExpect(jsonPath("$.companies.length()").value(2))
        .andExpect(jsonPath("$.companies[?(@.id == '" + companyA.identifier + "')]").exists())
        .andExpect(jsonPath("$.companies[?(@.id == '" + companyB.identifier + "')]").exists())
        .andExpect(jsonPath("$.companies[?(@.displayName == '" + companyA.name + "')]").exists())
        .andExpect(jsonPath("$.companies[?(@.displayName == '" + companyB.name + "')]").exists())
        .andDo(
            document(
                "projects/document-get-project-companies",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of " + "the project")),
                queryParameters(
                    parameterWithName("includeInactive")
                        .description(
                            "Flag to indicate if companies of inactive participants should be included")),
                responseFields(
                        subsectionWithPath("pageNumber").description("Number of page requested"),
                        subsectionWithPath("pageSize")
                            .description("Number of companies per page requested"),
                        subsectionWithPath("totalPages")
                            .description("Number of total pages available"),
                        subsectionWithPath("totalElements")
                            .description("Number of total companies available"),
                        subsectionWithPath("_links").ignored().optional(),
                        subsectionWithPath("companies").description("List of companies"))
                    .andWithPrefix("companies[].", participantCompaniesFields)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun documentFindAllAssignableParticipants() {
    mockMvc
        .perform(
            requestBuilder(
                    get(
                        latestVersionOf(ASSIGNABLE_PARTICIPANTS_BY_PROJECT_ID_ENDPOINT),
                        project.identifier))
                .param("sort", "user.lastName,user.firstName,asc")
                .param("size", "1")
                .param("page", "1"))
        .andExpect(status().isOk)
        .andExpect(header().doesNotExist(LOCATION))
        .andExpect(jsonPath("$.items").isArray)
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(
            jsonPath("$.items[?(@.project.id == '" + project.identifier.toString() + "')]")
                .exists())
        .andExpect(
            jsonPath(
                    "$.items[?(@.user.id == '" +
                        secondUserSortedByLastName.identifier.toString() +
                        "')]")
                .exists())
        .andDo(
            document(
                "projects/document-assignable-list-project-participants",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"))))
        .andDo(
            document(
                "projects/document-assignable-list-project-participants",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("company")
                        .optional()
                        .description(
                            "Identifier of company to filter participants list for (Optional)"),
                    parameterWithName("sort")
                        .optional()
                        .description(
                            "List of project attributes separated by commas followed by sorting order ASC/DESC" +
                                " (Optional)"),
                    parameterWithName("page")
                        .optional()
                        .description("Number of the requested page, defaults to 0 (Optional)"),
                    parameterWithName("size")
                        .optional()
                        .description(
                            "Size of the requested page, defaults to 20, maximum is 100 (Optional)")),
                links(
                    linkWithRel(LINK_ASSIGN).description(LINK_ASSIGN_DESCRIPTION),
                    linkWithRel(PREV.value()).description(LINK_PREVIOUS_DESCRIPTION),
                    linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION)),
                participantsResponseFieldsSnippet))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun documentAssignParticipant() {
    eventStreamGenerator
        .submitUser("userNew") {
          it.crafts = listOf(getByReference("craft1"), getByReference("craft2"))
        }
        .submitEmployee("employeeNew") { it.roles = listOf(EmployeeRoleEnumAvro.CR) }

    val userNew = repositories.findUser(getIdentifier("userNew"))!!
    val employeeNew = repositories.findEmployee(getIdentifier("employeeNew"))!!

    val expectedUrl = "/projects/" + project.identifier + "/participants/"
    val assignParticipantResource = AssignParticipantResource(employeeNew.user!!.email!!, CR)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(PARTICIPANTS_BY_PROJECT_ID_ENDPOINT), project.identifier),
                assignParticipantResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, CoreMatchers.containsString(expectedUrl)))
        .andExpect(jsonPath("$.project.id").value(project.identifier.toString()))
        .andExpect(jsonPath("$.project.displayName").value(project.getDisplayName()))
        .andExpect(jsonPath("$.projectRole").value(CR.name))
        .andExpect(jsonPath("$.company.id").exists())
        .andExpect(jsonPath("$.company.displayName").value(employeeNew.company!!.getDisplayName()))
        .andExpect(jsonPath("$.user.id").exists())
        .andExpect(jsonPath("$.user.displayName").value(employeeNew.user!!.getDisplayName()))
        .andExpect(jsonPath("$.phoneNumbers").isArray)
        .andExpect(jsonPath("$.email").value(assignParticipantResource.email))
        .andExpect(jsonPath("$.crafts").isArray)
        .andExpect(jsonPath("$.crafts", hasSize<Any>(2)))
        .andDo(
            document(
                "projects/document-assign-project-participant",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")),
                assignParticipantRequestFields))
        .andDo(
            document(
                "projects/document-assign-project-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                participantFields,
                participantLinks))

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, CREATED)
        .getAggregate()
        .also {
          assertThat(it.getProject())
              .isEqualByComparingTo(project.identifier.toAggregateReference())
          assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CR)
          assertThat(it.getCompany()).isEqualByComparingTo(companyA.toAggregateIdentifier())
          assertThat(it.getUser()).isEqualByComparingTo(userNew.toAggregateIdentifier())
        }

    verify { participantMailService.sendParticipantAdded(project.identifier, any(), any()) }
  }

  @Test
  fun documentAssignParticipantWithIdentifier() {
    eventStreamGenerator
        .submitUser("userNew") {
          it.crafts = listOf(getByReference("craft1"), getByReference("craft2"))
        }
        .submitEmployee("employeeNew") { it.roles = listOf(EmployeeRoleEnumAvro.CR) }

    val userNew = repositories.findUser(getIdentifier("userNew"))!!
    val employeeNew = repositories.findEmployee(getIdentifier("employeeNew"))!!

    val participantIdentifier = randomUUID()

    val expectedUrl = "/projects/" + project.identifier + "/participants/" + participantIdentifier
    val assignParticipantResource = AssignParticipantResource(employeeNew.user!!.email!!, CR)

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(PARTICIPANT_BY_PROJECT_ID_AND_PARTICIPANT_ID_ENDPOINT),
                    project.identifier.toString(),
                    participantIdentifier.toString()),
                assignParticipantResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, CoreMatchers.endsWith(expectedUrl)))
        .andExpect(jsonPath("$.project.id").value(project.identifier.toString()))
        .andExpect(jsonPath("$.project.displayName").value(project.getDisplayName()))
        .andExpect(jsonPath("$.projectRole").value(CR.name))
        .andExpect(jsonPath("$.company.id").exists())
        .andExpect(jsonPath("$.company.displayName").value(employeeNew.company!!.getDisplayName()))
        .andExpect(jsonPath("$.user.id").exists())
        .andExpect(jsonPath("$.user.displayName").value(employeeNew.user!!.getDisplayName()))
        .andExpect(jsonPath("$.phoneNumbers").isArray)
        .andExpect(jsonPath("$.email").value(assignParticipantResource.email))
        .andExpect(jsonPath("$.crafts").isArray)
        .andExpect(jsonPath("$.crafts", hasSize<Any>(2)))
        .andDo(
            document(
                "projects/document-assign-project-participant-with-identifier",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"),
                    parameterWithName(PATH_VARIABLE_PARTICIPANT_ID)
                        .description("ID of the participant")
                        .optional()),
                assignParticipantRequestFields))
        .andDo(
            document(
                "projects/document-assign-project-participant-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                participantFields,
                participantLinks))

    val expectedIdentifier =
        AggregateIdentifierAvro.newBuilder()
            .setType(PARTICIPANT.value)
            .setIdentifier(participantIdentifier.toString())
            .setVersion(0)
            .build()

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, CREATED)
        .getAggregate()
        .also {
          assertThat(it.getAggregateIdentifier()).isEqualByComparingTo(expectedIdentifier)
          assertThat(it.getProject())
              .isEqualByComparingTo(project.identifier.toAggregateReference())
          assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CR)
          assertThat(it.getCompany()).isEqualByComparingTo(companyA.toAggregateIdentifier())
          assertThat(it.getUser()).isEqualByComparingTo(userNew.toAggregateIdentifier())
        }

    val participants = mutableListOf<Participant>()
    verify {
      participantMailService.sendParticipantAdded(project.identifier, capture(participants), any())
    }
    assertThat(participants).hasSize(1)
    assertThat(participants.first().identifier).isEqualTo(participantIdentifier.asParticipantId())
  }

  @Test
  fun documentUpdateParticipant() {
    val employeeFm = repositories.findEmployee(getIdentifier("employee"))!!
    val userFm = repositories.findUser(getIdentifier("user"))!!

    val updateParticipantResource = UpdateParticipantResource(CR)
    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT),
                    participantFm.identifier.toString()),
                updateParticipantResource,
                0L))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andExpect(jsonPath("$.project.id").value(project.identifier.toString()))
        .andExpect(jsonPath("$.project.displayName").value(project.getDisplayName()))
        .andExpect(jsonPath("$.projectRole").value(CR.name))
        .andExpect(jsonPath("$.company.id").exists())
        .andExpect(jsonPath("$.company.displayName").value(employeeFm.company!!.getDisplayName()))
        .andExpect(jsonPath("$.user.id").exists())
        .andExpect(jsonPath("$.user.displayName").value(employeeFm.user!!.getDisplayName()))
        .andExpect(jsonPath("$.phoneNumbers").isArray)
        .andExpect(jsonPath("$.email").value(employeeFm.user!!.email))
        .andExpect(jsonPath("$.crafts").isArray)
        .andExpect(jsonPath("$.crafts", hasSize<Any>(2)))
        .andDo(
            document(
                "projects/document-update-project-participant",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PARTICIPANT_ID)
                        .description("ID of the participant")),
                requestHeaders(
                    headerWithName(IF_MATCH)
                        .description(
                            "Mandatory entity tag of the participant to be updated (previously received value of the " +
                                "response header field `ETag`)")),
                updateParticipantRequestFields))
        .andDo(
            document(
                "projects/document-update-project-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the participant, needed for possible further updates of the participant")),
                participantFields,
                participantLinks))

    val expectedIdentifier =
        AggregateIdentifierAvro.newBuilder()
            .setType(PARTICIPANT.value)
            .setIdentifier(participantFm.identifier.toString())
            .setVersion(1)
            .build()

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, UPDATED)
        .getAggregate()
        .also {
          assertThat(it.getAggregateIdentifier()).isEqualByComparingTo(expectedIdentifier)
          assertThat(it.getProject())
              .isEqualByComparingTo(project.identifier.toAggregateReference())
          assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.CR)
          assertThat(it.getCompany()).isEqualByComparingTo(companyA.toAggregateIdentifier())
          assertThat(it.getUser()).isEqualByComparingTo(userFm.toAggregateIdentifier())
        }
  }

  @Test
  fun documentDeleteParticipant() {
    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT),
                    participantFm.identifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "projects/document-delete-participant",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PARTICIPANT_ID)
                        .description("Identifier of the project participant"))))

    val expectedIdentifier =
        AggregateIdentifierAvro.newBuilder()
            .setType(PARTICIPANT.value)
            .setIdentifier(participantFm.identifier.toString())
            .setVersion(1)
            .build()

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, DEACTIVATED)
        .getAggregate()
        .also {
          assertThat(it.getAggregateIdentifier()).isEqualByComparingTo(expectedIdentifier)
          assertThat(it.getProject())
              .isEqualByComparingTo(project.identifier.toAggregateReference())
          assertThat(it.getRole()).isEqualTo(ParticipantRoleEnumAvro.FM)
          assertThat(it.getCompany())
              .isEqualByComparingTo(participantFm.company!!.toAggregateIdentifier())
          assertThat(it.getUser())
              .isEqualByComparingTo(participantFm.user!!.toAggregateIdentifier())
        }
  }

  @Test
  fun verifyAndDocumentResendInvitation() {
    eventStreamGenerator.invitationForUnregisteredUser("invitedUser")

    invitationEventStoreUtils.reset()

    val invitedParticipant =
        repositories.findParticipant(getIdentifier("invitedUser-participant").asParticipantId())!!

    val invitation =
        repositories.invitationRepository.findOneByParticipantIdentifier(
            getIdentifier("invitedUser-participant").asParticipantId())!!

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(PARTICIPANT_BY_PARTICIPANT_ID_RESEND_ENDPOINT),
                    invitedParticipant.identifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "projects/document-resend-invitation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PARTICIPANT_ID)
                        .description("Identifier of the project participant"))))

    val expectedIdentifier =
        AggregateIdentifierAvro.newBuilder()
            .setType(INVITATION.value)
            .setIdentifier(invitation.identifier.toString())
            .setVersion(1)
            .build()

    invitationEventStoreUtils
        .verifyContainsAndGet(InvitationEventAvro::class.java, RESENT)
        .getAggregate()
        .also {
          assertThat(it.getAggregateIdentifier()).isEqualByComparingTo(expectedIdentifier)
          assertThat(it.getEmail()).isEqualTo(invitation.email)
          assertThat(it.getParticipantIdentifier())
              .isEqualTo(invitedParticipant.identifier.toString())
          assertThat(it.getLastSent())
              .isNotEqualTo(invitation.lastSent.toInstant(ZoneOffset.UTC).toEpochMilli())
        }

    verify {
      participantMailService.sendParticipantInvited(
          project.identifier, invitation.email, any(), any())
    }
  }

  companion object {
    private const val LINK_NEXT_DESCRIPTION = "Link to the next project participant page"
    private const val LINK_PREVIOUS_DESCRIPTION = "Link to the previous project participant page"
    private const val LINK_ASSIGN_DESCRIPTION = "Link to assign participants to the project"
    private const val LINK_RESEND_DESCRIPTION = "Link to resend invitation to the project"
    private const val LINK_UPDATE_DESCRIPTION = "Link to update participants of the project"
    private const val LINK_DELETE_DESCRIPTION = "Link to delete the participant"

    private val PARTICIPANTS_LINKS =
        links(
            linkWithRel(LINK_ASSIGN).description(LINK_ASSIGN_DESCRIPTION),
            linkWithRel(PREV.value()).description(LINK_PREVIOUS_DESCRIPTION),
            linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION))

    private val REQUEST_PARAMETER_DESCRIPTORS =
        buildSortingAndPagingParameterDescriptors(PARTICIPANT_ALLOWED_SORTING_PROPERTIES.keys)
  }
}
