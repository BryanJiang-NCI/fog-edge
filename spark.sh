#!/bin/bash
###
 # @Author: Bryan x23399937@student.ncirl.ie
 # @Date: 2025-07-02 19:15:52
 # @LastEditors: Bryan x23399937@student.ncirl.ie
 # @LastEditTime: 2025-07-24 19:07:07
 # @FilePath: /FEC-CA/spark.sh
 # @Description: 
 # 
 # Copyright (c) 2025 by Bryan Jiang, All Rights Reserved. 
### 
# submit_job.sh

# 加载环境变量或参数（可从 .env 或命令行传入）
APP_ID="00fu9jvq8s58u209"        # 替换为你的 Application ID
JOB_ROLE="arn:aws:iam::762603643140:role/service-role/AmazonEMRStudio_RuntimeRole_1751301346337"
S3_BUCKET="fog-edge"
REGION="us-east-1"

# 额外参数：传递到 spark-submit
SPARK_PARAMS="--conf spark.executor.memory=1g --conf spark.executor.cores=1"

# --- 解析必传参数 ---
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    --script)
      SCRIPT_PATH="$2"
      shift; shift
      ;;
    --job-mode)
      JOB_MODE="$2"
      shift; shift
      ;;
    --parameters)
      PARAMETERS="$2"
      shift; shift
      ;;
    *)
      echo "❌ Unknown option: $1"
      echo "Usage: ./submit_job.sh --script <s3://path/to/script.py> --job-mode <job-mode> --parameters <parameters>"
      exit 1
      ;;
  esac
done

# --- 校验 ---
if [[ -z "$SCRIPT_PATH" || -z "$JOB_MODE" ]]; then
  echo "❌ Missing required arguments."
  echo "Usage: ./submit_job.sh --script <s3://path/to/script.py> --job-mode <job-mode> --parameters <parameters>"
  exit 1
fi

# 提交作业
aws emr-serverless start-job-run \
  --application-id $APP_ID \
  --mode $JOB_MODE \
  --execution-role-arn $JOB_ROLE \
  --job-driver "{
      \"sparkSubmit\": {
        \"entryPoint\": \"s3://$S3_BUCKET/alert_stream_processor.py\",
        \"sparkSubmitParameters\": \"--executor-memory 1g --executor-cores 1 --driver-memory 1g --driver-cores 1 $SPARK_PARAMS $PARAMETERS\"
      }
    }" \
  --name "$SCRIPT_PATH" \
  --region $REGION
