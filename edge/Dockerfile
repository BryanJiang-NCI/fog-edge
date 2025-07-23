FROM python:3.10-slim

WORKDIR /app

COPY edge_app.py .

RUN pip install flask boto3 requests

EXPOSE 80

CMD ["python", "edge_app.py"]