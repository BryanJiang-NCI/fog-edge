"""
Author: Bryan x23399937@student.ncirl.ie
Date: 2025-07-22 17:40:21
LastEditors: Bryan x23399937@student.ncirl.ie
LastEditTime: 2025-07-22 19:45:36
FilePath: /FEC-CA/cloud/cloud_app.py
Description:
test
Copyright (c) 2025 by Bryan Jiang, All Rights Reserved.
"""

from flask import Flask, request, jsonify
import time
import boto3
import json

app = Flask(__name__)

# Kinesis config
REGION = "us-east-1"
STREAM_NAME = "fog-edge"
kinesis = boto3.client("kinesis", region_name=REGION)


@app.route("/", methods=["GET"])
def index():
    return "Cloud Node is running."


@app.route("/alert", methods=["POST"])
def receive():
    data = request.get_json()
    print(f"[Cloud] Alert event at {time.time()}:\n{data}")
    return jsonify({"status": "received"}), 200


@app.route("/upload", methods=["POST"])
def upload():
    try:
        raw = request.get_json()
        print(f"[Edge] Received raw data: {raw}")

        # ======= lightweight preprocessing =======
        processed = process_structure(raw)

        # ======= upload data to Kinesis =======
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


def process_structure(raw) -> dict:
    processed = {
        "device_type": raw.get("device_type", "unknown"),
        "device_id": raw.get("device_id", "unknown"),
        "reading": raw.get("reading"),
        "unit": raw.get("unit", "L"),
        "battery": raw.get("battery", -1),
        "status": raw.get("status", "unknown"),
        "timestamp": raw.get("timestamp", time.time()),
        "anomaly": is_anomaly(raw.get("reading")),
        "location": "Dublin",
        "ingest_time": time.time(),
    }
    return processed


# checking is anomaly
def is_anomaly(reading):
    try:
        reading = float(reading)
        return reading < 0 or reading > 1000
    except:
        return True


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
