FROM maven:3.9.6-eclipse-temurin-17
USER root

# Install docker CLI + Node.js so this image can run Maven, Docker and AI test generation scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io nodejs npm \
    && rm -rf /var/lib/apt/lists/*

# Ensure docker group has GID 101 (matches host) so --group-add 101 works
RUN groupmod -g 101 docker || true

# Ensure a non-root user with UID 1000 (matches Jenkins workspace permissions)
RUN useradd -m -u 1000 jenkins || true
# Add jenkins user to docker group so it can access the socket
RUN usermod -aG docker jenkins || true
USER 1000
WORKDIR /home/jenkins
