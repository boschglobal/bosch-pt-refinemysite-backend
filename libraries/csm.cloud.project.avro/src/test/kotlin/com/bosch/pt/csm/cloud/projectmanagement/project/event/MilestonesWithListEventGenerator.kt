/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import java.time.LocalDate

fun EventStreamGenerator.submitMilestonesWithList(
    listReference: String = "milestoneList",
    date: LocalDate,
    header: Boolean = false,
    project: String? = null,
    workArea: String? = null,
    milestones: List<SubmitMilestoneWithListDto>
): EventStreamGenerator {
  val milestoneReferences = mutableListOf<AggregateIdentifierAvro>()
  milestones.withIndex().forEach { (index, milestone) ->
    val milestoneReference = listReference + "M" + index
    submitMilestone(
        asReference = milestoneReference,
        rootContextIdentifier = project?.let { getIdentifier(it) } ?: lastProjectIdentifier()) {
      it.date = date.toEpochMilli()
      project?.apply { it.project = getByReference(project) }
      it.header = header
      workArea?.apply { it.workarea = getByReference(workArea) }
      it.name = milestone.name ?: milestoneReference
      it.type = milestone.type
      milestone.craft?.apply { it.craft = milestone.craft }
    }
    milestoneReferences.add(getByReference(milestoneReference))
    submitMilestoneList(
        asReference = listReference,
        rootContextIdentifier = project?.let { getIdentifier(it) } ?: lastProjectIdentifier(),
        eventType =
            if (index == 0) MilestoneListEventEnumAvro.CREATED
            else MilestoneListEventEnumAvro.ITEMADDED) {
      it.date = date.toEpochMilli()
      project?.apply { it.project = getByReference(project) }
      it.header = header
      workArea?.apply { it.workarea = getByReference(workArea) }
      it.milestones = milestoneReferences
    }
  }
  return this
}

private fun EventStreamGenerator.lastProjectIdentifier() =
    getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID()

class SubmitMilestoneWithListDto(
    val type: MilestoneTypeEnumAvro,
    val name: String? = null,
    val craft: AggregateIdentifierAvro? = null
)
