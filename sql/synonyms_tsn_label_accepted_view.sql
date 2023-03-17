CREATE 
    ALGORITHM = UNDEFINED 
    DEFINER = `root`@`%` 
    SQL SECURITY DEFINER
VIEW `synonyms_tsn_label_accepted` AS
    SELECT 
        `synonym_links`.`tsn` AS `tsn`,
        `synonym_links`.`tsn_accepted` AS `tsn_accepted`,
        `taxonomic_units`.`complete_name` AS `tsn_label_accepted`
    FROM
        (`taxonomic_units`
        JOIN `synonym_links`)
    WHERE
        (`synonym_links`.`tsn_accepted` = `taxonomic_units`.`tsn`)