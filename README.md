BiomeTag ![build status](https://ci.biome.pw/BiomeTag/badge)
=======================

```sql
create table biometag.`tag-data`
(
    username            text       not null,
    uuid                text       not null,
    tagged              tinyint(1) null,
    `times-tagged`      int        null,
    `total-time-tagged` int        null,
    tagger              text       null,
    constraint `tag-data_uuid_uindex`
        unique (uuid) using hash
);
```