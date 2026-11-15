#!/usr/bin/env bash
#
# Session 10: lets GitHub Actions deploy to the Google Cloud project without any key (Workload Identity Federation).
# The four steps of deploy/github-actions.md in one file. Read it before you run it.
#
# Once, after deploy/cloud-run.md (it needs the "ticketing" repository and the three runtime service accounts),
# in Git Bash, from the repository root, signed in with `gcloud auth login` and `gh auth login`:
#
#   PROJECT_ID=frit-ticketing-2026 GITHUB_REPO=kaffailevi/frit-ticketing-app bash deploy/setup-pipeline.sh
#
# Safe to run again: whatever already exists is left as it is (a binding that exists is simply confirmed).

set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID to the Google Cloud project id}"
: "${GITHUB_REPO:?Set GITHUB_REPO to owner/name, exactly as on GitHub}"

REGION="${REGION:-europe-west1}"
POOL=github
PROVIDER=github-actions
DEPLOYER="github-deployer@$PROJECT_ID.iam.gserviceaccount.com"

gcloud config set project "$PROJECT_ID"
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')

echo
echo "== 1. The pool and the provider =="
# The provider trusts tokens signed by GitHub, but only from this repository's main branch. Without the repository
# any GitHub repository could use the pool; without the branch, a workflow edited on any branch could deploy.
if ! gcloud iam workload-identity-pools describe "$POOL" --location=global >/dev/null 2>&1; then
  gcloud iam workload-identity-pools create "$POOL" --location=global --display-name="GitHub Actions"
fi
if ! gcloud iam workload-identity-pools providers describe "$PROVIDER" --location=global --workload-identity-pool="$POOL" >/dev/null 2>&1; then
  gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
      --location=global --workload-identity-pool="$POOL" \
      --issuer-uri=https://token.actions.githubusercontent.com \
      --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
      --attribute-condition="assertion.repository=='$GITHUB_REPO' && assertion.ref=='refs/heads/main'"
fi

echo
echo "== 2. The deployer and its three roles =="
# Push images to this one repository, update Cloud Run services, deploy them as the three runtime accounts.
# Nothing else: it cannot read a secret, and cannot change who may call a service.
if ! gcloud iam service-accounts describe "$DEPLOYER" >/dev/null 2>&1; then
  gcloud iam service-accounts create github-deployer --display-name="GitHub Actions deployer"
fi
gcloud artifacts repositories add-iam-policy-binding ticketing --location="$REGION" \
    --member="serviceAccount:$DEPLOYER" --role=roles/artifactregistry.writer --format=none
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$DEPLOYER" --role=roles/run.developer --condition=None --format=none
for sa in ticketing-run notification-run gateway-run; do
  gcloud iam service-accounts add-iam-policy-binding "$sa@$PROJECT_ID.iam.gserviceaccount.com" \
      --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser --format=none
done

echo
echo "== 3. Only this repository may act as the deployer =="
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER" --role=roles/iam.workloadIdentityUser \
    --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL/attribute.repository/$GITHUB_REPO" \
    --format=none
echo "The deployer's keys (none, and it stays that way):"
gcloud iam service-accounts keys list --iam-account "$DEPLOYER" --managed-by user

echo
echo "== 4. The repository's four variables =="
# Variables, not secrets: they name things, they don't unlock them (and they show in the public run logs).
gh variable set GCP_PROJECT_ID   --repo "$GITHUB_REPO" --body "$PROJECT_ID"
gh variable set GCP_REGION       --repo "$GITHUB_REPO" --body "$REGION"
gh variable set GCP_WIF_PROVIDER --repo "$GITHUB_REPO" --body "projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL/providers/$PROVIDER"
gh variable set GCP_DEPLOYER     --repo "$GITHUB_REPO" --body "$DEPLOYER"
gh variable list --repo "$GITHUB_REPO"

echo
echo "Done. The next push to main deploys: gh run watch"
