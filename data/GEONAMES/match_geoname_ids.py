import pandas as pd

mapping=dict()

# read geonames DE data
geonames_de = pd.read_csv('./DE/DE_mod.csv', delimiter='\t', low_memory=False)
geonames_de['parent']=0
# read geonames hierarchy data
hierarchy = pd.read_csv('./hierarchy.csv', delimiter='\t', names=["parentId", "childId", "type"])
print(geonames_de.columns)
print(hierarchy.columns)
#for index, row in hierarchy.iterrows():
#    geonames_de['parent']=row['parentId']

# iterate both sheets and match ids
for row in geonames_de.head(geonames_de.shape[0]).itertuples():
    print(row[1])
    print(row[20])
#for i, row in geonames_de.iterrows():
    if row[1] in hierarchy['childId'].values:
        print(hierarchy[hierarchy['childId']==row[1]][0:1]['parentId'].values[0])
        #print(type(hierarchy[hierarchy['childId']==row['geonameid']]))
        geonames_de.at[row[0],'parent'] = hierarchy[hierarchy['childId']==row[1]][0:1]['parentId'].values[0]

#print(geonames_de.head(10))
df = pd.DataFrame.from_dict(mapping, orient="index")
df.to_csv("data.csv")

geonames_de.to_csv("DE_mod_2.csv")
