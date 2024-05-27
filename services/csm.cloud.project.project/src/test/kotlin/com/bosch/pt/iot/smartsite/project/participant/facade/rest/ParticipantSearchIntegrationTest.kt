/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.SearchParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant.Companion.STATUS
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant.Companion.USER_FIRST_NAME
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant.Companion.USER_LAST_NAME
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.http.HttpStatus.OK

@DisplayName("Verify participant search operations")
@EnableAllKafkaListeners
class ParticipantSearchIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var participantController: ParticipantController

  private val userFmCompanyA by lazy { repositories.findUser(getIdentifier("userFmCompanyA"))!! }
  private val userCrCompanyA by lazy { repositories.findUser(getIdentifier("userCrCompanyA"))!! }
  private val userCsmCompanyA by lazy { repositories.findUser(getIdentifier("userCsmCompanyA"))!! }
  private val companyA by lazy { repositories.findCompany(getIdentifier("companyA"))!! }
  private val companyB by lazy { repositories.findCompany(getIdentifier("companyA"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val participantFmCompanyA by lazy {
    repositories.findParticipant(getIdentifier("participantFmCompanyA"))!!
  }
  private val participantFm2CompanyA by lazy {
    repositories.findParticipant(getIdentifier("participantFm2CompanyA"))!!
  }
  private val participantFmDeletedCompanyA by lazy {
    repositories.findParticipant(getIdentifier("participantFmDeletedCompanyA"))!!
  }
  private val participantFmCompanyB by lazy {
    repositories.findParticipant(getIdentifier("participantFmCompanyB"))!!
  }
  private val participantFmInvited by lazy {
    repositories.findParticipant(getIdentifier("participantFmInvited"))!!
  }
  private val participantFmInValidation by lazy {
    repositories.findParticipant(getIdentifier("participantFmInValidation"))!!
  }
  private val participantCrCompanyA by lazy {
    repositories.findParticipant(getIdentifier("participantCrCompanyA"))!!
  }
  private val participantCrCompanyB by lazy {
    repositories.findParticipant(getIdentifier("participantCrCompanyB"))!!
  }
  private val participantCsmCompanyA by lazy {
    repositories.findParticipant(getIdentifier("participantCsmCompanyA"))!!
  }
  private val participantCsmCompanyB by lazy {
    repositories.findParticipant(getIdentifier("participantCsmCompanyB"))!!
  }

  @BeforeEach
  fun setup() {
    val invitedEmailAddress = "05@test.com"
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany("companyA") { it.name = "Company A" }
        .submitProject()
        .submitUserEmployeeAndParticipant(
            "FmCompanyA", "fm", "companyA", "04@test.com", ParticipantRoleEnumAvro.FM)
        .submitUserEmployeeAndParticipant(
            "Fm2CompanyA", "fm2", "companyA", "03@test.com", ParticipantRoleEnumAvro.FM)
        .submitUserEmployeeAndParticipant(
            "CrCompanyA", "cr", "companyA", "06@test.com", ParticipantRoleEnumAvro.CR)
        .submitUserEmployeeAndParticipant(
            "CsmCompanyA", "csm", "companyA", "08@test.com", ParticipantRoleEnumAvro.CSM)
        .submitUserEmployeeAndParticipant(
            "FmDeletedCompanyA",
            "fm",
            "deletedCompanyA",
            "01@test.com",
            ParticipantRoleEnumAvro.FM,
            ParticipantStatusEnumAvro.INACTIVE)
        .submitUserTombstones("userFmDeletedCompanyA")
        .submitUserEmployeeAndParticipant(
            "FmInValidation",
            "Hans",
            "Invalidation",
            "10@test.com",
            ParticipantRoleEnumAvro.FM,
            ParticipantStatusEnumAvro.VALIDATION)
        .submitParticipantG3("participantFmInvited") {
          it.user = null
          it.company = null
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INVITED
        }
        .submitInvitation { it.email = invitedEmailAddress }
        .submitCompany("companyB") { it.name = "Company B" }
        .submitUserEmployeeAndParticipant(
            "FmCompanyB", "fm", "companyB", "02@test.com", ParticipantRoleEnumAvro.FM)
        .submitUserEmployeeAndParticipant(
            "CrCompanyB", "cr", "companyB", "07@test.com", ParticipantRoleEnumAvro.CR)
        .submitUserEmployeeAndParticipant(
            "CsmCompanyB", "csm", "companyB", "09@test.com", ParticipantRoleEnumAvro.CSM)

    setAuthentication(userFmCompanyA.identifier!!)
    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
  }

  @AfterEach
  fun verifyEventStoreEmpty() {
    projectEventStoreUtils.verifyEmpty()
    invitationEventStoreUtils.verifyEmpty()
  }

  @Test
  fun verifyFindParticipantById() {
    val response = participantController.findParticipantById(participantFmCompanyA.identifier)
    assertThat(response.statusCode).isEqualTo(OK)

    val participantResource = response.body
    assertThat(participantResource).isNotNull

    assertThat(participantResource!!.identifier)
        .isEqualTo(participantFmCompanyA.identifier.toUuid())
    assertThat(participantResource.project.identifier.asProjectId())
        .isEqualTo(participantFmCompanyA.project!!.identifier)
    assertThat(participantResource.user!!.identifier)
        .isEqualTo(participantFmCompanyA.user!!.identifier)
    assertThat(participantResource.projectRole).isEqualTo(participantFmCompanyA.role)
    assertThat(participantResource.company!!.identifier)
        .isEqualTo(participantFmCompanyA.company!!.identifier)
  }

  @Nested
  internal inner class VerifyFindAllAssignableParticipants {
    @Test
    fun findAllAssignableParticipantsForCsm() {
      authorizeWithUser(userCsmCompanyA)
      val response =
          participantController.findAllAssignableParticipants(
              project.identifier, null, PAGE_REQUEST_SORTING_BY_USERNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(7)
          .extracting("identifier")
          .contains(
              participantCsmCompanyA.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid())
    }

    @Test
    fun findAllAssignableParticipantsForCsmAndCompanyA() {
      authorizeWithUser(userCsmCompanyA)
      val response =
          participantController.findAllAssignableParticipants(
              project.identifier,
              companyA.identifier!!.asCompanyId(),
              PAGE_REQUEST_SORTING_BY_USERNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(4)
          .extracting("identifier")
          .contains(
              participantCsmCompanyA.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid())
    }

    @Test
    fun findAllAssignableParticipantsForCr() {
      authorizeWithUser(userCrCompanyA)
      val response =
          participantController.findAllAssignableParticipants(
              project.identifier, null, PAGE_REQUEST_SORTING_BY_USERNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(4)
          .extracting("identifier")
          .contains(
              participantCrCompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid())
    }

    @Test
    fun findAllAssignableParticipantsForCrAndCompanyA() {
      authorizeWithUser(userCrCompanyA)
      val response =
          participantController.findAllAssignableParticipants(
              project.identifier,
              companyA.identifier!!.asCompanyId(),
              PAGE_REQUEST_SORTING_BY_USERNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(4)
          .extracting("identifier")
          .contains(
              participantCrCompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid())
    }

    @Test
    fun findAllAssignableParticipantsForFm() {
      authorizeWithUser(userFmCompanyA)
      val response =
          participantController.findAllAssignableParticipants(
              project.identifier, null, PAGE_REQUEST_SORTING_BY_USERNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(1)
          .extracting("identifier")
          .contains(participantFmCompanyA.identifier.toUuid())
    }

    @Test
    fun findAllAssignableParticipantsForFmAndCompanyA() {
      authorizeWithUser(userFmCompanyA)
      val response =
          participantController.findAllAssignableParticipants(
              project.identifier,
              companyA.identifier!!.asCompanyId(),
              PAGE_REQUEST_SORTING_BY_USERNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(1)
          .extracting("identifier")
          .contains(participantFmCompanyA.identifier.toUuid())
    }
  }

  @Nested
  internal inner class VerifyFindAllAssignableCompanies {
    @Test
    fun findAllAssignableCompaniesForCsm() {
      authorizeWithUser(userCsmCompanyA)
      val response =
          participantController.findAllAssignableCompanies(
              project.identifier, false, PAGE_REQUEST_SORTED_BY_DISPLAYNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.companies)
          .hasSize(2)
          .extracting("identifier")
          .contains(companyA.identifier!!, companyB.identifier!!)
    }

    @Test
    fun findAllAssignableCompaniesForCr() {
      authorizeWithUser(userCrCompanyA)
      val response =
          participantController.findAllAssignableCompanies(
              project.identifier, false, PAGE_REQUEST_SORTED_BY_DISPLAYNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.companies)
          .hasSize(1)
          .extracting("identifier")
          .contains(companyA.identifier)
    }

    @Test
    fun findAllAssignableCompaniesForFm() {
      authorizeWithUser(userFmCompanyA)
      val response =
          participantController.findAllAssignableCompanies(
              project.identifier, false, PAGE_REQUEST_SORTED_BY_DISPLAYNAME)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.companies)
          .hasSize(1)
          .extracting("identifier")
          .contains(companyA.identifier)
    }
  }

  @Nested
  internal inner class VerifyFindAllParticipantsFiltered {
    @Test
    fun withNoFilterApplied() {
      val searchParticipantResource = SearchParticipantResource(null, null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(10)
          .extracting("identifier")
          .containsExactly(
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantCsmCompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byCompany() {
      val searchParticipantResource = SearchParticipantResource(null, companyA.identifier, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(5)
          .extracting("identifier")
          .containsExactly(
              participantCrCompanyA.identifier.toUuid(),
              participantCsmCompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byRoleFM() {
      val searchParticipantResource = SearchParticipantResource(null, null, setOf(FM))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(6)
          .extracting("identifier")
          .containsExactly(
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byRoleCR() {
      val searchParticipantResource = SearchParticipantResource(null, null, setOf(CR))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(2)
          .extracting("identifier")
          .containsExactly(
              participantCrCompanyA.identifier.toUuid(), participantCrCompanyB.identifier.toUuid())
    }

    @Test
    fun byRoleCsm() {
      val searchParticipantResource = SearchParticipantResource(null, null, setOf(CSM))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(2)
          .extracting("identifier")
          .containsExactly(
              participantCsmCompanyA.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid())
    }

    @Test
    fun byMultipleRoles() {
      val searchParticipantResource = SearchParticipantResource(null, null, setOf(CR, CSM))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(4)
          .extracting("identifier")
          .containsExactly(
              participantCrCompanyA.identifier.toUuid(),
              participantCsmCompanyA.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid())
    }

    @Test
    fun byStatus() {
      val searchParticipantResource = SearchParticipantResource(setOf(INVITED), null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(1)
          .extracting("identifier")
          .containsExactly(participantFmInvited.identifier.toUuid())
    }

    @Test
    fun byMultipleStatus() {
      val searchParticipantResource =
          SearchParticipantResource(setOf(VALIDATION, INACTIVE), null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(2)
          .extracting("identifier")
          .containsExactly(
              participantFmInValidation.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byStatusAndCompany() {
      val searchParticipantResource =
          SearchParticipantResource(
              setOf(INVITED, VALIDATION, ACTIVE, INACTIVE), companyA.identifier, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(5)
          .extracting("identifier")
          .containsExactly(
              participantCrCompanyA.identifier.toUuid(),
              participantCsmCompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byStatusAndRole() {
      val searchParticipantResource =
          SearchParticipantResource(setOf(INVITED, VALIDATION, ACTIVE, INACTIVE), null, setOf(FM))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(6)
          .extracting("identifier")
          .containsExactly(
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byCompanyAndRole() {
      val searchParticipantResource =
          SearchParticipantResource(null, companyA.identifier, setOf(CR))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(1)
          .extracting("identifier")
          .contains(participantCrCompanyA.identifier.toUuid())
    }

    @Test
    fun byStatusAndCompanyAndRole() {
      val searchParticipantResource =
          SearchParticipantResource(
              setOf(INVITED, VALIDATION, ACTIVE, INACTIVE), companyA.identifier, setOf(FM))
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier, searchParticipantResource, PAGE_REQUEST_DEFAULT)

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(3)
          .extracting("identifier")
          .containsExactly(
              participantFmCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }
  }

  @Nested
  inner class VerifyFindAllParticipantsSorted {

    @Test
    fun byStatus() {
      val searchParticipantResource = SearchParticipantResource(null, null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier,
              searchParticipantResource,
              PageRequest.of(0, 10, Sort.by(ASC, "status, email")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(10)
          .extracting("identifier")
          .containsExactly(
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid(),
              participantCsmCompanyA.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid())
    }

    @Test
    fun byRole() {
      val searchParticipantResource = SearchParticipantResource(null, null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier,
              searchParticipantResource,
              PageRequest.of(0, 10, Sort.by(ASC, "role, email")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(10)
          .extracting("identifier")
          .containsExactly(
              participantCsmCompanyA.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid())
    }

    @Test
    fun byCompanyName() {
      val searchParticipantResource = SearchParticipantResource(null, null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier,
              searchParticipantResource,
              PageRequest.of(0, 10, Sort.by(ASC, "company.displayName, email")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(10)
          .extracting("identifier")
          .containsExactly(
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid(),
              participantFmDeletedCompanyA.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantCsmCompanyA.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid(),
              participantCsmCompanyB.identifier.toUuid())
    }

    @Test
    fun byUserName() {
      val searchParticipantResource = SearchParticipantResource(null, null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier,
              searchParticipantResource,
              PageRequest.of(0, 4, Sort.by(ASC, "status, user.firstName, user.lastName")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(4)
          .extracting("identifier")
          .containsExactly(
              participantFmInvited.identifier.toUuid(),
              participantFmInValidation.identifier.toUuid(),
              participantCrCompanyA.identifier.toUuid(),
              participantCrCompanyB.identifier.toUuid())
    }

    @Test
    fun byEmail() {
      val searchParticipantResource = SearchParticipantResource(null, null, null)
      val response =
          participantController.findAllParticipantsWithFilters(
              project.identifier,
              searchParticipantResource,
              PageRequest.of(0, 5, Sort.by(ASC, "email")))

      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body!!.items)
          .hasSize(5)
          .extracting("identifier")
          .containsExactly(
              participantFmDeletedCompanyA.identifier.toUuid(),
              participantFmCompanyB.identifier.toUuid(),
              participantFm2CompanyA.identifier.toUuid(),
              participantFmCompanyA.identifier.toUuid(),
              participantFmInvited.identifier.toUuid())
    }
  }

  companion object {
    private val PAGE_REQUEST_SORTING_BY_USERNAME: Pageable =
        PageRequest.of(0, 10, Sort.by(ASC, USER_LAST_NAME, USER_FIRST_NAME))

    private val PAGE_REQUEST_DEFAULT: Pageable =
        PageRequest.of(0, 10, Sort.by(ASC, STATUS, USER_LAST_NAME, USER_FIRST_NAME))

    private val PAGE_REQUEST_SORTED_BY_DISPLAYNAME: Pageable =
        PageRequest.of(0, 10, Sort.by(ASC, USER_FIRST_NAME, USER_LAST_NAME))
  }
}

private fun EventStreamGenerator.submitUserEmployeeAndParticipant(
    referenceSuffix: String,
    firstName: String,
    lastName: String,
    email: String,
    participantRole: ParticipantRoleEnumAvro,
    participantStatus: ParticipantStatusEnumAvro = ParticipantStatusEnumAvro.ACTIVE,
): EventStreamGenerator {
  return submitUser("user$referenceSuffix") {
        it.firstName = firstName
        it.lastName = lastName
        it.email = email
      }
      .submitEmployee("employee$referenceSuffix")
      .submitParticipantG3("participant$referenceSuffix") {
        it.role = participantRole
        it.status = participantStatus
      }
}
