/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.Employee_
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant_
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskSpecifications.activeParticipant
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import java.util.UUID
import org.springframework.data.jpa.domain.Specification

object TopicSpecifications {

  /** Filters topics whose identifier is contained in the given collection of topic identifiers. */
  @Suppress("FunctionNaming")
  fun `in`(topicIdentifiers: Collection<TopicId>) =
      Specification { topic: Root<Topic>, _: CriteriaQuery<*>, _: CriteriaBuilder ->
        topic.get(Topic_.identifier).`in`(topicIdentifiers)
      }

  /** Filters topic whose id is equal to the given topic id. */
  fun equalsId(topicId: Long) =
      Specification { task: Root<Topic>, _: CriteriaQuery<*>, builder: CriteriaBuilder ->
        builder.equal(task.get(Topic_.id), topicId)
      }

  /** Filters topics where the specified user is a participant in the given role. */
  fun hasProjectRole(userIdentifier: UUID, role: ParticipantRoleEnum?) =
      Specification { topic: Root<Topic>, _: CriteriaQuery<*>, builder: CriteriaBuilder ->
        builder.equal(
            activeParticipant(userIdentifier, topic, builder).get(Participant_.role), role)
      }

  /**
   * Filters topics where the specified user is a participant.
   *
   * @return the join object for the path `topic.task.project.participants`.
   */
  private fun activeParticipant(
      userIdentifier: UUID,
      topic: Root<Topic>,
      builder: CriteriaBuilder
  ) = activeParticipant(userIdentifier, JoinRecycler.join(topic, Topic_.task).get(), builder)

  /** Filters topics created by the specified user. */
  fun isCreatedBy(userIdentifier: UUID?) =
      Specification { topic: Root<Topic>, _: CriteriaQuery<*>, builder: CriteriaBuilder ->
        builder.equal(topic.get(Topic_.createdBy).get<UUID>("identifier"), userIdentifier)
      }

  /** Filters topics created by an employee of the specified user's company. */
  fun isCreatedByCompanyOf(userIdentifier: UUID) =
      Specification { topic: Root<Topic>, query: CriteriaQuery<*>, builder: CriteriaBuilder ->
        builder.equal(
            activeParticipant(userIdentifier, topic, builder).get(Participant_.company),
            queryCreatorCompany(topic, query, builder))
      }

  /** Queries the company of a topic's creator as a [Subquery] */
  private fun queryCreatorCompany(
      topic: Root<Topic>,
      query: CriteriaQuery<*>,
      builder: CriteriaBuilder
  ): Subquery<Company> {
    val subQuery = query.subquery(Company::class.java)
    val employee = subQuery.from(Employee::class.java)
    return subQuery
        .select(employee.get(Employee_.company))
        .where(
            builder.equal(
                topic.get<Any>("createdBy").get<UUID>("identifier"),
                JoinRecycler.join(employee, Employee_.user).get().get<UUID>("identifier")))
  }
}
