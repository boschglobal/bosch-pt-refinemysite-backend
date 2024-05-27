/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.domain.Auditable
import org.springframework.test.context.ActiveProfiles

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
@ActiveProfiles("test", "restore-db-test", "idp-bosch-dev")
abstract class AbstractRestoreIntegrationTest : AbstractIntegrationTest() {

  fun <U : Auditable<UserId, *, LocalDateTime>> validateAuditingInformation(
      entity: U,
      aggregateAvro: SpecificRecordBase
  ) {
    assertThat(entity.createdBy.get().identifier)
        .isEqualTo(aggregateAvro.getCreatedBy().getIdentifier().toUUID())
    assertThat(entity.lastModifiedBy.get().identifier)
        .isEqualTo(aggregateAvro.getLastModifiedBy().getIdentifier().toUUID())
    assertThat(entity.createdDate.get()).isEqualTo(aggregateAvro.getCreatedDate())
    assertThat(entity.lastModifiedDate.get()).isEqualTo(aggregateAvro.getLastModifiedDate())
  }

  fun getUserIdentifierAvro(identifier: UUID): AggregateIdentifierAvro =
      EventStreamGenerator.Companion.newAggregateIdentifier(USER.value, identifier).build()

  private fun SpecificRecordBase.getCreatedBy(): AggregateIdentifierAvro =
      (this.get("auditingInformation") as SpecificRecordBase).get("createdBy")
          as AggregateIdentifierAvro

  private fun SpecificRecordBase.getLastModifiedBy(): AggregateIdentifierAvro =
      (this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedBy")
          as AggregateIdentifierAvro

  private fun SpecificRecordBase.getCreatedDate(): LocalDateTime =
      ((this.get("auditingInformation") as SpecificRecordBase).get("createdDate") as Long)
          .toLocalDateTimeByMillis()

  private fun SpecificRecordBase.getLastModifiedDate(): LocalDateTime =
      ((this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedDate") as Long)
          .toLocalDateTimeByMillis()
}
