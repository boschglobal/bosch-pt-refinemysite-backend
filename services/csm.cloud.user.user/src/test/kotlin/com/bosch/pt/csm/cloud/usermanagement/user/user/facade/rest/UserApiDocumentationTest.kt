/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.LINK_SELF_DESCRIPTION
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.common.util.FragmentExtractor.Companion.extract
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.PATH_VARIABLE_USER_ID
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USERS_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_LOCK_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_ROLES_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_SUGGESTIONS_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.SetUserLockedResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.SetUserRoleResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.SuggestionResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_DELETE
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_LOCK
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_SET_ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_UNLOCK
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_UNSET_ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.IanaLinkRelations.PREV
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CodeExample
@DisplayName("Test and Document User API")
class UserApiDocumentationTest : AbstractUserApiDocumentationTest() {

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
  }

  @Test
  fun `verify and document get user`() {
    eventStreamGenerator
        .submitUser("user") {
          it.position = "Boss"
          it.crafts =
              listOf(
                  eventStreamGenerator.getByReference("electricity"),
                  eventStreamGenerator.getByReference("plumbing"))
        }
        .submitProfilePicture("picture")

    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("user")))!!

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(USER_BY_USER_ID_ENDPOINT_PATH), user.getIdentifierUuid())))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("gender").value(user.gender!!.name),
            jsonPath("firstName").value(user.firstName),
            jsonPath("lastName").value(user.lastName),
            jsonPath("email").value(user.email),
            jsonPath("position").value(user.position),
            jsonPath("crafts").isArray,
            jsonPath("crafts.length()").value(2),
            jsonPath("phoneNumbers").exists(),
            jsonPath("eulaAccepted").value(true),
            jsonPath("admin").value(false),
            jsonPath("registered").value(true),
            jsonPath("locked").value(false))
        .andExpect(jsonPath("_embedded.profilePicture").exists())
        .andDo(
            document(
                "users/document-get-user",
                preprocessResponse(prettyPrint()),
                pathParameters(USER_ID_PATH_PARAMETER_DESCRIPTOR),
                responseFields(USER_RESPONSE_FIELDS),
                links(USER_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document list users`() {
    eventStreamGenerator
        .submitUser("user1") { it.lastName = "Aa" }
        .submitUser("user2") {
          it.firstName = "Albert"
          it.lastName = "Ableton"
          it.position = "Boss"
          it.crafts =
              listOf(
                  eventStreamGenerator.getByReference("electricity"),
                  eventStreamGenerator.getByReference("plumbing"))
        }
        .submitProfilePicture("picture1")

    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("user2")))!!

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(USERS_ENDPOINT_PATH))
                    .param("page", 1.toString())
                    .param("size", 1.toString())))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(jsonPath("users").isArray)
        .andExpect(jsonPath("users.length()").value(1))
        .andExpect(jsonPath("users[0].gender").value(user.gender!!.name))
        .andExpect(jsonPath("users[0].firstName").value(user.firstName))
        .andExpect(jsonPath("users[0].lastName").value(user.lastName))
        .andExpect(jsonPath("users[0].email").value(user.email))
        .andExpect(jsonPath("users[0].position").value(user.position))
        .andExpect(jsonPath("users[0].crafts").isArray)
        .andExpect(jsonPath("users[0].crafts.length()").value(2))
        .andExpect(jsonPath("users[0].phoneNumbers").exists())
        .andExpect(jsonPath("users[0].eulaAccepted").value(true))
        .andExpect(jsonPath("users[0].admin").value(false))
        .andExpect(jsonPath("users[0].registered").value(true))
        .andExpect(jsonPath("users[0].locked").value(false))
        .andExpect(jsonPath("pageNumber").value(1))
        .andExpect(jsonPath("pageSize").value(1))
        .andExpect(jsonPath("totalPages").value(4))
        .andExpect(jsonPath("totalElements").value(4))
        .andDo(
            document(
                "users/document-get-users",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    buildSortingAndPagingParameterDescriptors(
                        setOf(User.FIRST_NAME, User.LAST_NAME))),
                USER_LIST_RESPONSE_FIELDS,
                links(USER_LIST_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document suggest users`() {
    eventStreamGenerator.submitUser("user") { it.lastName = "special" }

    userEventStoreUtils.reset()

    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("user")))!!

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(USER_SUGGESTIONS_ENDPOINT_PATH))
                    .contentType(APPLICATION_JSON_VALUE)
                    .param("size", "1"),
                SuggestionResource().apply { term = "special" }))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(
            extract("items[0]") {
              jsonPath("displayName").value(user.getDisplayName()).match(it)
              jsonPath("id").value(user.getIdentifierUuid().toString()).match(it)
            })
        .andExpect(jsonPath("items.length()").value(1))
        .andExpect(jsonPath("pageNumber").value(0))
        .andExpect(jsonPath("pageSize").value(1))
        .andExpect(jsonPath("totalPages").value(1))
        .andExpect(jsonPath("totalElements").value(1))
        .andDo(
            document(
                "users/document-suggest-users-latest",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(SUGGEST_USER_REQUEST_PARAMETERS),
                requestFields(SUGGEST_USER_REQUEST_FIELD_DESCRIPTORS),
                SUGGEST_USER_RESPONSE_FIELDS,
                links(linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION))))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document set user role admin`() {
    eventStreamGenerator.submitUser("user") {
      it.position = "Boss"
      it.crafts =
          listOf(
              eventStreamGenerator.getByReference("electricity"),
              eventStreamGenerator.getByReference("plumbing"))
    }

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(USER_ROLES_BY_USER_ID_ENDPOINT_PATH),
                    eventStreamGenerator.getIdentifier("user")),
                SetUserRoleResource(true)))
        .andExpect(status().isOk)
        .andDo(
            document(
                "users/document-set-user-roles",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(SET_USER_ROLE_REQUEST_FIELD_DESCRIPTORS),
                responseFields(USER_RESPONSE_FIELDS),
                pathParameters(USER_ID_PATH_PARAMETER_DESCRIPTOR),
                links(USER_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, 1).apply {
      assertThat(first().getAggregate().getAdmin()).isTrue
    }
  }

  @Test
  fun `verify and document lock user`() {
    eventStreamGenerator.submitUser("user") {
      it.position = "Boss"
      it.crafts =
          listOf(
              eventStreamGenerator.getByReference("electricity"),
              eventStreamGenerator.getByReference("plumbing"))
    }

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf(USER_LOCK_BY_USER_ID_ENDPOINT_PATH),
                    eventStreamGenerator.getIdentifier("user")),
                SetUserLockedResource(true)))
        .andExpect(status().isOk)
        .andDo(
            document(
                "users/document-lock-user",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(LOCK_USER_REQUEST_FIELD_DESCRIPTORS),
                responseFields(USER_RESPONSE_FIELDS),
                pathParameters(USER_ID_PATH_PARAMETER_DESCRIPTOR),
                links(USER_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyContainsAndGet(UserEventAvro::class.java, UPDATED, 1).apply {
      assertThat(first().getAggregate().getLocked()).isTrue
    }
  }

  @Test
  fun `verify and document delete user`() {
    eventStreamGenerator.submitUser("user")

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(USER_BY_USER_ID_ENDPOINT_PATH),
                    eventStreamGenerator.getIdentifier("user"))))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "users/document-delete-user",
                preprocessResponse(prettyPrint()),
                pathParameters(USER_ID_PATH_PARAMETER_DESCRIPTOR)))

    userEventStoreUtils.verifyContainsTombstoneMessages(1, USER.toString())
  }

  companion object {

    private val USER_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_USER_ID).description("ID of the user")

    private val LOCK_USER_REQUEST_FIELD_DESCRIPTORS =
        ConstrainedFields(SetUserLockedResource::class.java)
            .withPath("locked")
            .description("Flag if user is locked")
            .type(BOOLEAN)

    private val SET_USER_ROLE_REQUEST_FIELD_DESCRIPTORS =
        ConstrainedFields(SetUserRoleResource::class.java)
            .withPath("admin")
            .description("Flag if user has administrative rights")
            .type(BOOLEAN)

    private val SUGGEST_USER_REQUEST_PARAMETERS =
        listOf(
            parameterWithName("size")
                .description("Optional: maximum number of elements to load")
                .optional())

    private val SUGGEST_USER_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("term")
                .description("The search term to be applied")
                .type(STRING)
                .attributes(
                    key("constraints")
                        .value("Mandatory field. Parts of firstname, lastname or email")))

    private val SUGGEST_USER_RESPONSE_FIELDS =
        buildPagedItemsListResponseFields(
            listOf(
                fieldWithPath("id").description("ID of an user").type(STRING),
                fieldWithPath("displayName").description("Full name of the user").type(STRING),
                fieldWithPath("email").description("Email of the user").type(STRING)))

    private val USER_RESPONSE_FIELDS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            *COMMON_USER_RESPONSE_FIELD_DESCRIPTORS,
            *ADMIN_USER_RESPONSE_FIELD_DESCRIPTORS,
            fieldWithPath("locked").description("Flag indicating if user is locked").type(BOOLEAN),
            subsectionWithPath("_links").ignored(),
            subsectionWithPath("_embedded")
                .optional()
                .description("Embedded resources")
                .type(OBJECT))

    private val USER_LIST_RESPONSE_FIELDS =
        buildPagedListResponseFields("users", USER_RESPONSE_FIELDS)

    private val USER_LIST_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
            linkWithRel(NEXT.value()).description("Link to the next user page"),
            linkWithRel(PREV.value()).description("Link to the previous user page"))

    private val USER_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
            linkWithRel(LINK_DELETE)
                .description("Link to delete the user, available if delete is possible")
                .optional(),
            linkWithRel(LINK_SET_ADMIN)
                .description("Link to give the user admin permissions")
                .optional(),
            linkWithRel(LINK_UNSET_ADMIN)
                .description("Link to remove the user admin permissions")
                .optional(),
            linkWithRel(LINK_LOCK).description("Link to lock a user").optional(),
            linkWithRel(LINK_UNLOCK).description("Link to unlock a user").optional())
  }
}
