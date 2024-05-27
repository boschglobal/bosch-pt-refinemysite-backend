/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.template.setupUsersAndCompanies
import com.bosch.pt.csm.cloud.projectmanagement.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import java.util.TimeZone
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Value

open class BaseNotificationTest : AbstractEventStreamIntegrationTest() {

  @Value("\${testadmin.user.id}") lateinit var testadminUserId: String

  @Value("\${testadmin.user.identifier}") lateinit var testadminUserIdentifier: String

  lateinit var csmUser: User
  lateinit var otherCsmUser: User
  lateinit var crUser: User
  lateinit var otherCompanyCrUser: User
  lateinit var fmUser: User
  lateinit var otherCrUser: User
  lateinit var otherCompanyFmUser: User

  val compactedUserIdentifier: AggregateIdentifierAvro =
      AggregateIdentifierAvro(randomString(), 0, UsermanagementAggregateTypeEnum.USER.value)

  @BeforeEach
  open fun setup() {
    assertThat(repositories.userRepository.findAll()).hasSize(0)
    assertThat(repositories.companyRepository.findAll()).hasSize(0)
    assertThat(repositories.employeeRepository.findAll()).hasSize(0)
    assertThat(repositories.projectRepository.findAll()).hasSize(0)
    assertThat(repositories.participantRepository.findAll()).hasSize(0)
    assertThat(repositories.projectCraftRepository.findAll()).hasSize(0)
    assertThat(repositories.workAreaRepository.findAll()).hasSize(0)
    assertThat(repositories.workAreaListRepository.findAll()).hasSize(0)
    assertThat(repositories.taskRepository.findAll()).hasSize(0)
    assertThat(repositories.topicRepository.findAll()).hasSize(0)
    assertThat(repositories.messageRepository.findAll()).hasSize(0)
    assertThat(repositories.taskScheduleRepository.findAll()).hasSize(0)
    assertThat(repositories.dayCardRepository.findAll()).hasSize(0)
    assertThat(repositories.taskAttachmentRepository.findAll()).hasSize(0)
    assertThat(repositories.topicAttachmentRepository.findAll()).hasSize(0)
    assertThat(repositories.messageAttachmentRepository.findAll()).hasSize(0)
    assertThat(repositories.milestoneRepository.findAll()).hasSize(0)

    eventStreamGenerator.setupUsersAndCompanies(testadminUserId, testadminUserIdentifier)

    csmUser = findUser(getByReference(CSM_USER))
    otherCsmUser = findUser(getByReference(OTHER_CSM_USER))
    crUser = findUser(getByReference(CR_USER))
    otherCompanyCrUser = findUser(getByReference(OTHER_COMPANY_CR_USER))
    fmUser = findUser(getByReference(FM_USER))
    otherCrUser = findUser(getByReference(OTHER_CR_USER))
    otherCompanyFmUser = findUser(getByReference(OTHER_COMPANY_FM_USER))
  }

  fun AggregateIdentifierAvro.toAggregateIdentifier(): AggregateIdentifier =
      AggregateIdentifier(
          identifier = UUID.fromString(getIdentifier()), type = getType(), version = getVersion())

  private fun findUser(aggregateIdentifierAvro: AggregateIdentifierAvro) =
      repositories.userRepository.findOneCachedByIdentifier(
          aggregateIdentifierAvro.getIdentifier().toUUID())!!

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
    const val COMPANY: String = "company"
    const val CSM_USER: String = "csm-user"
    const val CSM_EMPLOYEE: String = "csm-employee"
    const val CSM_PARTICIPANT: String = "csm-participant"
    const val OTHER_CSM_USER: String = "other-csm-user"
    const val OTHER_CSM_PARTICIPANT: String = "other-csm-participant"
    const val CR_USER: String = "cr-user"
    const val CR_PARTICIPANT: String = "cr-participant"
    const val CR_USER_INACTIVE: String = "cr-user-inactive"
    const val CR_PARTICIPANT_INACTIVE: String = "cr-participant-inactive"
    const val OTHER_CR_USER: String = "other-cr-user"
    const val OTHER_CR_PARTICIPANT: String = "other-cr-participant"
    const val FM_USER: String = "fm-user"
    const val FM_EMPLOYEE: String = "fm-employee"
    const val FM_PARTICIPANT: String = "fm-participant"
    const val OTHER_FM_USER: String = "other-fm-user"
    const val OTHER_FM_PARTICIPANT: String = "other-fm-participant"
    const val FM_USER_INACTIVE: String = "fm-user-inactive"
    const val FM_PARTICIPANT_INACTIVE: String = "fm-participant-inactive"
    const val COMPANY_2: String = "company2"
    const val OTHER_COMPANY_CR_USER: String = "other-company-cr-user"
    const val OTHER_COMPANY_CR_PARTICIPANT: String = "other-company-cr-participant"
    const val OTHER_COMPANY_FM_USER: String = "other-company-fm-user"
    const val OTHER_COMPANY_FM_PARTICIPANT: String = "other-company-fm-participant"

    const val PROJECT: String = "project"
    const val PROJECT_CRAFT: String = "projectCraft"
    const val MILESTONE: String = "milestone"
  }
}
