# ![logo](docs/images/ygg-logo-green.png) 이그드라시 - 블록체인 플랫폼

> We will change the world by blockchain.

[![Build Status](https://travis-ci.org/yggdrash/yggdrash.svg?branch=develop)](https://travis-ci.org/yggdrash/yggdrash)
[![Coverage Status](https://coveralls.io/repos/github/yggdrash/yggdrash/badge.svg?branch=develop)](https://coveralls.io/github/yggdrash/yggdrash?branch=develop)
[![codecov](https://codecov.io/gh/yggdrash/yggdrash/branch/develop/graph/badge.svg)](https://codecov.io/gh/yggdrash/yggdrash)

## 이그드라시 프로젝트

신뢰를 기반으로 모든 블록체인을 연결(Trust-based Multidimensional Blockchains)하고 인터넷 상의 모든 서비스를 블록체인(Internet re-designed by blockchains)으로 재구성하는 것을 목표로 하고 있습니다.

## 목차

* [문서](#문서)
* [개발](#개발)
    * [필수조건](#필수조건)
    * [소스받기](#소스받기)
    * [로컬실행](#로컬실행)
    * [도커실행](#도커실행)
    * [배포용 빌드](#배포용-빌드)
    * [테스트](#테스트)
* [APIs](#apis)
* 도커를 사용해서 쉽게 개발하기 (옵션)
* CI-CD 구성하기 (옵션)
* [소식보기](#소식보기)


## 문서
[이그드라시 기술 문서](docs) 에서 보다 상세한 내용을 참고하거나 [위키](https://github.com/yggdrash/yggdrash/wiki)를 방문하세요.

## 개발
자바로 개발되어 자바 런타임이 설치되는 다양한 운영체제(리눅스, 윈도우, OSX등 )에서 실행이 가능합니다.

### 필수조건

소스코드를 빌드하기 위해서는 최소 1.8 이상의 `JDK` 를 설치해야 합니다. [다운받기](http://www.oracle.com/technetwork/java/javase/overview/index.html)

### 소스받기

yggdrash 레파지토리를 클론합니다.

```
git clone https://github.com/yggdrash/yggdrash.git
cd yggdrash
```
> 만약 Git이 설치되어 있지 않거나 친숙하지 않다면, 소스코드가 압축된 [ZIP](https://github.com/yggdrash/yggdrash/archive/master.zip) 파일을 다운로드 받을 수 있습니다.

### 로컬실행

프로젝트에 포함된 gradlew 쉘을 사용해서 쉽게 실행 가능합니다. (리눅스/OSX 기준, 윈도우즈 실행: `gradlew`)
```
./gradlew
```
여러개의 노드를 실행할 경우 이미 바인딩된 포트들을 아래와 같이 수정하여 실행합니다. (IntelliJ 기준)

![config](docs/images/intellij-run-config.png)

### 도커실행
이그드라시 노드는 도커 이미지로 제작 가능하고 보다 자세한 설명은 [도커 문서](docker)를 참고하세요.

아래는 도커로 쉽고 빠르게 단일노드를 실행하고 컨테이너 종료시 삭제되는 명령입니다.

```
docker --rm -p 8080:8080 -p 9090:9090 -h yggdrash-node1 --name yggdrash-node1 yggdrash/yggdrash-node
```

이 경우 `localhost` 주소의 `8080`(기본값) 포트로 RESTful API 와 JSON RPC를 호출 가능하고, `9090`(기본값)은 노드간 gRPC 통신용 포트로 사용됩니다.

다른 포트(예:8081)로 실행하려면 `-p 8081:8080` 옵션으로 실행합니다.

[Dockerfile](Dockerfile)은 DockerHub에서 자동으로 소스코드를 빌드하여 도커 이미지를 생성하고 [이그드라시 도커 레파지토리](https://hub.docker.com/r/yggdrash/yggdrash-node/) 에 업로드 하는 용도로 사용됩니다.

### 배포용 빌드

아래와 같이 실행하면 실행가능한 배포용 `Jar` 바이너리가 생성됩니다.
```
./gradlew -PspringProfiles=prod clean build
```

정상적으로 노드가 실행되는지 확인합니다.
```
yggdrash-node/build/libs/*.jar
```

다음과 같은 옵션을 사용해서 노드 jar 를 실행 가능합니다. (예: yggdrash-node.jar --server.port=8081)

- `--server.address=value` JSON RPC 및 RESTful API 서비스용 호스트 주소
- `--server.port=value` JSON RPC 및 RESTful API 서비스용 포트
- `--yggdrash.node.grpc.host=value` gRPC 서비스용 호스트 주소
- `--yggdrash.node.grpc.port=value` gRPC 서비스용 포트
- `--yggdrash.node.max-peers=value` 노드에서 연결할 최대 피어 수(기본값: 25)

### 테스트
단위 테스트를 실행합니다.
```
./gradlew test
```
Gradle 캐시를 사용하지 않고 통합 테스트를 사용하기 위한 명령입니다.
```
./gradlew test -PspringProfiles=ci --rerun-tasks
```


## APIs

노드가 실행되면 생성된 블록을 웹브라우져에서 다음 주소로 쉽게 조회 가능합니다. [http://localhost:8080/blocks](http://localhost:8080/blocks)

- 전체 [JSON RPC API](docs/api/jsonrpc-api.md) 목록을 확인하세요.


## 도커를 사용해서 쉽게 개발하기 (옵션)

도커를 사용하면 개발 경험을 향상시킬수 있습니다. 여러개의 노드를 확장하거나 필요한 서비스를 조합해서 쉽게 실행할 수 있습니다.

예로 2개의 노드가 설정된 yml을 아래 명령어로 실행 가능합니다.
```
docker-compose -f docker/docker-compose.yml up -d
```

실행된 노드 컨테이너를 정지하고 삭제하기 위한 명령입니다.
```
docker-compose -f docker/docker-compose.yml down
```


## CI-CD 구성하기 (옵션)

환경을 구성하기 위해서 별도의 툴이 설치되거나 설정되어야 합니다.

- Jenkins
   - 도커로 필요한 젠킨스 서버를 [docker/jenkins.yml](docker/jenkins.yml) 쉽게 실행 가능합니다.

```
docker-compose -f docker/jenkins.yml up -d
```
- Travis: [Travis 설정 문서](https://docs.travis-ci.com/user/getting-started/)

### 이그드라시 프로젝트를 Jenkins에서 생성하기 위해 필요한 설정입니다.
```
* Project name: `Yggdrash`
* Source Code Management
    * Git Repository: `git@github.com:yggdrash/yggdrash.git`
    * Branches to build: `*/master`
    * Additional Behaviours: `Wipe out repository & force clone`
* Build Triggers
    * Poll SCM / Schedule: `H/5 * * * *`
* Build
    * Invoke Gradle script / Use Gradle Wrapper / Tasks: `-PspringProfiles=prod clean build`
    * Execute Shell / Command:
        ````
        ./gradlew bootRun &
        bootPid=$!
        sleep 30s
        kill $bootPid
        ````
* Post-build Actions
    * Publish JUnit test result report / Test Report XMLs: `build/test-results/*.xml`
```
Jenkins 파이프라인을 설정하기 위한 [파일](Jenkinsfile)이 준비되어 있고 다음과 같은 기능을 수행하는 스테이지들로 구성되어 있습니다.

- 도커 이미지 빌드
- Sonar를 사용한 품질분석
- 도커 레파지토리에 이미지 업로드


## 소식보기
- 페이스북 [@yggdrash](https://www.facebook.com/yggdrash)
- 트위터 [@YggdrashNews](https://twitter.com/YggdrashNews)
- [이그드라시 블로그](http://blog.naver.com/yggdrash)
- [이그드라시 웹사이트](https://yggdrash.io/#team) #팀 멤버


## 라이센스
이그드라시는 아파치 라이센스 2.0 으로 배포되고 상세 내용은 [LICENSE](LICENSE) 에서 확인할 수 있습니다.
