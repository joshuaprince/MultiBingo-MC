# Dockerfile that generates the nginx image to host both the frontend and backend.

# Stage 1: Build frontend's static files.
FROM node:10 AS front

WORKDIR /frontend

COPY ./frontend/yarn.lock /frontend
COPY ./frontend/package.json /frontend
RUN yarn install

COPY ./frontend /frontend
RUN yarn build


# Stage 2: Build the nginx image.
FROM nginx:1
COPY ./nginx.conf.template /etc/nginx/templates/nginx.conf.template
COPY --from=front /frontend/build /frontend
