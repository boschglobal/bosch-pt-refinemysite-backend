/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.repository

import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID

interface ExternalIdRepository :
    KafkaStreamableRepository<ExternalId, Long, ExternalIdEventEnumAvro> {

  fun findAllByProjectIdAndIdType(
      projectId: ProjectId,
      type: ExternalIdType,
  ): List<ExternalId>

  fun findAllByProjectId(projectId: ProjectId): List<ExternalId>

  fun findAllByObjectIdentifier(identifier: UUID): List<ExternalId>
}
