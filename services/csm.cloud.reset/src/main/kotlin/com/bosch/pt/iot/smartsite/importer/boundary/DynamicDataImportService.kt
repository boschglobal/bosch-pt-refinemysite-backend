/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.importer.boundary

import com.bosch.pt.iot.smartsite.dataimport.project.model.Milestone
import com.bosch.pt.iot.smartsite.dataimport.project.model.WorkArea
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCard
import com.bosch.pt.iot.smartsite.dataimport.task.model.Task
import com.bosch.pt.iot.smartsite.dataimport.task.model.Topic
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.time.LocalDate
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.RequestScope

@Service
@RequestScope
class DynamicDataImportService : AbstractDataImportService() {

  private lateinit var generatedData: GeneratedData

  fun importData(dataset: String, rootDate: LocalDate, numberOfAdditionalWorkAreas: Int) {
    generatedData = DynamicTestDataExtender.generateData(dataset, numberOfAdditionalWorkAreas)
    super.importData(dataset, rootDate)
  }

  override fun getWorkAreas(resources: Map<ResourceTypeEnum, Resource>): List<WorkArea> =
      generatedData.workAreas

  override fun getTasks(resources: Map<ResourceTypeEnum, Resource>): List<Task> =
      generatedData.tasks

  override fun getTopics(resources: Map<ResourceTypeEnum, Resource>): List<Topic> =
      generatedData.topics

  override fun getDayCards(resources: Map<ResourceTypeEnum, Resource>): List<DayCard> =
      generatedData.dayCards

  override fun getMilestones(resources: Map<ResourceTypeEnum, Resource>): List<Milestone> =
      generatedData.milestones
}
