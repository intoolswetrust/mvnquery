# Docker mvnquery image

Distroless Java Docker image with mvnquery app.

Use the `latest` tag to have up-to-date version.

## How to run it

### Print help

```bash
docker run -it --rm kwart/mvnquery --help 
```

### Run default query and cache data to the host

```bash
docker run -it --rm -v /path/to/mvnquery-data:/data kwart/mvnquery
```

### Query different repository

```bash
docker run -it --rm -v /path/to/mvnquery-data:/data kwart/mvnquery \
  --config-repo https://repo.jenkins-ci.org/artifactory/releases \
  --lastDays 7
```
