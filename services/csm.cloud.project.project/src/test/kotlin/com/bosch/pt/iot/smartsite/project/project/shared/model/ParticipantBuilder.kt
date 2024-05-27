/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.model

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeBuilder.Companion.employee
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import java.util.UUID.randomUUID

class ParticipantBuilder private constructor() {

  private var createdDate = now()
  private var lastModifiedDate = now()
  private var project: Project? = null
  private var role: ParticipantRoleEnum? = null
  private var company: Company? = null
  private var user: User? = null
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var identifier: UUID? = null
  private var status: ParticipantStatusEnum? = null
  private var email: String? = null

  fun withProject(project: Project?): ParticipantBuilder {
    this.project = project
    return this
  }

  fun withRole(role: ParticipantRoleEnum?): ParticipantBuilder {
    this.role = role
    return this
  }

  fun withEmployee(employee: Employee): ParticipantBuilder {
    company = employee.company
    user = employee.user
    return this
  }

  fun withCompany(company: Company?): ParticipantBuilder {
    this.company = company
    return this
  }

  fun withUser(user: User?): ParticipantBuilder {
    this.user = user
    return this
  }

  fun withLastModifiedBy(lastModifiedBy: User?): ParticipantBuilder {
    this.lastModifiedBy = lastModifiedBy
    return this
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): ParticipantBuilder {
    this.lastModifiedDate = lastModifiedDate
    return this
  }

  fun withCreatedBy(createdBy: User?): ParticipantBuilder {
    this.createdBy = createdBy
    return this
  }

  fun withCreatedDate(createdDate: LocalDateTime): ParticipantBuilder {
    this.createdDate = createdDate
    return this
  }

  fun withIdentifier(identifier: UUID?): ParticipantBuilder {
    this.identifier = identifier
    return this
  }

  fun withStatus(status: ParticipantStatusEnum?): ParticipantBuilder {
    this.status = status
    return this
  }

  fun withEmail(email: String?): ParticipantBuilder {
    this.email = email
    return this
  }

  fun build(): Participant {
    val participant = Participant()
    participant.identifier = identifier!!.asParticipantId()
    participant.project = project
    participant.role = role
    participant.company = company
    participant.user = user
    participant.setCreatedDate(createdDate)
    participant.setLastModifiedDate(lastModifiedDate)
    participant.setCreatedBy(createdBy?.identifier?.asUserId() ?: UserId())
    participant.setLastModifiedBy(lastModifiedBy?.identifier?.asUserId() ?: UserId())
    participant.status = status
    if (email != null) {
      // otherwise the email is set as part of the the user setter method
      participant.email = email
    }
    return participant
  }

  companion object {

    @JvmStatic
    fun participant(): ParticipantBuilder =
        with(employee().build()) {
          ParticipantBuilder()
              .withIdentifier(randomUUID())
              .withRole(FM)
              .withCompany(this.company)
              .withUser(this.user)
              .withStatus(ACTIVE)
        }

    @JvmStatic
    fun invitedParticipant(): ParticipantBuilder =
        ParticipantBuilder()
            .withIdentifier(randomUUID())
            .withRole(FM)
            .withCompany(null)
            .withUser(null)
            .withStatus(INVITED)

    @JvmStatic
    fun participantInValidation(): ParticipantBuilder =
        ParticipantBuilder()
            .withIdentifier(randomUUID())
            .withRole(FM)
            .withCompany(null)
            .withUser(user().build())
            .withStatus(VALIDATION)
  }
}
