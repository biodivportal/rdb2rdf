RDB2RDF is a tool for transforming a relational database dump into an ontology. CSV files or database tables are transformed into RDF triples using a mapping created in advance using the Karma Data Integration tool.

## Requirements

- [Karma](https://github.com/usc-isi-i2/Web-Karma)
- MySQL oder PostgreSQL Database (a [JDBC driver](https://github.com/usc-isi-i2/Web-Karma/issues/362) is needed)
- Docker/Docker-Compose >= 2.2.3
- JDK >= 11
- Maven >= 3.6.3
- At least 16 GB RAM

This repository contains transformation Pipeline Wrappers has been implemented for ITIS (https://www.itis.gov/) and Geonames (http://www.geonames.org/).

### ITIS

ITIS database dumps of the previous month are available [online](https://www.itis.gov/downloads/index.html) at the beginning of a new month; these overwrite the old dumps, i.e. only the current dump can be retrieved.
1. Data to be processed need to be obtained, a Docker image is provided which downloads the latest database dump and deploys it to a MySQL database, also installing additional scripts and views needed for the transformation
2. Using the aforementioned Karma tool, a mapping about how relational data should be mapped to semantic data should be created. For ITIS mappings were already created and are provided in the GitLab repository (see above) under /models/ITIS
3. After creating the required models the rdb2rdf tool can be used to begin the actual transformation. It connects itself to Karma using a karma-offline.jar, which acts as a bridge between models, the database, and the semantic output data.
4. For every model supplied all data of its referencing table will be exported to JSON first; afterwards, when the data collecting has finished, the actual triples (.ttl) will be created, using the JSON files as input for the transformation process provided by Karma.
    Finally, all triple files will be merged into one final .ttl (by using the org.apache.jena package)

### GEONAMES

The basic structure of a concept from GEONAMES is as follows:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rdf:RDF xmlns:cc="http://creativecommons.org/ns#" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:gn="http://www.geonames.org/ontology#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#">
    <gn:Feature rdf:about="https://sws.geonames.org/2922731/">
        <rdfs:isDefinedBy rdf:resource="https://sws.geonames.org/2922731/about.rdf"/>
        <gn:name>Ganderkesee</gn:name>
        <gn:alternateName>Ganderkesee</gn:alternateName>
        <gn:alternateName xml:lang="kk">Гандеркезе</gn:alternateName>
        <gn:alternateName xml:lang="ru">Гандеркезе</gn:alternateName>
        <gn:alternateName xml:lang="sr">Гандеркезе</gn:alternateName>
        <gn:alternateName xml:lang="zh">甘德尔克塞</gn:alternateName>
        <gn:featureClass rdf:resource="https://www.geonames.org/ontology#P"/>
        <gn:featureCode rdf:resource="https://www.geonames.org/ontology#P.PPLA4"/>
        <gn:countryCode>DE</gn:countryCode>
        <gn:population>31141</gn:population>
        <wgs84_pos:lat>53.03333</wgs84_pos:lat>
        <wgs84_pos:long>8.53333</wgs84_pos:long>
        <gn:parentFeature rdf:resource="https://sws.geonames.org/6552965/"/>
        <gn:parentCountry rdf:resource="https://sws.geonames.org/2921044/"/>
        <gn:parentADM1 rdf:resource="https://sws.geonames.org/2862926/"/>
        <gn:parentADM3 rdf:resource="https://sws.geonames.org/2857455/"/>
        <gn:parentADM4 rdf:resource="https://sws.geonames.org/6552965/"/>
        <gn:nearbyFeatures rdf:resource="https://sws.geonames.org/2922731/nearby.rdf"/>
        <gn:locationMap rdf:resource="https://www.geonames.org/2922731/ganderkesee.html"/>
    </gn:Feature>
    <foaf:Document rdf:about="https://sws.geonames.org/2922731/about.rdf">
        <foaf:primaryTopic rdf:resource="https://sws.geonames.org/2922731/"/>
        <cc:license rdf:resource="https://creativecommons.org/licenses/by/4.0/"/>
        <cc:attributionURL rdf:resource="https://www.geonames.org"/>
        <cc:attributionName rdf:datatype="https://www.w3.org/2001/XMLSchema#string">GeoNames</cc:attributionName>
        <dcterms:created rdf:datatype="https://www.w3.org/2001/XMLSchema#date">2006-01-15</dcterms:created>
        <dcterms:modified rdf:datatype="https://www.w3.org/2001/XMLSchema#date">2011-07-31</dcterms:modified>
    </foaf:Document>
</rdf:RDF>
```

Geonames dumps are downloadable [here](http://download.geonames.org/export/dump/).
  - per country the required files are available as `.zip`, e.g. `DE.zip`.
  - This archive contains a `readme.txt` and a `DE.txt`, the latter is the actual "dump".
  - Dumps are updated almost every day
  
In order to map the dump the following pre-processing steps have to be done:

  1. conversion of the raw data (from `.txt`) into a CSV file.
  2. complete the hierarchy in the CSV. This can take up to 2 hours.
  3. if the Java program aborts and gives a Python-related error, the preprocessing script must be executed manually on the command line, e.g. `python3 /var/opt/ts/buildGEONAMESHierarchy.py /var/opt/ts/ontologies/GEONAMES/DE.csv DE /var/opt/ts/ontologies/GEONAMES/`.
