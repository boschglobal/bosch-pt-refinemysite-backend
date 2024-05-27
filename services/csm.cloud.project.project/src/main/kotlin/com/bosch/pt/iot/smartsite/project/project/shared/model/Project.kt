/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Table(indexes = [Index(name = "UK_Project_Identifier", columnList = "identifier", unique = true)])
class Project : AbstractSnapshotEntity<Long, ProjectId>() {

  @field:Size(max = MAX_PROJECT_CLIENT_LENGTH)
  @Column(length = MAX_PROJECT_CLIENT_LENGTH)
  var client: String? = null

  @field:Size(max = MAX_PROJECT_DESCRIPTION_LENGTH)
  @Column(length = MAX_PROJECT_DESCRIPTION_LENGTH)
  var description: String? = null

  @Column(nullable = false, name = "project_start") lateinit var start: LocalDate

  @Column(nullable = false, name = "project_end") lateinit var end: LocalDate

  @field:Size(min = 1, max = MAX_PROJECT_NUMBER_LENGTH)
  @Column(nullable = false, length = MAX_PROJECT_NUMBER_LENGTH)
  lateinit var projectNumber: String

  @field:Size(min = 1, max = MAX_PROJECT_TITLE_LENGTH)
  @Column(nullable = false, length = MAX_PROJECT_TITLE_LENGTH)
  lateinit var title: String

  @Column(columnDefinition = "varchar(255)")
  @Enumerated(STRING)
  var category: ProjectCategoryEnum? = null

  @Embedded var projectAddress: ProjectAddress? = null

  @OneToMany(mappedBy = "project", fetch = LAZY, targetEntity = Participant::class)
  var participants: Set<Participant>? = null

  @Column(columnDefinition = "bit not null default 0", insertable = false) val deleted = false

  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("client", client)
          .append("description", description)
          .append("start", start)
          .append("end", end)
          .append("projectNumber", projectNumber)
          .append("description", description)
          .append("title", title)
          .append("category", category)
          .append("projectAddress", projectAddress)
          .toString()

  override fun getDisplayName(): String = title

  override fun getIdentifierUuid(): UUID = identifier.toUuid()

  companion object {
    private const val serialVersionUID: Long = -747433308267582199

    const val MAX_PROJECT_CLIENT_LENGTH = 100
    const val MAX_PROJECT_DESCRIPTION_LENGTH = 1000
    const val MAX_PROJECT_NUMBER_LENGTH = 100
    const val MAX_PROJECT_TITLE_LENGTH = 100
  }
}
