# Docker Compose
The docker compose example shows how to use localstack to simulate aws-sm-migrator tool.

## Usage:
~~~
docker compose up
~~~
Will initialize localstack secretsmanager service with 15 random secrets. The aws-sm-migrator will transform 15 secrets 
5 values per secret under `migrated-prefix`

The results can be manually tested via `aws-cli` docker image connecting to localstack.
~~~
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=us-east-2
export AWS_SESSION_TOKEN=test

docker run --rm -it \
-e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_REGION -e AWS_SESSION_TOKEN \
amazon/aws-cli --endpoint-url=http://host.docker.internal:4566 secretsmanager list-secrets --query 'SecretList[].Name'

docker run --rm -it \
-e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_REGION -e AWS_SESSION_TOKEN \
amazon/aws-cli --endpoint-url=http://host.docker.internal:4566 secretsmanager get-secret-value --secret-id 'migrated-prefix/EmKImQ8Yjh1iTb3VbDXM1n9KxOU'
~~~