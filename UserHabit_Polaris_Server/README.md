# Polaris project
Polaris is Open API, HTML Web Server, based on the Rx, Netty, for non-blocking web application server

## Getting Started

### Prerequisites

- [JDK 11 +](https://jdk.java.net/)
- [MongoDB 4.4.X +](https://www.mongodb.com/download-center/community/releases)
- [Git (latest)](https://rogerdudler.github.io/git-guide/index.ko.html)

### Installing

```shell
cd <your project root path>
git clone https://bitbucket.org/andbut/polaris.git
```
### Editing mongodb configuration file

```text
Add path 'mongo home directory' to 'mongodb.home' key into the following file
<your project root path>/polaris/src/main/resources/config.dev.properties

Usage:
mongodb.home=/home/userhabit/app/mongodb-linux-x86_64-ubuntu2004-4.4.4

```

### S3 를 사용하는 경우 auth.keys

s3 를 사용하는 경우 `auth.keys`라는 이름의 파일을 `<server-root>` directory 에 넣어놔야 한다.

```aidl
<server-root>
    |
    +--- auth.keys
    +--- bin/
    +--- console/
    +--- lib/
```

auth.keys 는 아래와 같은 내용을 가지고 있다.

```
$ cat auth.keys
s3.access_key=DFSGECN2OEDX3C5KE
s3.secret_key=fjdkslgjdfzTx+rxt8yGYkqiG9/J1QW1gCqmQF
```

### Running the server in dev mode

```shell
cd <your project root path>/polaris
./gradlew dev
# ./gradlew stage
# ./gradlew prod

# or for windows
./gradlew.bat dev

# with debug
./gradlew run --args="env=dev log.level=debug" # dev
#./gradlew run --args="env=stage log.level=debug"
#./gradlew run --args="env=prod log.level=debug"
```

### Running the benchmark test with the sample data
```shell
cd <your project root path>/polaris

# only one
./gradlew benchmark
# or for windows
./gradlew.bat benchmark

# multiple
./gradlew benchmark -Plimit=10000
```

### Running the client test by sample data
```shell
cd <your project root path>/polaris/src/main/resources/test

# session & event post,put(CrUd)
./polaris_sdk_post.sh |jq 

# session get(cRud)
TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InRlc3QwMUB1c2VyaGFiaXQuaW8iLCJpYXQiOjE2MDU2OTE5NTcsImV4cCI6MTYwODI4Mzk1NywiYWlsIjpbImU2YzEwMWYwMjBlMTAxOGI1YmExN2NkYmUzMmFkZTJkNjc5YjQ0YmMiXX0.57HzpLuMlzxhohW7NOS1gfVt-Uvjvy_AClWSYfBr5AA"
FROM_DATE=$(date -d "-1 months" -u "+%Y-%m-%dT%H:%M:%SZ")
TO_DATE=$(date -d "0 months" -u "+%Y-%m-%dT%H:%M:%SZ")

# usage :curl -H "Authorization: Bearer <json token>" "localhost:8000/v3/session/<session id>?param=value" |jq
curl -H "Authorization: Bearer ${TOKEN}" "localhost:8000/v3/session/1_cf9f99b08d6744d9bd3fac192538c3c7?is_crash=true&from_date=${FROM_DATE}&to_date=${TO_DATE}&limit=2&skip=2" |jq

# event get(cRud)
# usage :curl -H "Authorization: Bearer <json token>" "localhost:8000/v3/event/<event id>?param=value" |jq
curl -H "Authorization: Bearer ${TOKEN}" "localhost:8000/v3/event?session_id_list=0_cf9f99b08d6744d9bd3fac192538c3c7&from_date=${FROM_DATE}&to_date=${TO_DATE}&limit=5&skip=2" | jq
```

### Deployment for production
```shell
./gradlew build
# usage : 
# unzip ./build/distributions/polaris*.zip -d <target path>
unzip ./build/distributions/polaris*.zip -d ~/polaris/
cd ~/polaris/polaris*
./bin/polaris
# or for windows
./bin/polaris.bat

# with env, log.level
./bin/polaris env=dev log.level=debug
#./bin/polaris env=stage log.level=debug
#./bin/polaris env=prod log.level=debug
```

