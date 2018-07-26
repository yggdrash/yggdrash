# Docker

> Note: To ensure everything worked, run:
```shell
> java -jar yggdrash-node/build/libs/*.jar
```

first build a docker image of the yggdrash node by running:

```shell
> ./gradlew docker
```

then run the node:

```shell
> docker run --rm -p 8080:8080 -p 9090:9090 --name yggdrash-node yggdrash/yggdrash-node
```

now shows all the blocks:

```shell
> curl localhost:8080/blocks
```

#### TODO

 - Docker-Compose configuration
 - Kubernetes configuration
