FROM mysql:debian

COPY ./sql /itis

COPY ./scripts/download_ITISDB.sh /opt

# this will download the latest version of the ITIS database dump
RUN /opt/download_ITISDB.sh

# script will be executed automatically by the container (after initialising MySQL)
COPY ./scripts/install_ITISDB.sh /docker-entrypoint-initdb.d
