# AWS Secret Manager Migrator

A small ETL program that migrates AWS secrets under a prefix to another prefix. The secret values
from source prefix are combined into a single line terminated value (upto 200 per secret) and then created under target
prefix.

For example, if under the secrets manager, the secrets were created as :
~~~
source-prefix/<key1>
source-prefix/<key2>
...
source-prefix/<key200>
source-prefix/<key201>
...
source-prefix/<key400>
~~~

The 400 values will be transformed under two values under `target-prefix`, for example:
~~~
target-prefix/24R92MgE2DMfBwei1tHfINnVPYk
target-prefix/LIvkjZy0RuFRfwQyuCVMrV7o9Oc
~~~

## Build
~~~
./gradlew clean installdist
~~~

## Execute
This program uses AWS [Default Credential Provider](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain).
Following sample uses Environment variables credentials: 
~~~
export AWS_ACCESS_KEY_ID=ASIA...
export AWS_SECRET_ACCESS_KEY=Xddss...
export AWS_SESSION_TOKEN=IOP33...
export AWS_REGION=us-east-2

cd ./migrator/build/install/migrator/bin/
~~~

Dry run (does not create target secrets)
~~~
./migrator -s "signers-aws-integration/" -t "target-prefix/" -d
~~~

Run
~~~
./migrator -s "signers-aws-integration/" -t "target-prefix/"
~~~

## CLI options

These are the CLI option available:
~~~
➜  ./migrator/build/install/migrator/bin/migrator
Missing required options: '--source-prefix=source-prefix/', '--target-prefix=target-prefix/'
Usage: AWSSMMigrator [-dhV] [-e=http://localhost:4566] [-p=200]
                     -s=source-prefix/ -t=target-prefix/
Migrates AWS Secret values to line-terminated multi-values.
  -d, --dry-run              Dry run only.
  -e, --endpoint-url=http://localhost:4566
                             AWS endpoint url override.
  -h, --help                 Show this help message and exit.
  -p, --partition-size=200   Size of partitions for migration. Maximum is 200.
  -s, --source-prefix=source-prefix/
                             Source Secret Name Prefix which contains the
                               secrets.
  -t, --target-prefix=target-prefix/
                             Target Secret Name Prefix where to create the
                               secrets.
  -V, --version              Print version information and exit.
~~~

## Testing
Local test support is implemented with [localstack](https://docs.localstack.cloud) tool and docker-compose.
Following command would start local secrets manager loaded with `NUM_SECRETS` secrets.
~~~
docker-compose up -d secrets-manager
~~~
Then running following command, would run dockerize aws migrator against local secrets manager.
~~~
docker-compose run aws-migrator
~~~
Finally manual verification is performed with following commands
~~~
export AWS_ACCESS_KEY_ID=ASIA...
export AWS_SECRET_ACCESS_KEY=Xddss...
export AWS_SESSION_TOKEN=IOP33...
export AWS_REGION=us-east-2

aws --endpoint-url=http://localhost:4566 secretsmanager list-secrets --query 'SecretList[].Name'

aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value --secret-id test/750d9658-8b8a-4124-8947-32895e998b29
~~~