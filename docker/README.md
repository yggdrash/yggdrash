# Docker

> Note: Docker install on macOS
> https://store.docker.com/editions/community/docker-ce-desktop-mac

To ensure everything worked, run:

```shell
> yggdrash-node/build/libs/*.jar
```

first build a docker image of the yggdrash node by running:

```shell
> ./gradlew build docker
```

then run the node:

```shell
> docker run --rm -e SPRING_PROFILES_ACTIVE=local,master,gateway -p 8080:8080  yggdrash/yggdrash-node
```

now shows all the blocks:

```shell
> curl localhost:8080/yggdrash/blocks
```

# Docker Compose

## Usage

Launch all node by running: `docker-compose up --scale yggdrash-node=3 -d`.

## Configured Docker services

### Bootstrap node:
- active peers -> http://localhost:8080/peers/active

### Master node:
- block -> http://localhost:8081/yggdrash/blocks

### General node:
- best block -> http://localhost:8082/yggdrash/blocks/latest

## deploy to test server
- dev server: `docker-compose -f docker-compose-deploy.yml up -d`
- prod server: create `.env` file and run `docker-compose -f docker-compose-deploy.yml up -d`
```shell
PROFILE=prod
GRPC_HOST=52.79.188.79
```

# Elasticsearch Docker Compose

## Usage

Launch es by running: `docker-compose -f docker-compose-es.yml up`.

## Integrate node with es
```shell
> docker run --rm -e es.host=host:9200 -e es.prefix.index=INDEX_NAME -p 8080:8080  yggdrash/yggdrash-node
```

#### TODO
 - Kubernetes configuration
