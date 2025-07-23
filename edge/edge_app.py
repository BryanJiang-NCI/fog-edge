"""
Author: Bryan x23399937@student.ncirl.ie
Date: 2025-07-22 17:39:46
LastEditors: Bryan x23399937@student.ncirl.ie
LastEditTime: 2025-07-22 17:39:48
FilePath: /FEC-CA/edge/edge_app.py
Description:

Copyright (c) 2025 by Bryan Jiang, All Rights Reserved.
"""

from flask import Flask, request, jsonify
import boto3
import json
import time

app = Flask(__name__)

# Kinesis 配置
REGION = "us-east-1"  # 根据实际调整
STREAM_NAME = "fog-edge"  # 替换为你创建的 Stream 名称
kinesis = boto3.client("kinesis", region_name=REGION)


@app.route("/", methods=["GET"])
def index():
    return "Edge Node is running."


@app.route("/upload", methods=["POST"])
def upload():
    try:
        raw = request.get_json()
        print(f"[Edge] Received raw data: {raw}")

        # ======= 轻量处理逻辑 =======
        processed = {
            "device_id": raw.get("device_id", "unknown"),
            "reading": raw.get("reading"),
            "unit": raw.get("unit", "L"),
            "battery": raw.get("battery", -1),
            "status": raw.get("status", "unknown"),
            "timestamp": raw.get("timestamp", time.time()),
            "anomaly": is_anomaly(raw.get("reading")),
            "location": "Apt-305",
            "ingest_time": time.time(),
        }

        # ======= 上传到 Kinesis =======
        response = kinesis.put_record(
            StreamName=STREAM_NAME,
            Data=json.dumps(processed),
            PartitionKey=processed["device_id"],
        )

        print(f"[Edge] Uploaded to Kinesis: {processed}")
        return jsonify({"status": "ok", "kinesis_sequence": response["SequenceNumber"]})

    except Exception as e:
        print(f"[Edge] Error: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route("/metrics/upload", methods=["POST"])
def metrics_upload():
    try:
        raw = request.get_json()
        print(f"[Edge] Received raw data: {raw}")

        # ======= 轻量处理逻辑 =======
        processed = {
            "device_id": raw.get("device_id", "unknown"),
            "reading": raw.get("reading"),
            "unit": raw.get("unit", "L"),
            "battery": raw.get("battery", -1),
            "status": raw.get("status", "unknown"),
            "timestamp": raw.get("timestamp", time.time()),
            "anomaly": is_anomaly(raw.get("reading")),
            "location": "Apt-305",
            "ingest_time": time.time(),
        }
        # 2. 同步上传到云端服务
        try:
            resp = request.post(, json=processed, timeout=5)
            print(f"[Edge][Metrics] Synced to cloud: {resp.status_code} {resp.text}")
        except Exception as ex:
            print(f"[Edge][Metrics] Sync to cloud failed: {ex}")

        return jsonify({"status": "ok"})

    except Exception as e:
        print(f"[Edge][Metrics] Error: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


def is_anomaly(reading):
    try:
        reading = float(reading)
        return reading < 0 or reading > 1000  # 示例阈值
    except:
        return True


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
