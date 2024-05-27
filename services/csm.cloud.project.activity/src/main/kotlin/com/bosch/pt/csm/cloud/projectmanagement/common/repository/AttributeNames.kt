/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.repository

object AttributeNames {

  object Common {

    const val ID = "_id"
    const val ID_IDENTIFIER = "_id.identifier"
    const val ID_TYPE = "_id.type"
    const val ID_VERSION = "_id.version"
  }

  object Project {

    const val ID_TYPE_VALUE_TASK = "TASK"
    const val ID_TYPE_VALUE_TOPIC = "TOPIC"
    const val ID_TYPE_VALUE_RFV_CUSTOMIZATION = "RFVCUSTOMIZATION"
    const val ID_TYPE_VALUE_TASK_CONSTRAINT_CUSTOMIZATION = "TASKCONSTRAINTCUSTOMIZATION"

    const val PROJECT_IDENTIFIER = "projectIdentifier"
    const val TASK_IDENTIFIER = "taskIdentifier"
    const val TOPIC_IDENTIFIER = "topicIdentifier"
  }

  object User {

    const val DISPLAY_NAME = "displayName"
    const val USER_PICTURE_IDENTIFIER = "userPictureIdentifier"
  }
}
