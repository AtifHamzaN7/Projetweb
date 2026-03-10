#!/usr/bin/env bash
set -euxo pipefail

# Basic packages
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release

# Install Docker (using Docker's repo)
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable docker
systemctl start docker

# Create deploy directory
mkdir -p /home/${SSH_USER}/deploy
chown -R ${SSH_USER}:${SSH_USER} /home/${SSH_USER}/deploy

# Allow non-root docker usage (handy for SSH deploy)
usermod -aG docker ${SSH_USER}