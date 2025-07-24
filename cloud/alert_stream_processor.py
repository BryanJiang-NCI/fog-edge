"""
Author: Bryan x23399937@student.ncirl.ie
Date: 2025-07-24 16:43:05
LastEditors: Bryan x23399937@student.ncirl.ie
LastEditTime: 2025-07-24 16:43:12
FilePath: /FEC-CA/cloud/alert_stream_processor.py
Description:

Copyright (c) 2025 by Bryan Jiang, All Rights Reserved.
"""

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, from_json
from pyspark.sql.types import (
    StructType,
    LongType,
    StringType,
    DoubleType,
    IntegerType,
    BooleanType,
)

# 初始化 Spark Session（用于 EMR Serverless）
spark = SparkSession.builder.appName("FogEdgeAlertProcessor").getOrCreate()

# 从 Kinesis 实时读取数据
raw_df = (
    spark.readStream.format("aws-kinesis")
    .option("kinesis.streamName", "fog-edge")
    .option("kinesis.region", "us-east-1")
    .option("kinesis.consumerType", "GetRecords")
    .option("kinesis.endpointUrl", "https://kinesis.us-east-1.amazonaws.com")
    .option("kinesis.startingPosition", "LATEST")
    .load()
)

# 定义输入数据的 JSON schema
schema = (
    StructType()
    .add("timestamp", LongType())
    .add("device_type", StringType())
    .add("device_id", StringType())
    .add("reading", DoubleType())
    .add("unit", StringType())
    .add("battery", IntegerType())
    .add("location", StringType())
    .add("status", StringType())
    .add("anomaly", BooleanType())
)

# 解码 JSON 数据
json_df = (
    raw_df.selectExpr("CAST(data AS STRING) as json_str")
    .select(from_json(col("json_str"), schema).alias("data"))
    .select("data.*")
)

# 过滤告警条件：reading > 1000
alert_df = json_df.filter(col("reading") > 1000)

# 输出到控制台（可换成 S3、Redshift、OpenSearch）
query = (
    alert_df.writeStream.format("console")
    .option("truncate", False)
    .outputMode("append")
    .option("checkpointLocation", "s3://fog-edge/checkpoints/fog-edge-alerts/")
    .start()
)

# 等待终止
query.awaitTermination()
