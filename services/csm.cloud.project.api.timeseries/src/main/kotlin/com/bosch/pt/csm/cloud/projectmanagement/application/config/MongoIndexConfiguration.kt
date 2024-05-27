/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.COMPANY_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PAT_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.PROJECT_CRAFT_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DAY_CARD_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MILESTONE_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.PARTICIPANT_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.PROJECT_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RELATION_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.RFV_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TASK_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TASK_CONSTRAINT_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TASK_CONSTRAINT_SELECTION_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TASK_SCHEDULE_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TOPIC_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WORK_AREA_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WORK_AREA_LIST_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WORK_DAY_CONFIGURATION_PROJECTION
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.USER_PROJECTION
import org.bson.Document
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexOperations

@Configuration
class MongoIndexConfiguration(private val mongoTemplate: MongoTemplate) {

  @EventListener(ApplicationReadyEvent::class)
  fun initIndicesAfterStartup() {
    mongoTemplate.indexOps(COMPANY_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureCompoundIndex(Document(mapOf("identifier" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(DAY_CARD_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("task" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(MILESTONE_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("identifier" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(PARTICIPANT_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "status" to 1)))
      ensureCompoundIndex(Document(mapOf("user" to 1, "status" to 1)))
    }

    mongoTemplate.indexOps(PAT_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("impersonatedUserIdentifier", ASC))
    }

    mongoTemplate.indexOps(PROJECT_CRAFT_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(PROJECT_PROJECTION).apply { ensureIndex(Index().on("identifier", ASC)) }

    mongoTemplate.indexOps(RELATION_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("target.identifier" to 1, "type" to 1, "deleted" to 1)))
      ensureCompoundIndex(
          Document(
              mapOf(
                  "source.identifier" to 1,
                  "source.type" to 1,
                  "target.type" to 1,
                  "type" to 1,
                  "deleted" to 1)))
      ensureCompoundIndex(
          Document(
              mapOf(
                  "target.identifier" to 1,
                  "target.type" to 1,
                  "source.type" to 1,
                  "type" to 1,
                  "deleted" to 1)))
    }

    mongoTemplate.indexOps(RFV_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(TASK_CONSTRAINT_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(TASK_CONSTRAINT_SELECTION_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("task" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(TASK_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("identifier" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(TASK_SCHEDULE_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureIndex(Index().on("task", ASC))
      ensureCompoundIndex(Document(mapOf("task" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(TOPIC_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("task" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(USER_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("idpIdentifier", ASC))
    }

    mongoTemplate.indexOps(WORK_AREA_LIST_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(WORK_AREA_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("identifier" to 1, "deleted" to 1)))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }

    mongoTemplate.indexOps(WORK_DAY_CONFIGURATION_PROJECTION).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureIndex(Index().on("project", ASC))
      ensureCompoundIndex(Document(mapOf("project" to 1, "deleted" to 1)))
    }
  }

  private fun IndexOperations.ensureCompoundIndex(keys: Document): String =
      ensureIndex(CompoundIndexDefinition(keys))
}
