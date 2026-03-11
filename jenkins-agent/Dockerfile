FROM maven:3.9.6-eclipse-temurin-17
USER root

# Install docker CLI + Node.js 20 so this image can run Maven, Docker and AI test generation scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg docker.io \
    && install -d -m 0755 /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Ensure docker group has GID 101 (matches host) so --group-add 101 works
RUN groupmod -g 101 docker || true

# Ensure a non-root user with UID 1000 (matches Jenkins workspace permissions)
RUN useradd -m -u 1000 jenkins || true
# Add jenkins user to docker group so it can access the socket
RUN usermod -aG docker jenkins || true
USER 1000
WORKDIR /home/jenkins
