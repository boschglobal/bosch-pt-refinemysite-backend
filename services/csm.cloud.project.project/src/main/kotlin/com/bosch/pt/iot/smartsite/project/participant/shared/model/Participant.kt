/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.user.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull

@Entity
@Table(
    name = "project_participant",
    indexes = [Index(name = "UK_ProjPart_Identifier", columnList = "identifier", unique = true)],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "UK_ProjPart_Assignment",
                columnNames = ["project_id", "company_id", "user_id"])])
class Participant : AbstractSnapshotEntity<Long, ParticipantId> {

  @field:NotNull
  @JoinColumn(foreignKey = ForeignKey(name = "FK_ProjPart_Project"), nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_ProjPart_Company"))
  @ManyToOne(fetch = FetchType.LAZY)
  var company: Company? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_ProjPart_User"))
  @ManyToOne(fetch = FetchType.LAZY)
  var user: User? = null
    set(value) {
      email = value?.email
      field = value
    }

  @field:NotNull @Column(nullable = false) var role: ParticipantRoleEnum? = null

  @field:NotNull @Column(nullable = false) var status: ParticipantStatusEnum? = ACTIVE

  /**
   * This is a redundant field either sourced from the email attribute of the user entity or from
   * the email attribute of the invitation (in case of invited participants). It was introduced to
   * allow sorting by email over all participants (even those still being invited). This field is
   * nullable to have no dependencies between the invitation topic and project topic in restore mode
   * since there is not overall order given.
   */
  var email: String? = null

  /** For JPA. */
  constructor() {
    // Empty
  }

  constructor(project: Project, company: Company? = null, user: User, role: ParticipantRoleEnum) {
    this.project = project
    this.company = company
    this.role = role
    this.user = user
  }

  constructor(project: Project, role: ParticipantRoleEnum, email: String) {
    this.project = project
    this.role = role
    this.email = email
  }

  override fun getDisplayName(): String? = user?.getDisplayName()

  fun isActive(): Boolean = status === ACTIVE

  fun isUpdatePossible() = !isInactive() && !isOwnParticipant()

  fun isResendPossible() = this.status == INVITED

  private fun isInactive() = this.status == INACTIVE

  private fun isOwnParticipant() =
      this.user != null && this.user!!.identifier == AuthorizationUtils.getCurrentUser().identifier

  companion object {
    private const val serialVersionUID: Long = -5297833077160269636

    const val STATUS = "status"
    const val USER_LAST_NAME = "user.lastName"
    const val USER_FIRST_NAME = "user.firstName"
  }
}
