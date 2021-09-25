#!/usr/bin/env bash

SCRIPT_PATH=$( cd "$(dirname "$0")" ; pwd -P )
MODULE=${1}
COMMAND=${2}
COMPOSE_FILE=

function checkComposeFile(){
  if [[ ! -e "${SCRIPT_PATH}/../compose/base-docker-compose.yaml" ]];then
    echo "base-docker-compose.yaml not found."
    exit 1
  fi
  if [[ ! -e "${SCRIPT_PATH}/../compose/${COMPOSE_FILE}" ]];then
    echo "${COMPOSE_FILE} not found."
    exit 1
  fi
}

function clean(){
  cd "${SCRIPT_PATH}"/../compose && docker-compose  -f base-docker-compose.yaml -f ${COMPOSE_FILE} down -v
}

function build(){
  cd "${SCRIPT_PATH}"/../../ && ./gradlew clean build
  if [[ ${?} != 0 ]]; then
    echo "Failed to gradle build"
    exit 1
  fi
  cd "${SCRIPT_PATH}"/../compose && docker-compose -f base-docker-compose.yaml -f ${COMPOSE_FILE} build
}

function up(){
  cd "${SCRIPT_PATH}"/../compose && docker-compose -f base-docker-compose.yaml -f ${COMPOSE_FILE} up --force-recreate -d
}

function down(){
  cd "${SCRIPT_PATH}"/../compose && docker-compose -f base-docker-compose.yaml -f ${COMPOSE_FILE} down -v
}

# Module
if [[ ${MODULE} == "burrow" ]]; then
  COMPOSE_FILE=burrow-docker-compose.yaml
elif [[ ${MODULE} == "cmak" ]]; then
  COMPOSE_FILE=cmak-docker-compose.yaml
else
  echo "Unknown MODULE: ${MODULE}."
  echo $"Available: {burrow|cmak}"
  exit 1
fi

checkComposeFile

case "$COMMAND" in
  up)
      up
      ;;
  build)
      build
      ;;
  down)
      down
      ;;
  clean)
      clean
      ;;
  restart)
      down
      clean
      up
      ;;
  *)
      echo $"Usage: ${COMMAND} {up|down|build|clean|restart}"
      exit 1
esac
