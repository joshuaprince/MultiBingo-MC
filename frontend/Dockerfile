FROM node:10

WORKDIR /frontend

COPY ./yarn.lock /frontend
COPY ./package.json /frontend
RUN yarn install

COPY . /frontend
RUN yarn build

# Nothing to run.
