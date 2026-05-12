output "mongo_uri" {
  value = "mongodb://localhost:${var.mongo_port}/${var.mongo_database}"
}

output "mongo_container_name" {
  value = docker_container.mongo.name
}

