/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.message.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.Employee_
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message_
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant_
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskSpecifications
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import java.util.UUID
import org.springframework.data.jpa.domain.Specification

object MessageSpecifications {

  /**
   * Filters messages whose identifier is contained in the given collection of message identifiers.
   */
  @Suppress("FunctionNaming")
  fun `in`(messageIdentifiers: Collection<MessageId>) =
      Specification { message: Root<Message?>, _: CriteriaQuery<*>?, _: CriteriaBuilder? ->
        message.get(Message_.identifier).`in`(messageIdentifiers)
      }

  /** Filters messages where the specified user is a participant in the given role. */
  fun hasProjectRole(userIdentifier: UUID, role: ParticipantRoleEnum) =
      Specification { message: Root<Message>, _: CriteriaQuery<*>?, builder: CriteriaBuilder ->
        builder.equal(activeParticipant(userIdentifier, message, builder)[Participant_.role], role)
      }

  /**
   * Filters messages where the specified user is a participant.
   *
   * @return the join object for the path `message.topic.task.project.participants`.
   */
  private fun activeParticipant(
      userIdentifier: UUID,
      message: Root<Message>,
      builder: CriteriaBuilder
  ) =
      TaskSpecifications.activeParticipant(
          userIdentifier,
          JoinRecycler.join(message, Message_.topic).join(Topic_.task).get(),
          builder)

  /** Filters messages created by the specified user. */
  fun isCreatedBy(userIdentifier: UUID) =
      Specification { message: Root<Message?>, _: CriteriaQuery<*>?, builder: CriteriaBuilder ->
        builder.equal(message.get(Message_.createdBy).get<UUID>("identifier"), userIdentifier)
      }

  /** Filters messages created by an employee of the specified user's company. */
  fun isCreatedByCompanyOf(userIdentifier: UUID) =
      Specification { message: Root<Message>, query: CriteriaQuery<*>, builder: CriteriaBuilder ->
        builder.equal(
            activeParticipant(userIdentifier, message, builder).get(Participant_.company),
            queryCreatorCompany(message, query, builder))
      }

  /** Queries the company of a message's creator as a [Subquery] */
  private fun queryCreatorCompany(
      message: Root<Message>,
      query: CriteriaQuery<*>,
      builder: CriteriaBuilder
  ): Subquery<Company> {
    val subQuery = query.subquery(Company::class.java)
    val employee = subQuery.from(Employee::class.java)
    return subQuery
        .select(employee.get(Employee_.company))
        .where(
            builder.equal(
                message.get<Any>("createdBy").get<UUID>("identifier"),
                JoinRecycler.join(employee, Employee_.user).get().get<UUID>("identifier")))
  }
}
