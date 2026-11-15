# Deploying from GitHub Actions (Session 10)

A commit on `main` is the only way into production: built, tested, named and deployed by the pipeline, with no key anywhere, and undone in seconds ([rollback.md](rollback.md)). The pipeline is [.github/workflows/build.yml](../.github/workflows/build.yml); it needs the manual deploy of [cloud-run.md](cloud-run.md) first, because it only swaps the image of services that already exist.

## No key: Workload Identity Federation

The usual way is a service-account key, a JSON file in a GitHub secret: a password that never expires, and whoever copies it is you. Here nothing is stored:

1. **GitHub** signs a token for each run: this repository, this branch (`id-token: write`, on the deploy job only).
2. **The provider** `github-actions` in pool `github` checks that GitHub signed it and that the repository and branch are ours.
3. **Google** exchanges it for a short-lived token (STS + IAM Credentials APIs).
4. The run acts as **`github-deployer`** for an hour.

`google-github-actions/auth` does steps 2–4. The lock is one line on the provider:

    --attribute-condition="assertion.repository=='kaffailevi/frit-ticketing-app' && assertion.ref=='refs/heads/main'"

Without it any repository on GitHub could present a valid GitHub token to the pool. The branch is checked on Google's side too: the workflow's `if:` can be edited on any branch.

## The set-up, once per project

[setup-pipeline.sh](setup-pipeline.sh) runs these four steps (safe to run again):

    PROJECT_ID=frit-ticketing-2026 GITHUB_REPO=kaffailevi/frit-ticketing-app bash deploy/setup-pipeline.sh

1. The pool `github` and the provider `github-actions`, with the condition above.
2. The deployer `github-deployer` and only the roles deploying needs:

   | Role | On what | So that it can |
   | --- | --- | --- |
   | `artifactregistry.writer` | the `ticketing` repository | push images |
   | `run.developer` | the project | update the services |
   | `iam.serviceAccountUser` | `ticketing-run`, `notification-run`, `gateway-run` | deploy them as those accounts |

   It cannot read the secrets (the services read them, not the deployer) and cannot change who may call a service.
3. Only this repository may act as the deployer (`iam.workloadIdentityUser` for the repository's principal set). The deployer has no keys, and it stays that way:

       gcloud iam service-accounts keys list --iam-account github-deployer@frit-ticketing-2026.iam.gserviceaccount.com --managed-by user

4. Four repository **variables** (not secrets: they name things, they don't unlock them, and they show in the public run logs): `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WIF_PROVIDER`, `GCP_DEPLOYER`.

## The pipeline

Four jobs, each waiting for the one before. A red one stops everything after it.

| Job | Runs on | Does |
| --- | --- | --- |
| `compile` | every push and pull request | `./gradlew assemble testClasses` |
| `test` | after `compile` | `./gradlew build`: every test (Testcontainers, ArchUnit) |
| `images` | after `test` | `docker compose build`: the Dockerfile still builds the three images |
| `deploy` | after `images`, only on `main`, only if `GCP_PROJECT_ID` is set | see below |

The deploy job:

1. trades GitHub's token for an hour as `github-deployer`;
2. builds the three images tagged with the **commit SHA** and pushes them to `ticketing` (not `:latest`, which moves with every push, and not a hand-picked tag, which can name two images);
3. gives each service the new image and a `commit=<sha>` label, and nothing else: the account, port, profile, hosts, secrets and probes from `cloud-run.md` stay. `gcloud run deploy` waits until the new revision passes its readiness probe, so a service that cannot reach Atlas or CloudAMQP fails the job;
4. sends all traffic to the new revisions (`--to-latest`, which also undoes a rollback's pin);
5. smoke-tests the public gateway: `/actuator/health` answers, and `/events` **without a key is refused with 401**. The trainer's pipeline calls `/events` and expects 200; here the gateway asks for `X-Api-Key`, and the key is deliberately not in GitHub.

One deploy at a time (`concurrency: production`, `cancel-in-progress: false`): a deploy that has started finishes, the next one waits. There is no approval step: a green build on `main` goes straight to Cloud Run. To require one, create a GitHub environment `production` (Settings → Environments, yourself as required reviewer, `main` only) and add `environment: production` to the deploy job.

To deploy `main` again without a commit: GitHub → Actions → Build → Run workflow, or `gh workflow run build.yml --ref main`.

## Checking a deploy

    gh run watch
    gcloud run revisions list --service api-gateway --region europe-west1 --limit 1 --format="value(metadata.labels.commit)"
    git rev-parse HEAD     # the same SHA

## When something goes wrong

| What you see | Why | What to do |
| --- | --- | --- |
| `auth`: `unauthorized_client` / rejected by the attribute condition | the run is not on `main`, or the repository name differs | `gh repo view --json nameWithOwner`; fix the provider with `gcloud iam workload-identity-pools providers update-oidc github-actions --location=global --workload-identity-pool=github --attribute-condition=…` |
| `auth`: `iam.serviceAccounts.getAccessToken` denied | the `workloadIdentityUser` binding names another repository | run step 3 of the script again with the right `GITHUB_REPO` |
| `docker push`: denied | the deployer cannot write to `ticketing` | step 2 of the script again |
| `deploy` skipped | the variables are missing, or not on `main` | `gh variable list` |
| `gcloud run deploy` times out | the new revision never got ready | Cloud Run → the service → Logs; the old revision keeps serving meanwhile |
