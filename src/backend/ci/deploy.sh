#!/bin/bash
set -e  # Exit immediately if a command exits with a non-zero status

NEW_TAG=$(cat new_image_tag.txt)
echo "Deploying image: $ECR_URL:$NEW_TAG"

# Get the current task definition
aws ecs describe-task-definition --task-definition $ECS_TASK_DEFINITION --region $AWS_REGION \
  --query "taskDefinition" --output json > task-definition.json

# Modify the task definition JSON
jq --arg IMAGE "$ECR_URL:$NEW_TAG" \
   --argjson ENV_VARS "$ENV_VARS" \
   --argjson SECRETS "$SECRETS" \
   '. |
    del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy) |
    .containerDefinitions[0].image=$IMAGE |
    .containerDefinitions[0].environment=$ENV_VARS |
    .containerDefinitions[0].secrets=$SECRETS' task-definition.json > final-task-definition.json


# Register the new task definition
NEW_TASK_ARN=$(aws ecs register-task-definition --cli-input-json file://final-task-definition.json --region $AWS_REGION \
  --query 'taskDefinition.taskDefinitionArn' --output text)

echo "New Task Definition ARN: $NEW_TASK_ARN"

# Update ECS service
aws ecs update-service --cluster $ECS_CLUSTER --service $ECS_SERVICE \
  --task-definition $NEW_TASK_ARN --region $AWS_REGION

# Force new deployment
aws ecs update-service --cluster $ECS_CLUSTER --service $ECS_SERVICE \
  --force-new-deployment --region $AWS_REGION
