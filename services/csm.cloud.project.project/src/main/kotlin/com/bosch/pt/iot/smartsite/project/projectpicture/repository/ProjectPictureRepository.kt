/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectpicture.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectPictureRepository :
    KafkaStreamableRepository<ProjectPicture, Long, ProjectPictureEventEnumAvro> {

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findOneByIdentifier(identifier: UUID): ProjectPicture?

  @Query("select p.project.identifier from ProjectPicture p where p.identifier = :identifier")
  fun findProjectIdentifierByPictureIdentifier(@Param("identifier") identifier: UUID): ProjectId?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findOneByProjectIdentifier(identifier: ProjectId): ProjectPicture?

  @EntityGraph(
      attributePaths =
          [
              "project",
              "createdBy",
              "createdBy.profilePicture",
              "lastModifiedBy",
              "lastModifiedBy.profilePicture"])
  fun findAllByProjectIdentifierIn(identifiers: Set<ProjectId>): Set<ProjectPicture>
}
