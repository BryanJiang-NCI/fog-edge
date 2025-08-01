<!--
 * @Author: Bryan x23399937@student.ncirl.ie
 * @Date: 2025-07-21 21:37:35
 * @LastEditors: Bryan x23399937@student.ncirl.ie
 * @LastEditTime: 2025-08-01 10:22:42
 * @FilePath: /FEC-CA/README.md
 * @Description: 
 * 
 * Copyright (c) 2025 by Bryan Jiang, All Rights Reserved. 
-->
# fog-edge
A smart meter IOT system simulation source code. 

## Simulation tool
- iFogSim: for sensor and edge gateway device
- AWS: for edge and cloud deployment

## Folder Files introduction
```
├── /.github/workflows/              # github actions cicd pipeline config file
    ├── cloud_cicd.yml               # cloud pipeline code
    ├── edge_cicd.yml                # edge pipeline code
├── /cloud/                          # cloud application code
    ├── alert_stream_processor.py    # spark code
    ├── cloud_app.py                 # cloud source code
├── /edge/                           # edge application code
    ├── edge_app.py                  # edge source code
├── /iFogSim/                        # sensor simulation code
    ├── DataStructure.json           # simulation data structure
    ├── MyFogBroker.java             # make simulation data using http post to the aws environment
    ├── Smart.java                   # sensor device simulation code
├── spark.sh                         # using aws cli submit spark job shell script
```

## Dependency
- flask
- boto3
- pyspark

## running step
### sensor and gateway
put the iFogSim code like shown below in the iFogSim folder
├── /org.for.smart/      
    ├── MyFogBroker.java  
    ├── Smart.java  

### edge and cloud application
```
pip install -r requirements.txt
```

```
cd edge && python edge_app.py
```

```
cd cloud && python cloud_app.py
```


## export ecs task-define json command
aws ecs describe-task-definition \
  --task-definition edge-taskk \
  --query "taskDefinition" \
  --output json > ecs-task-def.json
