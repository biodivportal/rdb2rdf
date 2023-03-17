CREATE 
    ALGORITHM = UNDEFINED 
    DEFINER = `root`@`%` 
    SQL SECURITY DEFINER
VIEW `rank_to_taxa` AS
    SELECT 
        `taxonomic_units`.`tsn` AS `tsn`,
        `taxon_unit_types`.`rank_name` AS `rank_name`
    FROM
        (`taxonomic_units`
        JOIN `taxon_unit_types`)
    WHERE
        (`taxonomic_units`.`rank_id` = `taxon_unit_types`.`rank_id`)