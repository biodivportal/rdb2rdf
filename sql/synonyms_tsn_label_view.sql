CREATE 
    ALGORITHM = UNDEFINED 
    DEFINER = `root`@`%` 
    SQL SECURITY DEFINER
VIEW `synonyms_tsn_label` AS
    SELECT 
        `taxonomic_units`.`tsn` AS `tsn`,
        `synonym_links`.`tsn_accepted` AS `tsn_accepted`,
        `taxonomic_units`.`complete_name` AS `tsn_label`
    FROM
        (`taxonomic_units`
        JOIN `synonym_links`)
    WHERE
        (`taxonomic_units`.`tsn` = `synonym_links`.`tsn`)