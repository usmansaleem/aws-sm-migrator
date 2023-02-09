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
