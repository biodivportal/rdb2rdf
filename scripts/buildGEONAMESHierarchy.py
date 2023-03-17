import sys

def resolve_parent_geonameid(df, admin_code, admin_level):
    '''
        Parameters:
        df -- DataFrame Geonames CSV (loaded as DataFrame)
        admin_code -- Integer Value of the adminx_code
        admin_level -- Integer The current hierarchy level, e.g., 4
    '''
    # feature_class 'A' marks the administrative unit of the current item
    s = df[(df['feature_class'] == 'A') & (df['admin'+str(admin_level)+'_code'] == admin_code) & (df['feature_code'] == 'ADM'+str(admin_level))]['geonameid']

    if(s.size > 0):
        gid = s.values[0]
        return gid
    else:
        return np.nan

def iterate_adm_levels(df):
    '''
        Parameters:
        df -- DataFrame Geonames CSV (loaded as DataFrame)

    '''
    print(' iterating ADM levels ')

    # geonames hierarchy consists of 4 hierachy levels: 4 (fine) to 1 (coarse)
    # adminx_code is available in raw data and will be mapped to admx_feature
    # thus, we'll set the admx_feature of a certain hierarchy level of an item to the geonames id of its administrative unit
    adm_levels = [('adm4_feature', 'admin4_code', 4), ('adm3_feature', 'admin3_code', 3), ('adm2_feature', 'admin2_code', 2), ('adm1_feature', 'admin1_code', 1)]
    for lvl in adm_levels:
        print(' current lvl={} '.format(lvl))

        adm_feature = lvl[0]; admin_code = lvl[1]; admin_level = lvl[2]

        df[adm_feature] = df.apply(lambda row: resolve_parent_geonameid(df, row[admin_code], admin_level), axis=1)

def set_correct_parent_id(df):
    '''

    '''
    def lowest_adm_feature(row):
        if pd.isnull(row['adm4_feature']) == False:
            return row['adm4_feature']
        elif pd.isnull(row['adm3_feature']) == False:
            return row['adm3_feature']
        elif pd.isnull(row['adm2_feature']) == False:
            return row['adm2_feature']
        else:
            return row['adm1_feature']

    print(' setting parent ids ')
    df['parent'] = df.apply(lambda row: lowest_adm_feature(row), axis=1)


def read_dataframe(csv_file_path):
    '''

    '''

    print(' read in CSV {} '.format(csv_file_path))

    df = pd.read_csv(csv_file_path, delimiter='|', encoding='UTF-8', low_memory=False)
    df=df.drop(['cc2', 'dem', 'timezone', 'elevation'], axis=1)

    df['parent']=0
    df['adm4_feature']=0
    df['adm3_feature']=0
    df['adm2_feature']=0
    df['adm1_feature']=0

    print(" df.shape: {} ".format(df.shape))

    return df


if __name__ == "__main__":
    '''
        Main function. Converts a Geonames xx.CSV into a xx_MOD.csv, which contains compiled hierarchy data ('admx_feature')

    '''
    try:
        import time
        import pandas as pd
        import numpy as np
    except ModuleNotFoundError as error:
        print(" module pandas not found ")
        sys.exit(-1)

    path_to_csv = sys.argv[1]
    language_code = sys.argv[2]
    work_dir = sys.argv[3]

    if(len(sys.argv) < 3):
        print('missing arguments')
    else:
        startTime = time.time()

        geonames_de = read_dataframe(path_to_csv)
        iterate_adm_levels(geonames_de)
        set_correct_parent_id(geonames_de)

        executionTime = (time.time() - startTime)
        print(' Execution time in seconds: {} '.format(executionTime))
        print(" all done ")

        mod_csv_path=work_dir+language_code+"_MOD.csv"

        print(" writing to new CSV {}".format(mod_csv_path))
        # export final DataFrame to CSV
        geonames_de.to_csv(mod_csv_path, index=False, sep='|')
