FROM python:3.10-slim

WORKDIR /app

COPY cloud_app.py .

RUN pip install flask boto3

EXPOSE 80

CMD ["python", "cloud_app.py"]