# AWS Secret Manager Migrator

A small ETL program that migrates AWS secrets under a prefix to another prefix. The secret values
from source prefix are combined into a single line terminated value (up to 200 per secret) and then created under target
prefix.

For example, if under the secrets manager, the secrets were created as (where each secret has only one value) :
~~~
source-prefix/<UUID1>
source-prefix/<UUID2>
...
source-prefix/<UUID200>
source-prefix/<UUID201>
...
source-prefix/<UUID400>
~~~

The 400 secrets will be transformed under two values (200 `\n` separated) under `target-prefix`, for example:
~~~
target-prefix/<UUID>
target-prefix/<UUID>
~~~

## Build Instructions
### Docker build 
Docker build does not run tests.
~~~
docker build -t usmans/aws-sm-migrator:latest .
~~~
Docker build is published via GitHub actions and image can be used without building the project as well.
~~~
docker pull usmans/aws-sm-migrator:latest
~~~

### Gradle build
Requires Java 17 and docker compose plugin available for running integration tests. The distribution will be created in 
`./migrator/build/install/migrator/`
~~~
./gradlew build installdist
~~~

## Usage
This program uses AWS [Default Credential Provider](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain).
Following sample uses Environment variables credentials: 
~~~
export AWS_ACCESS_KEY_ID=ASIA...
export AWS_SECRET_ACCESS_KEY=Xddss...
export AWS_SESSION_TOKEN=IOP33...
export AWS_REGION=us-east-2
~~~
Usage via docker:
~~~
docker run --rm -it -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN -e AWS_REGION usmans/aws-sm-migrator:latest --help
~~~
Usage without docker:
~~~
./migrator/build/install/migrator/bin/aws-sm-migrator --help
~~~

~~~
Usage: aws-sm-migrator [-dhV] [--delete-source-secrets] [-e=<URI>]
                       [-n=<NUMBER>] -s=source-prefix/ -t=target-prefix/
Migrates AWS Secret values to line-terminated multi-values.
  -d, --dry-run   Dry run only.
      --delete-source-secrets
                  Delete secrets under source prefix after migration.
  -e, --aws-endpoint-override-uri=<URI>
                  Override AWS endpoint. Useful for integration testing with
                    localstack.
  -h, --help      Show this help message and exit.
  -n, --number-of-keys=<NUMBER>
                  Number of keys to store in single secret. Maximum size is
                    200. Defaults to 200.
  -s, --source-prefix=source-prefix/
                  Source Secret Name Prefix which contains the secrets.
  -t, --target-prefix=target-prefix/
                  Target Secret Name Prefix where to create the secrets.
  -V, --version   Print version information and exit.
~~~

## Docker Compose
See `examples` directory for docker-compose example using localstack.
