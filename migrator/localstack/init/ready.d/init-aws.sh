#! /bin/bash

set -o errexit

END="${NUM_OF_SECRETS:?Set NUM_OF_SECRETS env variable}"
PREFIX="${SECRET_PREFIX:?Set prefix via env variable}"
for ((i=1;i<=END;i++)); do
   SECRET_VALUE=`openssl rand -hex 16`
   SECRET_NAME=`od -x /dev/urandom | head -1 | awk '{OFS="-"; print $2$3,$4,$5,$6,$7$8$9}'`
   echo "Creating $PREFIX$SECRET_NAME"
   awslocal secretsmanager create-secret --name "$PREFIX$SECRET_NAME" \
     --description "Random Test Secret $i" \
     --secret-string "$SECRET_VALUE"
done
