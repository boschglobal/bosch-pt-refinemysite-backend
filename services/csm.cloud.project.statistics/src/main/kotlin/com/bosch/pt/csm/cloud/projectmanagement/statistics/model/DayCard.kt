/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import java.time.LocalDate
import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    indexes =
        [
            Index(
                name = "IX_DayCard_ContIdentContTyp",
                columnList = "context_identifier,context_type"),
            Index(name = "IX_DayCard_TaskId", columnList = "task_identifier")],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "UK_DayCard_PrjIdConIdentContTyp",
                columnNames = ["project_identifier", "context_identifier", "context_type"])])
class DayCard(
    // type / identifier
    contextObject: ObjectIdentifier,

    // project reference
    projectIdentifier: UUID,

    // optional status
    @Column(nullable = true) var status: DayCardStatusEnum?,

    // optional reason
    @Column(nullable = true) var reason: DayCardReasonNotDoneEnum?,

    // task reference
    @Column(name = "task_identifier") var taskIdentifier: UUID
) : DayCardIdentifier(projectIdentifier, contextObject) {

  // optional date
  @Column(nullable = true) var date: LocalDate? = null

  // optional participant reference
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  var assignedParticipant: ParticipantMapping? = null

  // optional craft reference
  @Column(nullable = true) var craftIdentifier: UUID? = null
}
