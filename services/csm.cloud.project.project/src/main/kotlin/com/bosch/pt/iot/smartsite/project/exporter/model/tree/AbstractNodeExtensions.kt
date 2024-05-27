/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

fun MutableList<AbstractNode>.sortNodes() = this.sortedWith(
compareBy(
{
  val exId = checkNotNull(it.externalId)
  if (exId.fileId == null) {
    Int.MAX_VALUE
  } else {
    exId.fileId
  }
},
{ it.startDate },
{
  when (it) {
    is MilestoneNode -> 0
    is TaskNode -> 1
    else -> 2
  }
},
{
  when (it) {
    is MilestoneNode -> it.milestone.name
    is TaskNode -> it.rmsTask.name
    else -> "zz" // put at the end
  }
}))