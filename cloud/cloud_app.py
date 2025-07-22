"""
Author: Bryan x23399937@student.ncirl.ie
Date: 2025-07-22 17:40:21
LastEditors: Bryan x23399937@student.ncirl.ie
LastEditTime: 2025-07-22 19:45:36
FilePath: /FEC-CA/cloud/cloud_app.py
Description:

Copyright (c) 2025 by Bryan Jiang, All Rights Reserved.
"""

from flask import Flask, request, jsonify
import time

app = Flask(__name__)


@app.route("/", methods=["GET"])
def index():
    return "Cloud Node is running."


@app.route("/receive", methods=["POST"])
def receive():
    data = request.get_json()
    print(f"[Cloud] Received data at {time.time()}:\n{data}")
    return jsonify({"status": "received"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
