#!/usr/bin/env sh
set -x

echo "NUM_SECRETS=$NUM_SECRETS"
echo "SECRET_PREFIX=$SECRET_PREFIX"

mkdir -p keys

for i in $(seq $NUM_SECRETS);
do
  echo "Creating UUID & private key $i ...";
  UUID=$(cat /proc/sys/kernel/random/uuid)
  openssl genrsa -out keys/private$i.key 512
  awslocal secretsmanager create-secret \
    --name "$SECRET_PREFIX$UUID" \
    --description "Random test secret $i" \
    --secret-string file://keys/private$i.key
done

awslocal secretsmanager list-secrets

set +x