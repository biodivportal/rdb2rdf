CREATE 
    ALGORITHM = UNDEFINED 
    DEFINER = `root`@`%` 
    SQL SECURITY DEFINER
VIEW `kingdom_to_taxa` AS
    SELECT 
        `taxonomic_units`.`tsn` AS `tsn`,
        `kingdoms`.`kingdom_name` AS `kingdom_name`
    FROM
        (`taxonomic_units`
        JOIN `kingdoms`)
    WHERE
        (`taxonomic_units`.`kingdom_id` = `kingdoms`.`kingdom_id`)