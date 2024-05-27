/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_ATTACHMENTS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_WORK_AREA
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_START
import org.springframework.stereotype.Component

@Component
class TaskAttributeSorter {

  fun sortAttributes(attributes: Set<String>) =
      attributes.sortedWith(
          Comparator { a, b -> attributeSortIndex(a).compareTo(attributeSortIndex(b)) })

  private fun attributeSortIndex(attribute: String?) =
      when (attribute) {
        TASK_SCHEDULE_ATTRIBUTE_START -> 1
        TASK_SCHEDULE_ATTRIBUTE_END -> 2
        TASK_ATTRIBUTE_NAME -> 3
        TASK_ATTRIBUTE_CRAFT -> 4
        TASK_ATTRIBUTE_WORK_AREA -> 5
        TASK_ATTRIBUTE_LOCATION -> 6
        TASK_ATTRIBUTE_DESCRIPTION -> 7
        TASK_ATTRIBUTE_ATTACHMENT -> 8
        TASK_ATTRIBUTE_ATTACHMENTS -> 9
        else -> 100
      }
}
