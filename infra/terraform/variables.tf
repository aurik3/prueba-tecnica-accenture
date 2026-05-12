variable "network_name" {
  type    = string
  default = "franchise-net"
}

variable "mongo_image" {
  type    = string
  default = "mongo:7.0"
}

variable "mongo_container_name" {
  type    = string
  default = "franchise-mongo-tf"
}

variable "mongo_volume_name" {
  type    = string
  default = "franchise-mongo-data"
}

variable "mongo_database" {
  type    = string
  default = "franchise_db"
}

variable "mongo_port" {
  type    = number
  default = 27017
}

