/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectpicture.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTPICTURE
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.projectpicture.repository.ProjectPictureRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreProjectPictureStrategy(
    private val projectRepository: ProjectRepository,
    private val projectPictureRepository: ProjectPictureRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, projectPictureRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      PROJECTPICTURE.value == record.key().aggregateIdentifier.type &&
          record.value() is ProjectPictureEventAvro?

  public override fun doHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ) {
    val key = record.key()
    val event = record.value() as ProjectPictureEventAvro?
    assertEventNotNull(event, key)

    if (event!!.getName() == DELETED) {
      deletePicture(key.aggregateIdentifier.identifier)
    } else if (event.getName() == CREATED || event.getName() == UPDATED) {
      val aggregate = event.getAggregate()
      val picture = findPicture(aggregate.getAggregateIdentifier())

      if (picture == null) {
        createPicture(aggregate)
      } else {
        updatePicture(picture, aggregate)
      }
    } else {
      handleInvalidEventType(event.getName().name)
    }
  }

  private fun createPicture(aggregate: ProjectPictureAggregateAvro) =
      entityManager.persist(
          ProjectPicture().apply {
            setPictureAttributes(this, aggregate)
            setAuditAttributes(this, aggregate.auditingInformation)
          })

  private fun updatePicture(picture: ProjectPicture, aggregate: ProjectPictureAggregateAvro) =
      update(
          picture,
          object : DetachedEntityUpdateCallback<ProjectPicture> {
            override fun update(entity: ProjectPicture) {
              setPictureAttributes(entity, aggregate)
              setAuditAttributes(entity, aggregate.auditingInformation)
            }
          })

  private fun deletePicture(identifier: UUID) =
      delete(projectPictureRepository.findOneByIdentifier(identifier))

  private fun setPictureAttributes(
      picture: ProjectPicture,
      aggregate: ProjectPictureAggregateAvro
  ) =
      picture
          .apply {
            identifier = aggregate.getAggregateIdentifier().getIdentifier().toUUID()
            version = aggregate.getAggregateIdentifier().getVersion()
            fileSize = aggregate.getFileSize()
            setFullAvailable(aggregate.getFullAvailable())
            height = aggregate.getHeight()
            project = findProject(aggregate.getProject())
            setSmallAvailable(aggregate.getSmallAvailable())
            width = aggregate.getWidth()
          }
          .returnUnit()

  private fun findPicture(aggregateIdentifierAvro: AggregateIdentifierAvro): ProjectPicture? =
      projectPictureRepository.findOneByIdentifier(
          UUID.fromString(aggregateIdentifierAvro.identifier))

  private fun findProject(aggregateIdentifierAvro: AggregateIdentifierAvro): Project? =
      projectRepository.findOneByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())
}
