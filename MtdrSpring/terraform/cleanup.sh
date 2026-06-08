#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# cleanup.sh — Limpieza responsable de recursos del laboratorio
# ─────────────────────────────────────────────────────────────────────────────
# Apaga/elimina los recursos creados para evitar costos al cierre del lab.
# Todos los recursos llevan el tag freeform `project=mytodolist-sprintly`
# (ver tags.tf), por lo que se pueden inventariar y verificar antes de destruir.
#
# Uso:
#   ./cleanup.sh inventory   # solo lista los recursos etiquetados (no destruye)
#   ./cleanup.sh k8s         # elimina los workloads de OKE (app, qdrant, HPA)
#   ./cleanup.sh destroy     # terraform destroy de TODA la infraestructura
#   ./cleanup.sh all         # k8s + destroy
#
# Requisitos: oci-cli configurado, kubectl con el kubeconfig de OKE, terraform.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

PROJECT_TAG="mytodolist-sprintly"
NAMESPACE="mtdrworkshop"
COMPARTMENT_OCID="${OCI_COMPARTMENT_OCID:-}"

inventory() {
  echo "== Inventario de recursos con tag project=${PROJECT_TAG} =="
  if [ -z "$COMPARTMENT_OCID" ]; then
    echo "!! Define OCI_COMPARTMENT_OCID para inventariar vía OCI Search."
    return 0
  fi
  oci search resource structured-search --query-text \
    "query all resources where freeformTags.key = 'project' && freeformTags.value = '${PROJECT_TAG}'" \
    --output table --query 'data.items[*].{Type:"resource-type",Name:"display-name",State:"lifecycle-state"}'
}

cleanup_k8s() {
  echo "== Eliminando workloads de OKE en namespace ${NAMESPACE} =="
  kubectl delete -f src/main/resources/todolistapp-springboot.yaml -n "$NAMESPACE" --ignore-not-found
  kubectl delete -f src/main/resources/qdrant.yaml -n "$NAMESPACE" --ignore-not-found
  # El PVC de Qdrant (Block Storage) NO se borra con el StatefulSet: hay que forzarlo.
  kubectl delete pvc -l app=qdrant -n "$NAMESPACE" --ignore-not-found
  echo "Workloads eliminados. El LoadBalancer de OCI se libera al borrar el Service."
}

destroy_infra() {
  echo "== terraform destroy (cluster OKE, ATP, VCN, bucket, repo) =="
  echo "Esto ELIMINA toda la infraestructura. Ctrl-C para abortar."
  read -r -p "Escribe 'destroy' para confirmar: " confirm
  [ "$confirm" = "destroy" ] || { echo "Abortado."; exit 1; }
  terraform destroy -auto-approve
}

case "${1:-}" in
  inventory) inventory ;;
  k8s)       cleanup_k8s ;;
  destroy)   destroy_infra ;;
  all)       cleanup_k8s; destroy_infra ;;
  *)
    echo "Uso: $0 {inventory|k8s|destroy|all}"
    exit 1
    ;;
esac
