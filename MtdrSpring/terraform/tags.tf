// ─────────────────────────────────────────────────────────────────────────────
// Tags de costo y gobierno
// ─────────────────────────────────────────────────────────────────────────────
// Etiquetas aplicadas a todos los recursos facturables para trazabilidad de
// costos y limpieza responsable. Se referencian como `freeform_tags = local.common_tags`.
//
// Permiten en OCI Cost Analysis filtrar/agrupar el gasto por proyecto, equipo,
// ambiente y centro de costo, y localizar qué destruir al cierre del laboratorio.

variable "environment" {
  description = "Ambiente de despliegue (dev | staging | prod | lab)"
  type        = string
  default     = "lab"
}

variable "ownerTeam" {
  description = "Equipo responsable de los recursos"
  type        = string
  default     = "equipo65"
}

variable "costCenter" {
  description = "Centro de costo para reportes de gasto"
  type        = string
  default     = "tec-devops-oci"
}

locals {
  common_tags = {
    "project"     = "mytodolist-sprintly"
    "environment" = var.environment
    "owner"       = var.ownerTeam
    "cost-center" = var.costCenter
    "managed-by"  = "terraform"
    "run-name"    = var.runName
  }
}
