#!/bin/bash

# Collect static files
# Really not necessary without a frontend
#echo "Collect static files"
#python ./manage.py collectstatic --noinput

# Apply database migrations
echo "Apply database migrations"
python ./manage.py migrate

# Start server
echo "Starting server"
daphne -b 0.0.0.0 -p 8000 MCBingo.asgi:application
