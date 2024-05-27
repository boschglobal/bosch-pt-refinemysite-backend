#  *************************************************************************
#
#   Copyright:       Robert Bosch Power Tools GmbH, 2020
#
#   *************************************************************************

import json

taskFile = open("import-task.json", "r")
taskJson = json.load(taskFile)

topicFile = open("import-topic.json", "r")
topicJson = json.load(topicFile)

messageFile = open("import-message.json", "r")
messageJson = json.load(messageFile)


def findMessage(name):
    for message in messageJson:
        if message['id'] == name:
            return message


def findTopic(name):
    for topic in topicJson:
        if topic['id'] == name:
            return topic


def findTask(name):
    for task in taskJson:
        if task['id'] == name:
            return task
#

with open('import-taskattachment.json') as json_file:
    inputJson = json.load(json_file)
    outputArray = []
    for entry in inputJson:
        if 'taskId' in entry and 'createWithUserId' not in entry:
            entry['createWithUserId'] = findTask(entry['taskId'])['createWithUserId']
        if 'topicId' in entry and 'createWithUserId' not in entry:
            entry['createWithUserId'] = findTopic(entry['topicId'])['createWithUserId']
        if 'messageId' in entry and 'createWithUserId' not in entry:
            entry['createWithUserId'] = findMessage(entry['messageId'])['createWithUserId']
        outputArray.append(entry)
    json = json.dumps(outputArray, indent=4)
    print(json)

taskFile.close()
topicFile.close()
messageFile.close()

outputFile = open("import-taskattachment-new.json", "a")
outputFile.write(json)
outputFile.close()

