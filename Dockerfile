FROM python:3
ENV PYTHONUNBUFFERED=1
ENV DJANGO_SETTINGS_MODULE=MCBingo.settings_prod
WORKDIR /code
COPY requirements.txt /code/
RUN pip install -r requirements.txt
COPY . /code/
RUN chmod +x ./docker-entrypoint.sh

EXPOSE 8000
ENTRYPOINT ["./docker-entrypoint.sh"]
CMD []
