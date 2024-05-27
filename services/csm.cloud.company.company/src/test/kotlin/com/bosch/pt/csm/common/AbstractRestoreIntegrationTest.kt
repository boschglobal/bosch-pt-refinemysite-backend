/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common

import com.bosch.pt.csm.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.common.facade.AbstractIntegrationTest
import com.bosch.pt.csm.common.util.getCreatedBy
import com.bosch.pt.csm.common.util.getCreatedDate
import com.bosch.pt.csm.common.util.getLastModifiedBy
import com.bosch.pt.csm.common.util.getLastModifiedDate
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.context.ActiveProfiles

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
@ActiveProfiles("test", "restore-db-test", "idp-bosch-dev")
abstract class AbstractRestoreIntegrationTest : AbstractIntegrationTest() {

  fun validateAuditingInformation(entity: AuditableSnapshot, aggregateAvro: SpecificRecordBase) {
    assertThat(entity.createdBy?.identifier)
        .isEqualTo(aggregateAvro.getCreatedBy().identifier.toUUID())
    assertThat(entity.lastModifiedBy?.identifier)
        .isEqualTo(aggregateAvro.getLastModifiedBy().identifier.toUUID())
    assertThat(entity.createdDate).isEqualTo(aggregateAvro.getCreatedDate())
    assertThat(entity.lastModifiedDate).isEqualTo(aggregateAvro.getLastModifiedDate())
  }
}
