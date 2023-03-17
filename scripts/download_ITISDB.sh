#! /bin/sh

echo "Downloading ITIS Database"
apt-get update
apt-get install -y curl
apt-get install -y unzip

FILENAME=itisDB+$(date "+%m-%d-%y")

# download latest ITIS DB image
curl https://www.itis.gov/downloads/itisMySQLBulk.zip -o ./$FILENAME.zip

# unzip archive
unzip -j $FILENAME -d itis
