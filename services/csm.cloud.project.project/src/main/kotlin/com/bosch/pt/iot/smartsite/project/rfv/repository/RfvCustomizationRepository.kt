/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.repository

import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.model.RfvCustomization
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph

interface RfvCustomizationRepository :
    KafkaStreamableRepository<RfvCustomization, Long, RfvCustomizationEventEnumAvro> {

  fun findOneByKeyAndProjectIdentifier(
      key: DayCardReasonEnum,
      projectIdentifier: ProjectId
  ): RfvCustomization?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): RfvCustomization?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<RfvCustomization>

  fun findOneByIdentifier(identifier: UUID): RfvCustomization?
}
