terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# Enable Compute API (needed for VM/network)
resource "google_project_service" "compute" {
  service            = "compute.googleapis.com"
  disable_on_destroy = false
}

resource "google_compute_network" "vpc" {
  name                    = "${var.name}-vpc"
  auto_create_subnetworks = false
  depends_on              = [google_project_service.compute]
}

resource "google_compute_subnetwork" "subnet" {
  name          = "${var.name}-subnet"
  ip_cidr_range = "10.10.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc.id
}

# Firewall: SSH
resource "google_compute_firewall" "ssh" {
  name    = "${var.name}-allow-ssh"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = [var.allowed_ssh_cidr]
  target_tags   = ["${var.name}-vm"]
}

# Firewall: app port (8082 by default)
resource "google_compute_firewall" "app" {
  name    = "${var.name}-allow-app"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = [tostring(var.app_port)]
  }

  source_ranges = [var.allowed_app_cidr]
  target_tags   = ["${var.name}-vm"]
}

# Reserve a static external IP (optional but helpful so Jenkins target doesn't change)
resource "google_compute_address" "staging_ip" {
  name   = "${var.name}-ip"
  region = var.region
}

# VM
data "google_compute_image" "ubuntu" {
  family  = "ubuntu-2204-lts"
  project = "ubuntu-os-cloud"
}

locals {
  startup_script = templatefile("${path.module}/cloud-init.sh", {
    SSH_USER = var.ssh_user
  })
}

resource "google_compute_instance" "vm" {
  name         = "${var.name}-vm"
  machine_type = var.machine_type
  tags         = ["${var.name}-vm"]

  boot_disk {
    initialize_params {
      image = data.google_compute_image.ubuntu.self_link
      size  = 20
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.subnet.id

    access_config {
      nat_ip = google_compute_address.staging_ip.address
    }
  }

  metadata = {
    # Inject SSH key into instance metadata:
    # Format: "user:ssh-rsa AAAA..." or "user:ssh-ed25519 AAAA..."
    ssh-keys = "${var.ssh_user}:${var.ssh_public_key}"
  }

  metadata_startup_script = local.startup_script

  depends_on = [
    google_project_service.compute
  ]
}
