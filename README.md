<!--
 * @Author: Bryan x23399937@student.ncirl.ie
 * @Date: 2025-07-21 21:37:35
 * @LastEditors: Bryan x23399937@student.ncirl.ie
 * @LastEditTime: 2025-07-24 21:38:26
 * @FilePath: /FEC-CA/README.md
 * @Description: 
 * 
 * Copyright (c) 2025 by Bryan Jiang, All Rights Reserved. 
-->
# fog-edge

test for demo

## export ecs task-define json command
aws ecs describe-task-definition \
  --task-definition edge-taskk \
  --query "taskDefinition" \
  --output json > ecs-task-def.json
