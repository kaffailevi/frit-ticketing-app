# Removing everything from Google Cloud

Everything [cloud-run.md](cloud-run.md) and [setup-pipeline.sh](setup-pipeline.sh) created. **Only once nobody needs the system any more:** afterwards the gateway URL is gone. Deleting cannot be undone (except the whole project, for 30 days). Git Bash, from the repository root.

## 1. Stop the pipeline from deploying

Without the variables the `deploy` job is skipped, so later pushes stay green instead of failing.

```bash
for v in GCP_PROJECT_ID GCP_REGION GCP_WIF_PROVIDER GCP_DEPLOYER; do gh variable delete $v; done
gh variable list   # empty
```

## 2. The right project

```bash
export PROJECT_ID=frit-ticketing-2026 REGION=europe-west1
gcloud config set project $PROJECT_ID
gcloud run services list   # must show api-gateway, ticketing-service, notification-service
```

## 3a. The whole project (simplest)

```bash
gcloud projects delete $PROJECT_ID
gcloud projects describe $PROJECT_ID --format="value(lifecycleState)"   # DELETE_REQUESTED
```

Everything stops and nothing more is billed. `gcloud projects undelete $PROJECT_ID` brings it back within 30 days.

## 3b. Or piece by piece (the project stays, empty)

```bash
gcloud run services delete api-gateway --region $REGION --quiet
gcloud run services delete ticketing-service --region $REGION --quiet
gcloud run services delete notification-service --region $REGION --quiet
gcloud artifacts repositories delete ticketing --location=$REGION --quiet
gcloud secrets delete mongodb-password --quiet
gcloud secrets delete cloudamqp-password --quiet
gcloud secrets delete gateway-api-key --quiet
gcloud iam workload-identity-pools providers delete github-actions --location=global --workload-identity-pool=github --quiet
gcloud iam workload-identity-pools delete github --location=global --quiet   # the name stays reserved 30 days
for sa in github-deployer gateway-run ticketing-run notification-run; do
  gcloud iam service-accounts delete $sa@$PROJECT_ID.iam.gserviceaccount.com --quiet
done
```

The deployer's project role stays behind as `deleted:serviceAccount:…`. It grants nothing; remove it anyway:

```bash
gcloud projects get-iam-policy $PROJECT_ID --flatten="bindings[].members" --filter="bindings.members:github-deployer" --format="value(bindings.role,bindings.members)"
gcloud projects remove-iam-policy-binding $PROJECT_ID --role=roles/run.developer --member="<the deleted:serviceAccount:… member printed above>" --condition=None
```

Check, then optionally stop billing on the empty project:

```bash
gcloud run services list; gcloud artifacts repositories list; gcloud secrets list
gcloud iam workload-identity-pools list --location=global
gcloud iam service-accounts list --filter="email~-run@ OR email~github-deployer"
gcloud billing projects unlink $PROJECT_ID
```

## 4. Outside Google Cloud

- The local images: `docker image ls --filter "reference=*-docker.pkg.dev/*/ticketing/*"`, then `docker image rm …`.
- The budget alert belongs to the billing account; Atlas and CloudAMQP are free tiers in their own consoles.
- This repository stays as it is: the code, the workflow and its run history.
- A week later: Billing → Reports, filtered on the project: the cost line stops at the clean-up day.
