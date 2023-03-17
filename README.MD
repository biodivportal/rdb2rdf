# Wie mappe ich eine relationale Datenbank auf eine Ontologie/Terminologie?

## Tools

- [Karma](https://github.com/usc-isi-i2/Web-Karma)
- MySQL oder PostgreSQL Datenbank (dafür wird ein [Treiber](https://github.com/usc-isi-i2/Web-Karma/issues/362) benötigt)
- Docker

## Ablauf

### ITIS

_Es ist zu beachten, dass die Datenbank-Dumps des Vormonats meist am Anfang des neuen Monats online gestellt werden; diese überschreiben die alten Dumps, d.h. es kann immer nur der aktuelle Dump ausgelesen werden_

1. Schema-Mapping-File in Protégé erstellen
2. Karma installieren/herunterladen und starten:
   - Bspw. <https://github.com/usc-isi-i2/Web-Karma> klonen
   - In Unterordner karma-web wechseln und `mvn -Djetty.port=9999 jetty:run`
3. Mapping-File in Karma hochladen (als Ontology)
4. Datenquellen importieren:
   - Datenbank-Tabellen laden (`Import -> Database Table`):`taxonomic_units`, `taxon_unit_types`, `vernaculars`, `kingdoms`, `synonym_links`, `synonyms_tsn_label`, `synonyms_tsn_label_accepted`, `taxon_authors_lkp`
5. Wenn bereits Karma-Models vorhanden sind sollten diese nun für ihre entsprechende Tabelle hochgeladen werden:
   - `Apply R2RML Model -> From File` (bei erfolgreicher Anwendung erscheint eine Meldung `Model successfully applied`)
6. Wenn kein(e) Model(s) vorhanden sind, damit starten die `Base URI` anzuanpassen, z.B. `http://terminologies.gfbio.org/ITIS/`
7. Daten mit Mapping verbinden:
   - Spalte `tsn` als URI formatieren (`PyTransform` -> `<http://terminologies.gfbio.org/ITIS/Taxa_xx>`) und als `uri` per `Semantic Type` setzen
   - `complete_name`:`label`
   - usw. usf.
   - auch `parent_tsn` als URI wie oben zusammensetzen
   - für komplexe Properties: `Add Incoming/Outgoing Link`
8. `Publish -> Model -> Replace Existing Data -> Create new Context`, anschließend das Model herunterladen
9. Nachbearbeitung: Setze '/Taxa\_' noch als 'rdfs:subClassOf' und den richtigen Type ('owl:Class')
   <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> ; <http://www.w3.org/2000/01/rdf-schema#subClassOf> <https://terminologies.gfbio.org/ITIS#Taxa> .
   <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> # für Taxa
   <https://terminologies.gfbio.org/ITIS#parent> -> <http://www.w3.org/2000/01/rdf-schema#subClassOf>
   <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> ; <http://www.w3.org/2000/01/rdf-schema#subClassOf> <https://terminologies.gfbio.org/ITIS#TaxonomicRank> .
   <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> ; <http://www.w3.org/2000/01/rdf-schema#subClassOf> <https://terminologies.gfbio.org/ITIS#Kingdom>
   <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> ; <http://www.w3.org/2000/01/rdf-schema#subClassOf> <https://terminologies.gfbio.org/ITIS#Author>

10. `start_ITISDB.sh` ausführen. Dies baut den Container für die ITIS MySQL-Datenbank und initialisiert alle benötigten Skripte und Views. Die Installation ist abgeschlossen, wenn auf der Kommandozeile folgende Zeile erscheint:
    `/usr/sbin/mysqld: ready for connections. Version: '8.0.29'  socket: '/var/run/mysqld/mysqld.sock'  port: 3306  MySQL Community Server - GPL.`

Über die `rdb2rdf-x.x.jar` kann der Prozess der Umwandlung nun angestossen werden.

### GEONAMES

## Aufbau

Der prinzipielle Aufbau eines Konzepts aus GEONAMES kann dem folgenden Listing entnommen werden:

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

Obwohl durch den Dump die Felder `admin1_code` bis `admin4_code` bereitgestellt werden (aber nicht immer), können daraus NICHT die Werte für die Properties `gn:parentADM1` bis `gn:parentADM4` abgeleitet werden. Diese Properties bezeichnen quasi die zugehörige Hierarchie, z.B. Ort-Landkreis-Bundesland-Staat.

[GEONAMES codes](https://www.geonames.org/export/codes.html)

## Ablauf

- Geonames Dumps sind unter [dumps](http://download.geonames.org/export/dump/) herunterladbar
  - pro Land stehen die benötigten Dateien als `.zip` zur Verfügung, z.B. `DE.zip`
  - Dieses Archiv enthält eine `readme.txt` und eine `DE.txt`, letztere ist der eigentliche "Dump"
  - Dumps werden quasi jeden Tag aktualisiert
- Um den Dump zu mappen müssen folgende Pre-Processing Schritte vollzogen werden:
  1. Umwandlung der rohen Daten (aus `.txt`) in eine CSV-Datei. Dies geschieht per Java.
  2. Vervollständigen der Hierarchie in der CSV. Dies kann u.U. 2h in Anspruch nehmen.
  3. Sollte das Java-Programm abbrechen und einen Python-bezogenen Fehler anzeigen muss das Preprocessing-Skript manuell auf der Kommandozeile ausgeführt werden, z.B. `python3 /var/opt/ts/buildGEONAMESHierarchy.py /var/opt/ts/ontologies/GEONAMES/DE.csv DE /var/opt/ts/ontologies/GEONAMES/`
