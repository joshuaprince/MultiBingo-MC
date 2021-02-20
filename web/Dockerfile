FROM python:3
ENV PYTHONUNBUFFERED=1

WORKDIR /code
COPY ./requirements*.txt /code/
RUN pip install -r requirements_prod.txt

COPY . /code/web/
RUN chmod +x /code/web/docker-entrypoint.sh

WORKDIR /code/web

EXPOSE 8000
VOLUME /static/
ENTRYPOINT ["./docker-entrypoint.sh"]
