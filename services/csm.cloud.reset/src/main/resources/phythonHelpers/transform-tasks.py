#  *************************************************************************
#
#   Copyright:       Robert Bosch Power Tools GmbH, 2020
#
#   *************************************************************************

import json

with open('import-task.json') as json_file:
    input = json.load(json_file)
    tasks = []
    for p in input:
        if p['projectId'] == "project1" or p['projectId'] == "project3":
            p['createWithUserId'] = "daniel"
        if p['projectId'] == "project2" or p['projectId'] == "project4":
            p['createWithUserId'] = "anne"
        tasks.append(p)
    json = json.dumps(tasks, indent=4)
    print(json)

f = open("import-tasks-new.json", "a")
f.write(json)
f.close()