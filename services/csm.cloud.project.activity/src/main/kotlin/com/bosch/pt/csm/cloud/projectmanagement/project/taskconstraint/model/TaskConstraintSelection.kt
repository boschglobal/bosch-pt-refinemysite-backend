/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

/**
 * The TaskConstraintSelection is the successor of TaskAction.
 *
 * It was renamed but since we have existing data in the database the alias and action field remain
 * as they are.
 */
@Document(PROJECT_STATE)
@TypeAlias("TaskAction")
data class TaskConstraintSelection(
    @Id val aggregateIdentifier: AggregateIdentifier,
    override val projectIdentifier: UUID,
    val taskIdentifier: UUID,
    val actions: MutableList<TaskConstraintEnum>
) : ShardedByProjectIdentifier
