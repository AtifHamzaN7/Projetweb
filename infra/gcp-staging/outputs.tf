output "staging_ip" {
  value = google_compute_address.staging_ip.address
}

output "ssh_user" {
  value = var.ssh_user
}

output "app_url" {
  value = "http://${google_compute_address.staging_ip.address}:${var.app_port}"
}
