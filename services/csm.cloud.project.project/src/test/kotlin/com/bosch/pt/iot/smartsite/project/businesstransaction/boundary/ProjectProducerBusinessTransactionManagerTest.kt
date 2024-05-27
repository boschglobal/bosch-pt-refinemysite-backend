/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.businesstransaction.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import java.time.LocalDateTime
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
internal class ProjectProducerBusinessTransactionManagerTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectProducerBusinessTransactionManager

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()

    setAuthentication(getIdentifier("csm-user"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `batch operation business transaction contains started and finished events`() {
    transactionTemplate.execute {
      cut.doBatchInBusinessTransaction(projectIdentifier) {
        // do nothing
      }
    }

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            BatchOperationStartedEventAvro::class.java,
            BatchOperationFinishedEventAvro::class.java))

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationStartedEventAvro::class.java, null, false)
        .also {
          assertThat(it.getAuditingInformation().getDate()).isCloseToNow()
          assertThat(it.getAuditingInformation().getUser()).isIdentifierOf("csm-user")
        }

    projectEventStoreUtils
        .verifyContainsAndGet(BatchOperationFinishedEventAvro::class.java, null, false)
        .also {
          assertThat(it.getAuditingInformation().getDate()).isCloseToNow()
          assertThat(it.getAuditingInformation().getUser()).isIdentifierOf("csm-user")
        }
  }

  private fun AbstractLongAssert<*>.isCloseToNow() =
      this.isCloseTo(LocalDateTime.now().toEpochMilli(), offset(10_000))

  private fun AbstractStringAssert<*>.isIdentifierOf(reference: String) =
      this.isEqualTo(getIdentifier(reference).toString())
}
