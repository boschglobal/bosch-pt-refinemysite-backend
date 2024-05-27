/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.model

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddressBuilder.Companion.projectAddress
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class ProjectBuilder private constructor() {

  private var identifier: ProjectId = ProjectId()
  private var createdDate = now()
  private var lastModifiedDate = now()
  private var createdBy: UserId = UserId()
  private var lastModifiedBy: UserId = UserId()
  private var client: String? = null
  private var description: String? = null
  private var start = LocalDate.now()
  private var end = start.plusDays(5)
  private var projectNumber: String = ""
  private var title: String = ""
  private var category: ProjectCategoryEnum? = null
  private var projectAddress: ProjectAddress? = null
  private val participants: MutableSet<Participant> = HashSet()

  fun withIdentifier(identifier: ProjectId): ProjectBuilder {
    this.identifier = identifier
    return this
  }

  fun withCreatedDate(createdDate: LocalDateTime): ProjectBuilder {
    this.createdDate = createdDate
    return this
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): ProjectBuilder {
    this.lastModifiedDate = lastModifiedDate
    return this
  }

  fun withCreatedBy(createdBy: UserId): ProjectBuilder {
    this.createdBy = createdBy
    return this
  }

  fun withLastModifiedBy(lastModifiedBy: UserId): ProjectBuilder {
    this.lastModifiedBy = lastModifiedBy
    return this
  }

  fun withClient(client: String?): ProjectBuilder {
    this.client = client
    return this
  }

  fun withDescription(description: String?): ProjectBuilder {
    this.description = description
    return this
  }

  fun withEnd(end: LocalDate): ProjectBuilder {
    this.end = end
    return this
  }

  fun withStart(start: LocalDate): ProjectBuilder {
    this.start = start
    return this
  }

  fun withProjectNumber(projectNumber: String): ProjectBuilder {
    this.projectNumber = projectNumber
    return this
  }

  fun withTitle(title: String): ProjectBuilder {
    this.title = title
    return this
  }

  fun withCategory(category: ProjectCategoryEnum?): ProjectBuilder {
    this.category = category
    return this
  }

  fun withProjectAddress(projectAddress: ProjectAddress?): ProjectBuilder {
    this.projectAddress = projectAddress
    return this
  }

  fun withParticipant(participant: Participant): ProjectBuilder {
    participants.add(participant)
    return this
  }

  fun build(): Project {
    val project = Project()
    project.identifier = identifier
    if (createdDate != null) {
      project.setCreatedDate(createdDate)
    }
    if (lastModifiedDate != null) {
      project.setLastModifiedDate(lastModifiedDate)
    }
    project.setCreatedBy(createdBy)
    project.setLastModifiedBy(lastModifiedBy)
    project.client = client
    project.description = description
    project.end = end
    project.start = start
    project.projectNumber = projectNumber
    project.title = title
    project.category = category
    project.projectAddress = projectAddress
    project.participants = participants
    return project
  }

  companion object {

    @JvmStatic
    fun project(): ProjectBuilder =
        ProjectBuilder()
            .withTitle("Project Title")
            .withProjectNumber("Project Number")
            .withProjectAddress(projectAddress().build())
  }
}
