# Docker

> Note: Docker install on macOS
> https://store.docker.com/editions/community/docker-ce-desktop-mac

To ensure everything worked, run:

```shell
> yggdrash-node/build/libs/*.jar
```

first build a docker image of the yggdrash node by running:

```shell
> ./gradlew docker
```

then run the node:

```shell
> docker run --rm -p 8080:8080 -p 32918:32918 -h yggdrash-node1 --name yggdrash-node1 yggdrash/yggdrash-node
```

now shows all the blocks:

```shell
> curl localhost:8080/blocks
```

#### TODO

 - Docker-Compose configuration
 - Kubernetes configuration
