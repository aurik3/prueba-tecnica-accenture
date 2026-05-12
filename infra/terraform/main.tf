terraform {
  required_version = ">= 1.6.0"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {}

resource "docker_network" "franchise" {
  name = var.network_name
}

resource "docker_volume" "mongo_data" {
  name = var.mongo_volume_name
}

resource "docker_image" "mongo" {
  name         = var.mongo_image
  keep_locally = false
}

resource "docker_container" "mongo" {
  name  = var.mongo_container_name
  image = docker_image.mongo.image_id

  env = [
    "MONGO_INITDB_DATABASE=${var.mongo_database}"
  ]

  ports {
    internal = 27017
    external = var.mongo_port
  }

  networks_advanced {
    name = docker_network.franchise.name
  }

  volumes {
    volume_name    = docker_volume.mongo_data.name
    container_path = "/data/db"
  }
}

