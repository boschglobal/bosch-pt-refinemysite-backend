/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.projectmanagement.RandomData
import com.bosch.pt.csm.cloud.projectmanagement.application.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ParticipantMapping
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.UserRepository
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
@DisplayName("Verify statistics permissions")
internal class StatisticsServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: StatisticsService

  @Autowired private lateinit var participantMappingRepository: ParticipantMappingRepository

  @Autowired private lateinit var userRepository: UserRepository

  private val userMap: MutableMap<String, UserAccess> = HashMap()

  private val projectIdentifier = randomUUID()

  private val otherProjectIdentifier = randomUUID()

  @BeforeEach
  fun init() {
    initAdmin()
    doWithAuthorization(getUserAccess(ADMIN).user) {
      initCsm()
      initCr()
      initFm()
      initNotParticipant()
      initInactiveParticipant()
    }
  }

  @AfterEach
  fun cleanUp() {
    truncateDatabase()
  }

  // View overall PPC statistics permissions
  @TestFactory
  fun `view overall PPC is granted for`() =
      checkAccessWith(listOf(CSM, CR, FM, ADMIN), true) {
        cut.calculatePpc(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view overall PPC permission is denied for user not being project participant`() =
      checkAccessWith(listOf(NOT_PARTICIPANT), false) {
        cut.calculatePpc(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view overall PPC permission is denied for non-existing project`() =
      checkAccessWith(listOf(CSM), false) { cut.calculatePpc(randomUUID(), LocalDate.now(), 1) }

  // View grouped PPC statistics permissions
  @TestFactory
  fun `view grouped PPC is granted for`() =
      checkAccessWith(listOf(CSM, CR, FM, ADMIN), true) {
        cut.calculatePpcByCompanyAndCraft(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view grouped PPC permission is denied for user not being project participant`() =
      checkAccessWith(listOf(NOT_PARTICIPANT), false) {
        cut.calculatePpcByCompanyAndCraft(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view grouped PPC permission is denied for non-existing project`() =
      checkAccessWith(listOf(CSM), false) {
        cut.calculatePpcByCompanyAndCraft(randomUUID(), LocalDate.now(), 1)
      }

  // View overall RFV statistics permissions
  @TestFactory
  fun `view overall RFV permission is granted for`() =
      checkAccessWith(listOf(CSM, CR, FM, ADMIN), true) {
        cut.calculateRfv(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view overall RFV permission is denied for user not being project participant`() =
      checkAccessWith(listOf(NOT_PARTICIPANT), false) {
        cut.calculateRfv(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view overall RFV permission is denied for non-existing project`() =
      checkAccessWith(listOf(CSM), false) { cut.calculateRfv(randomUUID(), LocalDate.now(), 1) }

  // View grouped RFV statistics permissions
  @TestFactory
  fun `view grouped RFV permission is granted for`() =
      checkAccessWith(listOf(CSM, CR, FM, ADMIN), true) {
        cut.calculateRfvByCompanyAndCraft(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view grouped RFV permission is denied for user not being project participant`() =
      checkAccessWith(listOf(NOT_PARTICIPANT), false) {
        cut.calculateRfvByCompanyAndCraft(projectIdentifier, LocalDate.now(), 1)
      }

  @TestFactory
  fun `view grouped RFV permission is denied for non-existing project`() =
      checkAccessWith(listOf(CSM), false) {
        cut.calculateRfvByCompanyAndCraft(randomUUID(), LocalDate.now(), 1)
      }

  @TestFactory
  fun `access denied for inactive project participant`() =
      checkAccessWith(listOf(FM_INACTIVE), false) {
        cut.calculateRfvByCompanyAndCraft(randomUUID(), LocalDate.now(), 1)
      } +
          checkAccessWith(listOf(FM_INACTIVE), false) {
            cut.calculatePpcByCompanyAndCraft(randomUUID(), LocalDate.now(), 1)
          } +
          checkAccessWith(listOf(FM_INACTIVE), false) {
            cut.calculatePpc(randomUUID(), LocalDate.now(), 1)
          } +
          checkAccessWith(listOf(FM_INACTIVE), false) {
            cut.calculateRfv(randomUUID(), LocalDate.now(), 1)
          }

  private fun initAdmin() {
    userMap[ADMIN] = UserAccess(ADMIN, userRepository.save(RandomData.user()), true)
  }

  private fun initCsm() = initParticipant(CSM, CSM, projectIdentifier, true)

  private fun initCr() = initParticipant(CR, CR, projectIdentifier, true)

  private fun initFm() = initParticipant(FM, FM, projectIdentifier, true)

  private fun initNotParticipant() =
      initParticipant(NOT_PARTICIPANT, CSM, otherProjectIdentifier, true)

  private fun initInactiveParticipant() = initParticipant(FM_INACTIVE, FM, projectIdentifier, false)

  private fun initParticipant(
      alias: String,
      role: String,
      projectIdentifier: UUID,
      active: Boolean
  ) {
    val user = RandomData.user()
    createParticipantMapping(user, role, projectIdentifier, active)
    userMap[alias] = UserAccess(alias, user, false)
  }

  private fun createParticipantMapping(
      user: User,
      role: String,
      projectIdentifier: UUID,
      active: Boolean
  ) =
      participantMappingRepository.save(
          ParticipantMapping(randomUUID(), projectIdentifier, role, user.identifier, randomUUID())
              .also { it.active = active })

  companion object {
    private const val CSM = "CSM"
    private const val CR = "CR"
    private const val FM = "FM"
    private const val FM_INACTIVE = "FM_INACTIVE"
    private const val NOT_PARTICIPANT = "NOT_PARTICIPANT"
    private const val ADMIN = "ADMIN"
  }

  override fun getUserAccess(alias: String): UserAccess {
    return userMap[alias]!!
  }
}
