/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.projectmanagement.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.test.Repositories
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.core.context.SecurityContextHolder

@EnableKafkaListeners
@AutoConfigureMockMvc
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractIntegrationTest {

  @Autowired lateinit var eventStreamGenerator: EventStreamGenerator

  @Autowired lateinit var repositories: Repositories

  @Autowired lateinit var eventStreamContext: ProjectTimeSeriesApiServiceEventStreamContext

  @Value("\${testadmin.user.id}") lateinit var testadminUserId: String

  @Value("\${testadmin.user.identifier}") lateinit var testadminUserIdentifier: String

  val context: MutableMap<String, SpecificRecordBase> by lazy { eventStreamContext.events }

  @BeforeEach
  fun setupBase() {
    eventStreamGenerator
        .registerStaticContext()
        .reset()
        .submitUserAndActivate(asReference = "testadmin") {
          it.aggregateIdentifierBuilder.identifier = testadminUserIdentifier
          it.userId = testadminUserId
        }
        .submitUser(asReference = "csm-user") {
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
        }
        .submitCompanyWithBothAddresses(asReference = "company")
        .setUserContext("csm-user")
  }

  @AfterEach
  fun cleanupBase() {
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  fun EventStreamGenerator.submitCsmParticipant() =
      submitParticipantG3(asReference = "csm-participant") {
        it.user = getByReference("csm-user")
        it.role = ParticipantRoleEnumAvro.CSM
      }

  protected fun setAuthentication(userReference: String) =
      setAuthentication(eventStreamGenerator.getIdentifier(userReference))

  protected fun setAuthentication(userIdentifier: UUID) =
      repositories.userRepository.findOneByIdentifier(userIdentifier.asUserId())!!.also {
        authorizeWithUser(it, it.admin)
      }
}
