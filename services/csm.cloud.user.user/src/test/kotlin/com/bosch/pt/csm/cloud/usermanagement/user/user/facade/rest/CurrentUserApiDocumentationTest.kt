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
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.ID_AND_VERSION_FIELD_DESCRIPTORS
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.LINK_SELF_DESCRIPTION
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.CurrentUserController.Companion.CURRENT_USER_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.datastructure.PhoneNumberDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.CreateCurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.UpdateCurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.CurrentUserResource.Companion.LINK_CURRENT_USER_UPDATE
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType.MOBILE
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType.ORGANIZATION
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CodeExample
@DisplayName("Test and Document Current User API")
class CurrentUserApiDocumentationTest : AbstractUserApiDocumentationTest() {

  @Test
  fun `verify and document register current user`() {
    setSecurityContextAsUnregisteredUser()

    val createCurrentUserResource =
        CreateCurrentUserResource(
            MALE,
            "Hans",
            "Mustermann",
            "Foreman",
            listOf(craft1.identifier, craft2.identifier),
            phoneNumbersDs,
            true,
            Locale.UK,
            IsoCountryCodeEnum.GB)

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(CURRENT_USER_ENDPOINT_PATH)), createCurrentUserResource))
        .andExpect(status().isCreated)
        .andExpectAll(
            jsonPath("id").isNotEmpty,
            jsonPath("version").value(0),
            jsonPath("gender").value(createCurrentUserResource.gender.name),
            jsonPath("firstName").value(createCurrentUserResource.firstName),
            jsonPath("lastName").value(createCurrentUserResource.lastName),
            jsonPath("email").value("test@example.com"),
            jsonPath("position").value(createCurrentUserResource.position),
            jsonPath("locale").value(createCurrentUserResource.locale.toString()),
            jsonPath("country").value(createCurrentUserResource.country.name),
            jsonPath("eulaAccepted").value(true),
            jsonPath("crafts").isArray,
            jsonPath("crafts.length()").value(2),
            jsonPath("phoneNumbers").isArray,
            jsonPath("phoneNumbers.length()").value(2),
            jsonPath("phoneNumbers[0].phoneNumberType")
                .value(phoneNumbersDs.first().phoneNumberType.name),
            jsonPath("phoneNumbers[0].countryCode").value(phoneNumbersDs.first().countryCode),
            jsonPath("phoneNumbers[0].phoneNumber").value(phoneNumbersDs.first().phoneNumber),
            jsonPath("phoneNumbers[1].phoneNumberType")
                .value(phoneNumbersDs.elementAt(1).phoneNumberType.name),
            jsonPath("phoneNumbers[1].countryCode").value(phoneNumbersDs.elementAt(1).countryCode),
            jsonPath("phoneNumbers[1].phoneNumber").value(phoneNumbersDs.elementAt(1).phoneNumber),
        )
        .andDo(
            document(
                "users/document-register-current-user",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(REGISTER_CURRENT_USER_REQUEST_FIELD_DESCRIPTORS),
                responseFields(CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                links(CURRENT_USER_LINK_DESCRIPTORS)))

    userEventStoreUtils
        .verifyContainsAndGet(UserEventAvro::class.java, UserEventEnumAvro.CREATED)
        .getAggregate()
        .also {
          assertThat(it.aggregateIdentifier.identifier).isNotEmpty
          assertThat(it.aggregateIdentifier.version).isEqualTo(0)
          assertThat(it.aggregateIdentifier.type).isEqualTo(USER.toString())
          assertThat(it.userId).isEqualTo("UNREGISTERED")
          assertThat(it.email).isEqualTo("test@example.com")
          assertThat(it.admin).isFalse
          assertThat(it.eulaAcceptedDate).isNotNull
          assertThat(it.firstName).isEqualTo(createCurrentUserResource.firstName)
          assertThat(it.lastName).isEqualTo(createCurrentUserResource.lastName)
          assertThat(it.gender.toString()).isEqualTo(createCurrentUserResource.gender.toString())
          assertThat(it.registered).isTrue
          assertThat(it.position).isEqualTo(createCurrentUserResource.position)
          assertThat(it.locale).isEqualTo(createCurrentUserResource.locale.toString())
          assertThat(it.country.name).isEqualTo(createCurrentUserResource.country.name)
          assertCraft(it)
          assertPhoneNumber(it)
        }
  }

  @Test
  fun `verify and document update current user`() {
    eventStreamGenerator.submitUser("user")

    setAuthentication("user")
    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("user")))!!

    val updateCurrentUserResource =
        UpdateCurrentUserResource(
            gender = MALE,
            firstName = "Hans",
            lastName = "Mustermann",
            position = "Foreman",
            phoneNumbers = phoneNumbersDs,
            craftIds = listOf(craft1.identifier, craft2.identifier),
            locale = Locale.GERMANY,
            country = IsoCountryCodeEnum.DE)

    userEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf(CURRENT_USER_ENDPOINT_PATH)), updateCurrentUserResource, 0L))
        .andExpect(status().isOk)
        .andExpect { result: MvcResult ->
          jsonPath("id").value(user.getIdentifierUuid().toString()).match(result)
          jsonPath("version").value(1L).match(result)
          jsonPath("gender").value(updateCurrentUserResource.gender.name).match(result)
          jsonPath("firstName").value(updateCurrentUserResource.firstName).match(result)
          jsonPath("lastName").value(updateCurrentUserResource.lastName).match(result)
          jsonPath("email").value(user.email).match(result)
          jsonPath("position").value(updateCurrentUserResource.position).match(result)
          jsonPath("locale").value(updateCurrentUserResource.locale.toString()).match(result)
          jsonPath("country").value(updateCurrentUserResource.country.name).match(result)
          jsonPath("eulaAccepted").value(true).match(result)
          jsonPath("crafts").isArray.match(result)
          jsonPath("crafts.length()").value(2).match(result)
          jsonPath("phoneNumbers").isArray.match(result)
          jsonPath("phoneNumbers.length()").value(2).match(result)
        }
        .andDo(
            document(
                "users/document-update-current-user",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(UPDATE_CURRENT_USER_REQUEST_FIELD_DESCRIPTORS),
                responseFields(CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS),
                links(CURRENT_USER_LINK_DESCRIPTORS)))

    userEventStoreUtils
        .verifyContainsAndGet(UserEventAvro::class.java, UserEventEnumAvro.UPDATED)
        .getAggregate()
        .also {
          assertThat(it.aggregateIdentifier.identifier).isNotEmpty
          assertThat(it.aggregateIdentifier.version).isEqualTo(1L)
          assertThat(it.aggregateIdentifier.type).isEqualTo(USER.toString())
          assertThat(it.userId).isEqualTo(user.externalUserId)
          assertThat(it.email).isEqualTo(user.email)
          assertThat(it.admin).isFalse
          assertThat(it.eulaAcceptedDate).isNotNull
          assertThat(it.firstName).isEqualTo(updateCurrentUserResource.firstName)
          assertThat(it.lastName).isEqualTo(updateCurrentUserResource.lastName)
          assertThat(it.gender.toString()).isEqualTo(updateCurrentUserResource.gender.toString())
          assertThat(it.registered).isTrue
          assertThat(it.position).isEqualTo(updateCurrentUserResource.position)
          assertCraft(it)
          assertPhoneNumber(it)
          assertThat(it.locale).isEqualTo(updateCurrentUserResource.locale.toString())
          assertThat(it.country.name).isEqualTo(updateCurrentUserResource.country.name)
        }
  }

  @Test
  fun `verify and document get current user`() {
    eventStreamGenerator
        .submitUser("user") {
          it.position = "Boss"
          it.crafts =
              listOf(
                  eventStreamGenerator.getByReference("electricity"),
                  eventStreamGenerator.getByReference("plumbing"))
        }
        .submitProfilePicture("project")

    setAuthentication("user")
    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("user")))!!

    userEventStoreUtils.reset()

    mockMvc
        .perform(requestBuilder(get(latestVersionOf(CURRENT_USER_ENDPOINT_PATH))))
        .andExpect(status().isOk)
        .andExpect { result: MvcResult ->
          jsonPath("gender").value(user.gender!!.name).match(result)
          jsonPath("firstName").value(user.firstName).match(result)
          jsonPath("lastName").value(user.lastName).match(result)
          jsonPath("email").value(user.email).match(result)
          jsonPath("position").value(user.position).match(result)
          jsonPath("crafts").isArray.match(result)
          jsonPath("crafts.length()").value(2).match(result)
          jsonPath("phoneNumbers").exists().match(result)
          jsonPath("eulaAccepted").value(true).match(result)
        }
        .andExpect(jsonPath("_embedded.profilePicture").exists())
        .andDo(
            document(
                "users/document-get-current-user",
                preprocessResponse(prettyPrint()),
                responseFields(CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS),
                links(CURRENT_USER_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document get current user (when being an admin user)`() {
    eventStreamGenerator
        .submitUser("adminUser") {
          it.position = "Boss"
          it.admin = true
          it.crafts = listOf(eventStreamGenerator.getByReference("electricity"))
        }
        .submitProfilePicture("project")

    setAuthentication("adminUser")

    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("adminUser")))!!

    userEventStoreUtils.reset()

    mockMvc
        .perform(requestBuilder(get(latestVersionOf(CURRENT_USER_ENDPOINT_PATH))))
        .andExpect(status().isOk)
        .andExpect { result: MvcResult ->
          jsonPath("gender").value(user.gender!!.name).match(result)
          jsonPath("firstName").value(user.firstName).match(result)
          jsonPath("lastName").value(user.lastName).match(result)
          jsonPath("email").value(user.email).match(result)
          jsonPath("position").value(user.position).match(result)
          jsonPath("admin").value(user.admin).match(result)
          jsonPath("registered").value(user.registered).match(result)
          jsonPath("eulaAccepted").value(true).match(result)
        }
        .andDo(
            document(
                "users/document-get-current-admin-user",
                preprocessResponse(prettyPrint()),
                responseFields(CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS_FOR_ADMIN_USERS),
                links(CURRENT_USER_LINK_DESCRIPTORS)))

    userEventStoreUtils.verifyEmpty()
  }

  private fun assertCraft(aggregate: UserAggregateAvro) {
    val craft1Identifier =
        aggregate.crafts.firstOrNull { it.identifier == craft1.getIdentifierUuid().toString() }
    val craft2Identifier =
        aggregate.crafts.firstOrNull { it.identifier == craft2.getIdentifierUuid().toString() }
    assertThat(craft1Identifier).isNotNull
    assertThat(craft1Identifier!!.type).isEqualTo("CRAFT")
    assertThat(craft2Identifier).isNotNull
    assertThat(craft2Identifier!!.type).isEqualTo("CRAFT")
  }

  private fun assertPhoneNumber(aggregate: UserAggregateAvro) {
    val phoneNumber1 =
        aggregate.phoneNumbers.firstOrNull { it.phoneNumberType.toString() == MOBILE.toString() }
    val phoneNumber2 =
        aggregate.phoneNumbers.firstOrNull {
          it.phoneNumberType.toString() == ORGANIZATION.toString()
        }
    assertThat(phoneNumber1).isNotNull
    assertThat(phoneNumber1!!.countryCode).isEqualTo("+351")
    assertThat(phoneNumber1.callNumber).isEqualTo("917533951")
    assertThat(phoneNumber2).isNotNull
    assertThat(phoneNumber2!!.getCountryCode()).isEqualTo("+49")
    assertThat(phoneNumber2.callNumber).isEqualTo("1657498728")
  }

  companion object {

    private val phoneNumbersDs =
        mutableSetOf(
            PhoneNumberDto("+351", MOBILE, "917533951"),
            PhoneNumberDto("+49", ORGANIZATION, "1657498728"))

    private fun <T> buildCommonUserFieldDescriptors(input: Class<T>): List<FieldDescriptor> =
        listOf(
            ConstrainedFields(input).withPath("gender").description("Gender of user").type(STRING),
            ConstrainedFields(input)
                .withPath("firstName")
                .description("First name of the user")
                .type(STRING),
            ConstrainedFields(input)
                .withPath("lastName")
                .description("Last name of the user")
                .type(STRING),
            ConstrainedFields(input)
                .withPath("position")
                .description("Position of the user")
                .type(STRING)
                .optional(),
            ConstrainedFields(input)
                .withPath("phoneNumbers")
                .description("Phone numbers of the user")
                .attributes(
                    key("constraints")
                        .value("Optional: There can only be a maximum of five phone numbers"))
                .type(ARRAY)
                .optional(),
            ConstrainedFields(input)
                .withPath("phoneNumbers[].countryCode")
                .description("Country code of the phone")
                .attributes(
                    key("constraints")
                        .value(
                            "Only support the following pattern: \"+\" followed by one to four digits [1-9]"))
                .type(STRING),
            ConstrainedFields(input)
                .withPath("phoneNumbers[].phoneNumberType")
                .description("Type of the phone")
                .attributes(
                    key("constraints")
                        .value(
                            "Valid values are: BUSINESS,HOME,MOBILE,FAX,PAGER,ORGANIZATION,ASSISTANT,OTHER"))
                .type(STRING),
            ConstrainedFields(input)
                .withPath("phoneNumbers[].phoneNumber")
                .description("Number of the phone")
                .attributes(
                    key("constraints")
                        .value("Only support the following pattern: one to fifteen  digits [0-9]"))
                .type(STRING),
            ConstrainedFields(input)
                .withPath("craftIds")
                .description("Crafts of the user")
                .attributes(key("constraints").value("Optional"))
                .type(ARRAY)
                .optional(),
            ConstrainedFields(input)
                .withPath("locale")
                .description("The preferred language of the user saved as a locale.")
                .type(STRING)
                .optional(),
            ConstrainedFields(input)
                .withPath("country")
                .description("The 2-digit ISO code of the country the user resides in.")
                .type(STRING)
                .optional(),
        )

    private val REGISTER_CURRENT_USER_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            *buildCommonUserFieldDescriptors(CreateCurrentUserResource::class.java).toTypedArray(),
            ConstrainedFields(CreateCurrentUserResource::class.java)
                .withPath("eulaAccepted")
                .description("Boolean to explicitly state that / if the user accepted the EULA")
                .attributes(key("constraints").value("EULA has to be accepted to register user."))
                .type(BOOLEAN)
                .optional())

    private val UPDATE_CURRENT_USER_REQUEST_FIELD_DESCRIPTORS =
        buildCommonUserFieldDescriptors(CreateCurrentUserResource::class.java)

    private val CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ID_AND_VERSION_FIELD_DESCRIPTORS,
            *COMMON_USER_RESPONSE_FIELD_DESCRIPTORS,
            subsectionWithPath("_links").ignored(),
            subsectionWithPath("_embedded")
                .optional()
                .description("Embedded resources")
                .type(JsonFieldType.OBJECT))

    private val CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS_FOR_ADMIN_USERS =
        listOf(
            *CURRENT_USER_RESPONSE_FIELD_DESCRIPTORS.toTypedArray(),
            *ADMIN_USER_RESPONSE_FIELD_DESCRIPTORS)

    private val CURRENT_USER_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
            linkWithRel(LINK_CURRENT_USER_UPDATE).description("Link to update the current user."))
  }
}
