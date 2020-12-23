#!/bin/bash

# Collect static files
echo "Collect static files"
python ./web/manage.py collectstatic --noinput

# Apply database migrations
echo "Apply database migrations"
python ./web/manage.py migrate

# Start server
echo "Starting server"
python ./web/manage.py runserver 0.0.0.0:8000 --insecure
