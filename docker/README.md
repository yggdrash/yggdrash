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
> docker run --rm -p 8080:8080 -v $HOME/.yggdrash:/.yggdrash yggdrash/yggdrash-node
```

now shows all the blocks:

```shell
> curl localhost:8080/blocks
```

#### TODO

 - Docker-Compose configuration
 - Kubernetes configuration
