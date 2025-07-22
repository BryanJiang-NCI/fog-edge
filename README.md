# fog-edge


## export ecs task-define json command
aws ecs describe-task-definition \
  --task-definition edge-taskk \
  --query "taskDefinition" \
  --output json > ecs-task-def.json
