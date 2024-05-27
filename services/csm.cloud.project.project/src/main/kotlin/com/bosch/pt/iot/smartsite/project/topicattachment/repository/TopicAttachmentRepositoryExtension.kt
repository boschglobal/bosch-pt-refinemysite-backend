/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.repository

import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment

interface TopicAttachmentRepositoryExtension {

  fun getByTopicIdsPartitioned(topicIds: List<Long>): List<TopicAttachment>

  fun deletePartitioned(ids: List<Long>)
}
