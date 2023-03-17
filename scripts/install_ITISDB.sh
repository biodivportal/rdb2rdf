#! /bin/bash

cd ../itis

# install actual database
echo "Installing ITIS Database"
mysql -uroot -pexample < CreateDB.sql
mysql -uroot -pexample -D ITIS < ITIS.sql # writes values

# add additional views
echo "Installing ITIS Views"
mysql -uroot -pexample -D ITIS < synonyms_tsn_label_accepted_view.sql
mysql -uroot -pexample -D ITIS < synonyms_tsn_label_view.sql
mysql -uroot -pexample -D ITIS < author_to_taxa.sql
mysql -uroot -pexample -D ITIS < kingdom_to_taxa.sql
mysql -uroot -pexample -D ITIS < rank_to_taxa.sql
