/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.NewsResource
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.util.UUID.randomUUID

fun NewsController.getDetails(
    employee: () -> EmployeeAggregateAvro,
    task: AggregateIdentifierAvro
): List<NewsResource> = getDetails(employee(), task)

fun NewsController.getDetails(
    employee: EmployeeAggregateAvro,
    task: AggregateIdentifierAvro
): List<NewsResource> = findAllNewsForUserAndTask(employee.toUser(), task.identifier.toUUID()).items

fun NewsController.getList(
    employee: () -> EmployeeAggregateAvro,
    vararg tasks: AggregateIdentifierAvro
): List<NewsResource> =
    findNewsForUserAndListOfTasks(employee().toUser(), tasks.map { it.identifier.toUUID() }).items

fun NewsController.getList(
    employee: () -> EmployeeAggregateAvro,
    tasks: List<ObjectIdentifier>
): List<NewsResource> =
    findNewsForUserAndListOfTasks(employee().toUser(), tasks.map { it.identifier }).items

fun NewsController.delete(task: AggregateIdentifierAvro, employee: () -> EmployeeAggregateAvro) =
    delete(task, employee())

fun NewsController.delete(task: AggregateIdentifierAvro, vararg employees: EmployeeAggregateAvro) =
    employees.forEach { deleteAllNewsForUserAndTask(it.toUser(), task.identifier.toUUID()) }

fun NewsController.deleteByProject(
    project: AggregateIdentifierAvro,
    employee: () -> EmployeeAggregateAvro
) = deleteByProject(project, employee())

fun NewsController.deleteByProject(
    project: AggregateIdentifierAvro,
    vararg employees: EmployeeAggregateAvro
) = employees.forEach { deleteAllNewsForUserAndProject(it.toUser(), project.identifier.toUUID()) }

private fun EmployeeAggregateAvro.toUser() =
    User(identifier = user.identifier.toUUID(), userId = randomUUID().toString())
