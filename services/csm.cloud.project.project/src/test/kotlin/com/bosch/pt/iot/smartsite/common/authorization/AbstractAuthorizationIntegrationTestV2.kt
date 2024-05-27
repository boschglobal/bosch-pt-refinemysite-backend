/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.ProjectReferencedAggregateTypesEnum.USER
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.stream.Stream
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.tuple.Pair
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
abstract class AbstractAuthorizationIntegrationTestV2 : AbstractIntegrationTestV2() {

  var userMap = mutableMapOf<String, Pair<User?, Boolean>>()

  protected val userCreator by lazy { getUser("userCreator") }
  protected val userFm by lazy { getUser("userFm") }
  protected val userFmAssignee by lazy { getUser("userFmAssignee") }
  protected val userFmReassigned by lazy { getUser("userFmReassigned") }
  protected val userCr by lazy { getUser("userCr") }
  protected val userCsm by lazy { getUser("userCsm") }
  protected val userOtherCompanyFm by lazy { getUser("userOtherCompanyFm") }
  protected val userOtherCompanyCr by lazy { getUser("userOtherCompanyCr") }
  protected val userOtherCompanyCsm by lazy { getUser("userOtherCompanyCsm") }
  protected val userNotParticipant by lazy { getUser("userNotParticipant") }
  protected val userFmInactive by lazy { getUser("userFmInactive") }
  protected val userCrInactive by lazy { getUser("userCrInactive") }
  protected val userAdmin by lazy { getUser("userAdmin") }

  protected val project by lazy { getProject("project") }

  @BeforeEach
  protected fun initAbstractAuthorizationIntegrationTest() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUser(asReference = "userCreator")
        .submitUser(asReference = "userFm")
        .submitUser(asReference = "userFmAssignee")
        .submitUser(asReference = "userFmReassigned")
        .submitUser(asReference = "userCr")
        .submitUser(asReference = "userCsm")
        .submitUser(asReference = "userOtherCompanyFm")
        .submitUser(asReference = "userOtherCompanyCr")
        .submitUser(asReference = "userOtherCompanyCsm")
        .submitUser(asReference = "userNotParticipant")
        .submitUser(asReference = "userFmInactive") { it.locked = true }
        .submitUser(asReference = "userCrInactive") { it.locked = true }
        .submitUser(asReference = "userAdmin") { it.admin = true }
        // Submit one company and all the corresponding employees for that company
        .submitCompanyWithBothAddresses(asReference = "company")
        .submitEmployee(asReference = "employeeCreator") {
          it.user = getByReference("userCreator")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeFm") {
          it.user = getByReference("userFm")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeFmAssignee") {
          it.user = getByReference("userFmAssignee")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeFmReassigned") {
          it.user = getByReference("userFmReassigned")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeCr") {
          it.user = getByReference("userCr")
          it.roles = listOf(EmployeeRoleEnumAvro.CR)
        }
        .submitEmployee(asReference = "employeeCsm") {
          it.user = getByReference("userCsm")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        .submitEmployee(asReference = "employeeNotParticipant") {
          it.user = getByReference("userNotParticipant")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeFmInactive") {
          it.user = getByReference("userFmInactive")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeCrInactive") {
          it.user = getByReference("userCrInactive")
          it.roles = listOf(EmployeeRoleEnumAvro.CR)
        }
        // Submit other company with the corresponding employees for test differentiation
        .submitCompanyWithBothAddresses(asReference = "otherCompany")
        .submitEmployee(asReference = "employeeOtherCompanyFm") {
          it.user = getByReference("userOtherCompanyFm")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .submitEmployee(asReference = "employeeOtherCompanyCr") {
          it.user = getByReference("userOtherCompanyCr")
          it.roles = listOf(EmployeeRoleEnumAvro.CR)
        }
        .submitEmployee(asReference = "employeeOtherCompanyCsm") {
          it.user = getByReference("userOtherCompanyCsm")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        // Submit a project with the corresponding participants
        .submitProject(asReference = "project")
        .submitWorkdayConfiguration(asReference = "workdayConfiguration")
        .submitParticipantG3(asReference = "participantCsm") {
          it.user = getByReference("userCsm")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.CSM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantCr") {
          it.user = getByReference("userCr")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.CR
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantCreator") {
          it.user = getByReference("userCreator")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantFm") {
          it.user = getByReference("userFm")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantFmAssignee") {
          it.user = getByReference("userFmAssignee")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantFmReassignedBefore") {
          it.user = getByReference("userFmReassigned")
          it.company = getByReference("otherCompany")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitParticipantG3(asReference = "participantFmReassigned") {
          it.user = getByReference("userFmReassigned")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantOtherCompanyFm") {
          it.user = getByReference("userOtherCompanyFm")
          it.company = getByReference("otherCompany")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantOtherCompanyCsm") {
          it.user = getByReference("userOtherCompanyCsm")
          it.company = getByReference("otherCompany")
          it.role = ParticipantRoleEnumAvro.CSM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantOtherCompanyCr") {
          it.user = getByReference("userOtherCompanyCr")
          it.company = getByReference("otherCompany")
          it.role = ParticipantRoleEnumAvro.CR
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        // Submit other project with the corresponding participants for test differentiation
        .submitProject(asReference = "otherProject")
        .submitWorkdayConfiguration(asReference = "otherWorkdayConfiguration")
        .submitParticipantG3(asReference = "participantNp") {
          it.user = getByReference("userNotParticipant")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.CSM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }
        .submitParticipantG3(asReference = "participantCrInactive") {
          it.user = getByReference("userCrInactive")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.CR
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitParticipantG3(asReference = "participantFmInactive") {
          it.user = getByReference("userFmInactive")
          it.company = getByReference("company")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        // Set proper default user and default project to be used by events submitted
        // in sub-classes when the usr/project is not specified
        .setUserContext("userCreator")
        .setLastIdentifierForType(USER.value, userCreator.toAggregateIdentifier())
        .setLastIdentifierForType(PROJECT.value, project.identifier.toAggregateReference())

    userMap[FM_CREATOR] = Pair.of(userCreator, false)
    userMap[FM] = Pair.of(userFm, false)
    userMap[FM_ASSIGNEE] = Pair.of(userFmAssignee, false)
    userMap[FM_REASSIGNED] = Pair.of(userFmReassigned, false)
    userMap[CR] = Pair.of(userCr, false)
    userMap[CSM] = Pair.of(userCsm, false)
    userMap[OTHER_FM] = Pair.of(userOtherCompanyFm, false)
    userMap[OTHER_CR] = Pair.of(userOtherCompanyCr, false)
    userMap[OTHER_CSM] = Pair.of(userOtherCompanyCsm, false)
    userMap[NOT_PARTICIPANT] = Pair.of(userNotParticipant, false)
    userMap[FM_INACTIVE] = Pair.of(userFmInactive, false)
    userMap[CR_INACTIVE] = Pair.of(userCrInactive, false)
    userMap[ADMIN] = Pair.of(userAdmin, true)
    userMap[ANONYM] = Pair.of(null, false)

    authorizeWithUser(userCreator)
  }

  protected fun checkAccessWith(userTypeAccess: UserTypeAccess, runnable: Runnable) =
      checkAccessWith(userTypeAccess.userType, userTypeAccess.isAccessGranted, runnable)

  protected fun checkAccessWith(userRole: String, isAccessGranted: Boolean, procedure: Runnable) =
      if (isAccessGranted) {
        doWithAuthorization(userMap[userRole]!!, procedure)
      } else {
        doWithAuthorization(userMap[userRole]!!) {
          assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
            procedure.run()
          }
        }
      }

  private fun getUser(reference: String) = repositories.findUser(getIdentifier(reference))!!

  private fun getProject(reference: String) =
      repositories.findProject(getIdentifier(reference).asProjectId())!!

  companion object {
    @JvmStatic protected val FM_CREATOR = "FM_CREATOR"
    @JvmStatic protected val FM = "FM"
    @JvmStatic protected val FM_ASSIGNEE = "FM_ASSIGNEE"
    @JvmStatic protected val FM_REASSIGNED = "FM_REASSIGNED"
    @JvmStatic protected val CR = "CR"
    @JvmStatic protected val CSM = "CSM"
    @JvmStatic protected val OTHER_FM = "OTHER_FM"
    @JvmStatic protected val OTHER_CR = "OTHER_CR"
    @JvmStatic protected val OTHER_CSM = "OTHER_CSM"
    @JvmStatic protected val ADMIN = "ADMIN"
    @JvmStatic protected val NOT_PARTICIPANT = "NOT_PARTICIPANT"
    @JvmStatic protected val FM_INACTIVE = "FM_INACTIVE"
    @JvmStatic protected val CR_INACTIVE = "CR_INACTIVE"
    @JvmStatic protected val ANONYM = "ANONYM"

    @JvmStatic
    protected val userTypes =
        arrayOf(
            FM_CREATOR,
            FM,
            FM_ASSIGNEE,
            FM_REASSIGNED,
            CR,
            CSM,
            OTHER_FM,
            OTHER_CR,
            OTHER_CSM,
            NOT_PARTICIPANT,
            FM_INACTIVE,
            CR_INACTIVE,
            ADMIN)

    @JvmStatic
    protected fun adminOnly(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(arrayOf(CSM, ADMIN), setOf("ADMIN"))

    @JvmStatic
    protected fun noOneWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, emptySet())

    @JvmStatic
    protected fun noOneWithoutAdminAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(
            ArrayUtils.remove(userTypes, ArrayUtils.indexOf(userTypes, ADMIN)), emptySet())

    @JvmStatic
    protected fun allWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(ArrayUtils.add(userTypes, ANONYM), userTypes.toSet())

    @JvmStatic
    protected fun allAndAnonymous(): Stream<UserTypeAccess> =
        with(ArrayUtils.add(userTypes, ANONYM)) {
          UserTypeAccess.createGrantedGroup(this, this.toSet())
        }

    @JvmStatic
    protected fun allActiveParticipantsWithAccessAndAdmin(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(
            userTypes,
            setOf(
                FM_CREATOR,
                FM,
                FM_ASSIGNEE,
                FM_REASSIGNED,
                CR,
                CSM,
                OTHER_FM,
                OTHER_CR,
                OTHER_CSM,
                ADMIN))
    @JvmStatic
    protected fun allActiveParticipantsWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(
            userTypes,
            setOf(
                CSM, OTHER_CSM, CR, FM, FM_ASSIGNEE, FM_REASSIGNED, FM_CREATOR, OTHER_FM, OTHER_CR))

    @JvmStatic
    protected fun csmWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM))

    @JvmStatic
    protected fun csmAndCrWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, CR))

    @JvmStatic
    protected fun csmAndCrAndCreatorWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, CR, FM_CREATOR))

    @JvmStatic
    protected fun csmAndOtherCompanyCrWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, OTHER_CR))

    @JvmStatic
    protected fun csmAndCrAndAssignedFmWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, CR, FM_ASSIGNEE))

    @JvmStatic
    protected fun csmAndOtherCompanyWithAccess(): Stream<UserTypeAccess> =
        UserTypeAccess.createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, OTHER_CR, OTHER_FM))
  }
}
