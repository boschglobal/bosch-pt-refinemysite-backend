/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.projectmanagement.attachment.service.BlobStoreService
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import io.mockk.clearMocks
import io.mockk.every
import java.net.URL
import org.apache.avro.specific.SpecificRecordBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@EnableKafkaListeners
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractBaseIntegrationTest {

  @Autowired lateinit var eventStreamGenerator: EventStreamGenerator

  @Autowired lateinit var eventStreamContext: ActivityServiceEventStreamContext

  @Autowired lateinit var blobStoreService: BlobStoreService

  @Value("\${testadmin.user.id}") lateinit var testadminUserId: String

  @Value("\${testadmin.user.identifier}") lateinit var testadminUserIdentifier: String

  val context: MutableMap<String, SpecificRecordBase> by lazy { eventStreamContext.events }

  val crUser by lazy { context["cr-user"] as UserAggregateAvro }
  val csmUser by lazy { context["csm-user"] as UserAggregateAvro }
  val fmUser by lazy { context["fm-user"] as UserAggregateAvro }
  val fmUserInactive by lazy { context["fm-user-inactive"] as UserAggregateAvro }
  val fmUserNoParticipant by lazy { context["fm-user-no-participant"] as UserAggregateAvro }
  val otherProjectUser by lazy { context["other-project-user"] as UserAggregateAvro }
  val testAdminUser by lazy { context["testadmin"] as UserAggregateAvro }

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
        .submitUser(asReference = "cr-user") {
          it.firstName = "Carlos"
          it.lastName = "Caracho"
        }
        .submitUser(asReference = "cr-user-inactive") {
          it.firstName = "Inactive"
          it.lastName = "CR"
        }
        .submitUser(asReference = "fm-user") {
          it.firstName = "Ali"
          it.lastName = "Albatros"
        }
        .submitUser(asReference = "fm-user-inactive") {
          it.firstName = "fm-user"
          it.lastName = "inactive"
        }
        .submitUser(asReference = "fm-user-no-participant") {
          it.firstName = "fm-user"
          it.lastName = "no-participant"
        }
        .submitUser(asReference = "other-project-user") {
          it.firstName = "other-project"
          it.lastName = "fm-user"
        }
        .submitCompany(asReference = "company") {
          it.streetAddress =
              StreetAddressAvro.newBuilder()
                  .setArea("1")
                  .setCity("2")
                  .setHouseNumber("3")
                  .setStreet("4")
                  .setZipCode("5")
                  .setCountry("6")
                  .build()
        }
        .setUserContext("csm-user")
  }

  @BeforeEach
  fun setupMocks() {
      every { blobStoreService.generateSignedUrlForImage(any(), any(), any(), any()) } returns
          URL("http://example.com")
  }

  @AfterEach
  fun reset() {
    clearMocks(blobStoreService)
  }
}
