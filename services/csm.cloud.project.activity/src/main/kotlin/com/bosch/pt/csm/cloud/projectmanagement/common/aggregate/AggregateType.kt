/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.aggregate

enum class AggregateType(val type: String) {
  COMPANY("Company"),
  CRAFT("Craft"),
  DAYCARD("DayCard"),
  EMPLOYEE("Employee"),
  MESSAGE("Message"),
  MESSAGEATTACHMENT("MessageAttachment"),
  PARTICIPANT("ProjectParticipant"),
  PROJECT("Project"),
  PROJECTCRAFT("ProjectCraft"),
  PROJECTPICTURE("ProjectPicture"),
  RFVCUSTOMIZATION("RfvCustomization"),
  TASK("Task"),
  TASKACTION("TaskAction"),
  TASKATTACHMENT("TaskAttachment"),
  TASKCONSTRAINTCUSTOMIZATION("TaskConstraintCustomization"),
  TASKSCHEDULE("TaskSchedule"),
  TOPIC("Topic"),
  TOPICATTACHMENT("TopicAttachment"),
  USER("User"),
  USERPICTURE("UserPicture"),
  WORKAREA("WorkArea"),
  WORKAREALIST("WorkareaList")
}
