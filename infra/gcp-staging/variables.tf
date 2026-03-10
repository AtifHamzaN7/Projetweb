variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "europe-west9"
}

variable "zone" {
  type    = string
  default = "europe-west9-a"
}

variable "name" {
  type    = string
  default = "staging"
}

variable "machine_type" {
  type    = string
  default = "e2-micro"
}

variable "ssh_user" {
  type    = string
  default = "jenkins"
}

# Your *public* IP in CIDR form, e.g. "203.0.113.10/32"
# Use "0.0.0.0/0" only for quick demos (not recommended).
variable "allowed_ssh_cidr" {
  type = string
}

variable "allowed_app_cidr" {
  type = string
}

variable "app_port" {
  type    = number
  default = 8082
}

# Paste your public SSH key (e.g. contents of ~/.ssh/id_ed25519.pub)
variable "ssh_public_key" {
  type = string
}
