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

# init Spark Session
spark = SparkSession.builder.appName("FogEdgeAlertProcessor").getOrCreate()

# real time read data from kinesis
raw_df = (
    spark.readStream.format("aws-kinesis")
    .option("kinesis.streamName", "fog-edge")
    .option("kinesis.region", "us-east-1")
    .option("kinesis.consumerType", "GetRecords")
    .option("kinesis.endpointUrl", "https://kinesis.us-east-1.amazonaws.com")
    .option("kinesis.startingPosition", "LATEST")
    .load()
)

# define input data JSON schema
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

# mapping json data
json_df = (
    raw_df.selectExpr("CAST(data AS STRING) as json_str")
    .select(from_json(col("json_str"), schema).alias("data"))
    .select("data.*")
)

# filter error alert reading > 1000
alert_df = json_df.filter(col("reading") > 1000)

# print data
query = (
    alert_df.writeStream.format("console")
    .option("truncate", False)
    .outputMode("append")
    .option("checkpointLocation", "s3://fog-edge/checkpoints/fog-edge-alerts/")
    .start()
)

query.awaitTermination()
