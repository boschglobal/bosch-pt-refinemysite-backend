/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import org.junit.jupiter.api.BeforeEach
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath

abstract class AbstractUserApiDocumentationTest : AbstractApiDocumentationTest() {

  lateinit var craft1: Craft
  lateinit var craft2: Craft

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitCraft("electricity") { it.defaultName = "Electricity" }
        .submitCraft("plumbing") { it.defaultName = "Plumbing" }

    craft1 =
        repositories.craftRepository.findOneByIdentifier(
            CraftId(eventStreamGenerator.getIdentifier("electricity"))
        )!!
    craft2 =
        repositories.craftRepository.findOneByIdentifier(
            CraftId(eventStreamGenerator.getIdentifier("plumbing"))
        )!!

    userEventStoreUtils.reset()
  }

  companion object {

    @JvmStatic
    protected val COMMON_USER_RESPONSE_FIELD_DESCRIPTORS =
        arrayOf(
            fieldWithPath("gender").description("Gender of user").type(STRING),
            fieldWithPath("firstName").description("First name of the user").type(STRING),
            fieldWithPath("lastName").description("Last name of the user").type(STRING),
            fieldWithPath("email").description("Email of the user").type(STRING),
            fieldWithPath("position").description("Position of the user").type(STRING),
            fieldWithPath("eulaAccepted")
                .description("Boolean to explicitly state that / if the user accepted the EULA")
                .type(BOOLEAN),
            fieldWithPath("locale")
                .description(
                    "The preferred language of the user saved as a locale. This is also used to render date and time.")
                .type(STRING)
                .optional(),
            fieldWithPath("country")
                .description("The 2-digit ISO code of the country the user resides in.")
                .type(STRING)
                .optional(),
            fieldWithPath("crafts").description("Crafts of the user").type(ARRAY),
            fieldWithPath("crafts[].displayName").description("Name of the craft").type(STRING),
            fieldWithPath("crafts[].id").description("ID of the craft").type(STRING),
            subsectionWithPath("phoneNumbers").description("Phone numbers of the user").type(ARRAY),
        )

    @JvmStatic
    protected val ADMIN_USER_RESPONSE_FIELD_DESCRIPTORS =
        arrayOf(
            fieldWithPath("admin")
                .description("Flag if user has administrative rights")
                .type(BOOLEAN),
            fieldWithPath("registered")
                .description("Flag if user has finished the user registration")
                .type(BOOLEAN))
  }
}
