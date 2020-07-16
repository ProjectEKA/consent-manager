FROM alpine:3.11 AS build

# Note: Latest version of helm may be found at
# https://github.com/kubernetes/helm/releases
ENV HELM_VERSION="v3.2.4"

RUN wget -q https://get.helm.sh/helm-${HELM_VERSION}-linux-amd64.tar.gz -O - | tar -xzO linux-amd64/helm > /usr/local/bin/helm \
    && chmod +x /usr/local/bin/helm

FROM bitnami/kubectl:latest

COPY --from=build /usr/local/bin/helm /usr/bin/helm
