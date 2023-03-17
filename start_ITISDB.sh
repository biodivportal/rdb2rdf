#! /bin/sh

docker compose up --force-recreate --abort-on-container-exit --build db
