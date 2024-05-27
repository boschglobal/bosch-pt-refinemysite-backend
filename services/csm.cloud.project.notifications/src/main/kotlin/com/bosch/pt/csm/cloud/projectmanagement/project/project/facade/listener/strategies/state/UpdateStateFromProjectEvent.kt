/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.project.boundary.ProjectService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.ProjectAddress
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.ProjectCategoryEnum
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromProjectEvent(private val projectService: ProjectService) :
    AbstractStateStrategy<ProjectEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is ProjectEventAvro &&
          (record.value as ProjectEventAvro).name != ProjectEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: ProjectEventAvro): Unit =
      event.aggregate.run {
        projectService.save(
            Project(
                identifier = buildAggregateIdentifier(),
                projectIdentifier = getIdentifier(),
                title = title,
                description = description,
                start = start.toLocalDateByMillis(),
                end = end.toLocalDateByMillis(),
                client = client,
                projectNumber = projectNumber,
                category = category?.let { ProjectCategoryEnum.valueOf(category.toString()) },
                projectAddress =
                    ProjectAddress(
                        city = projectAddress.city,
                        houseNumber = projectAddress.houseNumber,
                        street = projectAddress.street,
                        zipCode = projectAddress.zipCode,
                    )))
      }
}
