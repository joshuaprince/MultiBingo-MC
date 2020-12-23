FROM python:3
ENV PYTHONUNBUFFERED=1
ENV DJANGO_SETTINGS_MODULE=MCBingo.settings_prod

WORKDIR /code
COPY ./web/requirements.txt /code
RUN pip install -r requirements.txt

COPY ./generation /code/generation
COPY ./web /code/web
RUN chmod +x /code/web/docker-entrypoint.sh

EXPOSE 8000
ENTRYPOINT ["/code/web/docker-entrypoint.sh"]
CMD []
