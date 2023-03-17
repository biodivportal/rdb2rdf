CREATE VIEW `author_to_taxa` AS
	SELECT
		taxonomic_units.tsn AS `tsn`,
		strippedauthor.shortauthor AS `author_label`
	FROM
		(`taxonomic_units`
		JOIN `strippedauthor`)
    WHERE
		(`taxonomic_units`.`taxon_author_id` = `strippedauthor`.`taxon_author_id`)
