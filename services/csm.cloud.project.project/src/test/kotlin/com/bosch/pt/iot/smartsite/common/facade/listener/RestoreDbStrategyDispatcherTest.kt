/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.listener

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy.UserContextRestoreDbStrategy
import io.mockk.mockkClass
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

@LibraryCandidate("common.streamable")
@SmartSiteSpringBootTest
class RestoreDbStrategyDispatcherTest {

  @Autowired private lateinit var transactionTemplate: TransactionTemplate

  private lateinit var cut: RestoreDbStrategyDispatcher<UserContextRestoreDbStrategy>

  @BeforeEach
  fun init() {
    cut = RestoreDbStrategyDispatcher(transactionTemplate, emptyList())
  }

  @Test
  fun dispatcherFailsWhenTransactionIsPropagated() {
    val key =
        AggregateEventMessageKey(
            AggregateIdentifier(PROJECT.value, UUID.randomUUID(), 0), UUID.randomUUID())
    val record = mockkClass(SpecificRecordBase::class)

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            cut.dispatch(ConsumerRecord("", 0, 0, key, record))
          }
        }
        .withMessage("No running transaction expected")
  }
}
