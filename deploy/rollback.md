# Rolling back a bad deploy

Every deploy creates a new Cloud Run revision, and a revision never changes: it keeps the image (tagged with the commit's SHA) and the configuration it was created with. A rollback rebuilds nothing: it moves the traffic back to a revision that worked. It takes seconds.

## 1. Find the revision to go back to

```bash
gcloud run revisions list --service api-gateway --region europe-west1 \
    --format="table(metadata.name,metadata.labels.commit,metadata.creationTimestamp)"
```

The newest is first; usually you want the second. The `commit` label is the commit it runs (`git show <sha>`). Revisions deployed by hand (Session 9) have no label.

## 2. Send all traffic to it

```bash
gcloud run services update-traffic api-gateway --region europe-west1 --to-revisions <revision>=100
gcloud run services describe api-gateway --region europe-west1 --format='value(status.traffic)'
curl -fsS "$(gcloud run services describe api-gateway --region europe-west1 --format='value(status.url)')/actuator/health"
```

The old revision may have no instance running, so the first request can be slow or fail while one starts. Try again before deciding the rollback did not work.

Roll back only the service that broke: the services depend on each other's API and messages, not on revision numbers.

## 3. Fix forward

While the traffic is pinned to an old revision, **a new revision gets no traffic by itself**. That is why the pipeline ends every deploy with `--to-latest`. So the way out of a rollback is: fix the code, push to `main`, and let the pipeline deploy it and move the traffic. To undo the rollback by hand, without a new deploy:

```bash
gcloud run services update-traffic api-gateway --region europe-west1 --to-latest
```
