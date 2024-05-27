/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql.resource.response.TopicPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql.resource.response.assembler.TopicPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.service.TopicQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class TopicGraphQlController(
    private val topicPayloadAssembler: TopicPayloadAssembler,
    private val topicQueryService: TopicQueryService
) {

  @BatchMapping
  fun topics(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, List<TopicPayloadV1>?> {
    val topics =
        topicQueryService.findAllByTasksAndDeletedFalse(tasks.map { it.id.asTaskId() }).groupBy {
          it.task
        }

    return tasks.associateWith { task ->
      topics[task.id.asTaskId()]?.map { topicPayloadAssembler.assemble(it) }
    }
  }
}
