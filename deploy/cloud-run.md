# Deploying to Cloud Run

The three services on Google Cloud Run: the gateway public, the two services private, the passwords in Secret Manager. Run the commands from the repository root in Git Bash (or another POSIX shell). Nothing here is a real value: the project id is the only setting written down, every credential comes from an environment variable and is never printed.

```
client ──https──▶ api-gateway (public; runs as gateway-run; X-Api-Key, rate limit, correlation id)
                     │ Authorization: Bearer <ID token for that service>   (IdentityTokens)
                     ├──▶ ticketing-service    (private; runs as ticketing-run)    ──▶ Atlas, CloudAMQP
                     └──▶ notification-service (private; runs as notification-run; always on: it consumes)
```

> **Git Bash on Windows rewrites arguments that look like paths.** `httpGet.path=/actuator/health/readiness` would reach Cloud Run as `C:/Program Files/Git/actuator/...` and the startup probe would fail. The deploy commands below start with `MSYS2_ARG_CONV_EXCL="httpGet.path="` for that reason.

## 0. The variables

The six course variables (`MONGODB_HOST`, `MONGODB_USERNAME`, `MONGODB_PASSWORD`, `CLOUDAMQP_HOST`, `CLOUDAMQP_USERNAME`, `CLOUDAMQP_PASSWORD`) and `GATEWAY_API_KEY`, the key SkillForge's server will send, must be in the shell. On Windows they are user variables; a shell started before they were set does not see them, so open a new one.

```bash
export PROJECT_ID=frit-ticketing-2026
export REGION=europe-west1
export REPO=$REGION-docker.pkg.dev/$PROJECT_ID/ticketing
gcloud config set project $PROJECT_ID
gcloud config set run/region $REGION
for v in MONGODB_HOST MONGODB_USERNAME MONGODB_PASSWORD CLOUDAMQP_HOST CLOUDAMQP_USERNAME CLOUDAMQP_PASSWORD GATEWAY_API_KEY; do
  [ -n "${!v}" ] && echo "$v set" || echo "$v MISSING"
done
```

## 1. Once per project: APIs, image repository, budget

```bash
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com \
    iam.googleapis.com logging.googleapis.com iamcredentials.googleapis.com sts.googleapis.com
gcloud artifacts repositories create ticketing --repository-format=docker --location=$REGION
gcloud auth configure-docker $REGION-docker.pkg.dev
```

A budget alert before anything runs: console › Billing › Budgets & alerts › Create: all services, 5 USD, email at 50 %, 90 % and 100 %. Atlas › Network Access must allow `0.0.0.0/0`: Cloud Run has no fixed address.

## 2. The secrets: only what lets someone in

Hosts and usernames are settings (section 5). Each value goes from the environment through a pipe, never onto the command line.

```bash
printf '%s' "$MONGODB_PASSWORD"   | gcloud secrets create mongodb-password   --data-file=- --replication-policy=automatic
printf '%s' "$CLOUDAMQP_PASSWORD" | gcloud secrets create cloudamqp-password --data-file=- --replication-policy=automatic
printf '%s' "$GATEWAY_API_KEY"    | gcloud secrets create gateway-api-key    --data-file=- --replication-policy=automatic
gcloud secrets list
```

A changed password is a new version: `printf '%s' "$MONGODB_PASSWORD" | gcloud secrets versions add mongodb-password --data-file=-`, then redeploy (the services read `:latest` when they start).

## 3. One identity per service, with only what it needs

```bash
gcloud iam service-accounts create ticketing-run    --display-name="ticketing-service on Cloud Run"
gcloud iam service-accounts create notification-run --display-name="notification-service on Cloud Run"
gcloud iam service-accounts create gateway-run      --display-name="api-gateway on Cloud Run"

for secret in mongodb-password cloudamqp-password; do
  for sa in ticketing-run notification-run; do
    gcloud secrets add-iam-policy-binding $secret --role=roles/secretmanager.secretAccessor \
        --member="serviceAccount:$sa@$PROJECT_ID.iam.gserviceaccount.com"
  done
done
gcloud secrets add-iam-policy-binding gateway-api-key --role=roles/secretmanager.secretAccessor \
    --member="serviceAccount:gateway-run@$PROJECT_ID.iam.gserviceaccount.com"
```

## 4. The images

The one [Dockerfile](../Dockerfile), once per service. `--platform linux/amd64` is what Cloud Run runs (and what a Windows PC builds anyway).

```bash
TAG=$(git rev-parse --short HEAD)
for pair in ticketing-service:8085 notification-service:8086 api-gateway:8080; do
  service=${pair%%:*}
  docker build --platform linux/amd64 --build-arg SERVICE=$service --build-arg PORT=${pair#*:} -t $REPO/$service:$TAG .
  docker push $REPO/$service:$TAG
done
gcloud artifacts docker images list $REPO
```

## 5. The two services, private

`--no-allow-unauthenticated`: without a Google ID token they answer 403. Both run the `cloud` profile ([application-cloud.yml](../ticketing-service/src/main/resources/application-cloud.yml)); a startup probe asks Actuator's readiness before traffic arrives.

`notification-service` takes messages from RabbitMQ and gets no requests. Cloud Run gives an instance CPU only during a request and scales to zero, so it keeps one instance with CPU always on (`--no-cpu-throttling --min-instances 1`). It is the one service that costs money while nobody uses it: see section 8.

The learning events are seeded on Atlas by `DataSeeder` the first time ticketing starts on an empty database.

```bash
CLOUD_ENV=SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME,CLOUDAMQP_HOST=$CLOUDAMQP_HOST,CLOUDAMQP_USERNAME=$CLOUDAMQP_USERNAME
PASSWORDS=MONGODB_PASSWORD=mongodb-password:latest,CLOUDAMQP_PASSWORD=cloudamqp-password:latest

MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy notification-service \
    --image $REPO/notification-service:$TAG \
    --service-account notification-run@$PROJECT_ID.iam.gserviceaccount.com \
    --no-allow-unauthenticated --port 8086 --cpu 1 --memory 512Mi \
    --no-cpu-throttling --min-instances 1 --max-instances 1 \
    --set-env-vars $CLOUD_ENV --set-secrets $PASSWORDS \
    --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8086,periodSeconds=5,timeoutSeconds=3,failureThreshold=24

MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy ticketing-service \
    --image $REPO/ticketing-service:$TAG \
    --service-account ticketing-run@$PROJECT_ID.iam.gserviceaccount.com \
    --no-allow-unauthenticated --port 8085 --cpu 1 --memory 512Mi \
    --min-instances 0 --max-instances 2 \
    --set-env-vars $CLOUD_ENV --set-secrets $PASSWORDS \
    --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8085,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

Check: 403 without a token, the events with yours.

```bash
TICKETING_URL=$(gcloud run services describe ticketing-service --format='value(status.url)')
NOTIFICATIONS_URL=$(gcloud run services describe notification-service --format='value(status.url)')
curl -s -o /dev/null -w "%{http_code}\n" $TICKETING_URL/events                                      # 403
curl -s -H "Authorization: Bearer $(gcloud auth print-identity-token)" $TICKETING_URL/events | head -c 200
gcloud run services logs read notification-service --limit 20
```

## 6. The gateway, the only public address

`gateway-run` may call both services (`roles/run.invoker`). With `GATEWAY_CLOUDRUNAUTH_ENABLED=true` the gateway asks its instance's metadata server for an ID token per service and sends it instead of whatever `Authorization` the client sent ([IdentityTokens](../api-gateway/src/main/java/ro/frit/gateway/IdentityTokens.java)). Callers need `X-Api-Key`.

```bash
for service in ticketing-service notification-service; do
  gcloud run services add-iam-policy-binding $service --role=roles/run.invoker \
      --member="serviceAccount:gateway-run@$PROJECT_ID.iam.gserviceaccount.com"
done

MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy api-gateway \
    --image $REPO/api-gateway:$TAG \
    --service-account gateway-run@$PROJECT_ID.iam.gserviceaccount.com \
    --allow-unauthenticated --port 8080 --cpu 1 --memory 512Mi \
    --min-instances 0 --max-instances 2 \
    --set-env-vars GATEWAY_TICKETINGURI=$TICKETING_URL,GATEWAY_NOTIFICATIONSURI=$NOTIFICATIONS_URL,GATEWAY_CLOUDRUNAUTH_ENABLED=true \
    --set-secrets GATEWAY_API_KEY=gateway-api-key:latest \
    --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8080,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

> **New IAM bindings take a few minutes.** A 403 from a service right after `add-iam-policy-binding` is the binding still on its way: wait, don't change the configuration.

## 7. End to end

```bash
GATEWAY_URL=$(gcloud run services describe api-gateway --format='value(status.url)')
curl -s -o /dev/null -w "no key: %{http_code}\n" $GATEWAY_URL/events                               # 401
curl -s -H "X-Api-Key: $GATEWAY_API_KEY" $GATEWAY_URL/events | head -c 300
curl -s -X POST $GATEWAY_URL/events/cloud-iasi/bookings -H "X-Api-Key: $GATEWAY_API_KEY" \
    -H "X-Correlation-Id: cloud-test-1" -H "Content-Type: application/json" \
    -d '{"customerEmail":"ana@example.com","seats":1}'
curl -s -H "X-Api-Key: $GATEWAY_API_KEY" "$GATEWAY_URL/notifications?customerEmail=ana@example.com"
gcloud logging read 'textPayload:"cloud-test-1"' --limit 20 --format='value(resource.labels.service_name,textPayload)'
```

The last command finds one booking in the logs of all three services: the correlation id from [Session 7](../README.md#following-one-request), now in Cloud Logging.

## 8. Costs: down when you stop

Ticketing and the gateway scale to zero by themselves. `notification-service` does not:

```bash
gcloud run services update notification-service --min-instances 0   # done for today
gcloud run services update notification-service --min-instances 1   # an hour before a demo
```

From now on a push to `main` deploys by itself: [github-actions.md](github-actions.md) (Session 10). Going back to an older revision: [rollback.md](rollback.md). Removing everything at the end: [cleanup.md](cleanup.md).
