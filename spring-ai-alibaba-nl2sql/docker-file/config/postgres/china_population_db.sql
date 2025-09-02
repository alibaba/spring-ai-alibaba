/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE DATABASE china_population_db;

\c china_population_db;

-- 创建人口总数统计表
CREATE TABLE population_total (
                                  id SERIAL PRIMARY KEY,
                                  year INTEGER NOT NULL,
                                  total_population BIGINT NOT NULL,
                                  natural_growth_rate DECIMAL(5,2), -- 自然增长率（‰）
                                  birth_rate DECIMAL(5,2), -- 出生率（‰）
                                  death_rate DECIMAL(5,2), -- 死亡率（‰）
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  UNIQUE(year)
);

-- 创建性别比例统计表
CREATE TABLE gender_ratio (
                              id SERIAL PRIMARY KEY,
                              year INTEGER NOT NULL,
                              male_population BIGINT NOT NULL,
                              female_population BIGINT NOT NULL,
                              sex_ratio DECIMAL(6,2), -- 性别比（男性对女性的比例）
                              male_percentage DECIMAL(5,2), -- 男性占比（%）
                              female_percentage DECIMAL(5,2), -- 女性占比（%）
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE(year)
);

-- 创建年龄结构统计表
CREATE TABLE age_structure (
                               id SERIAL PRIMARY KEY,
                               year INTEGER NOT NULL,
                               age_group VARCHAR(20) NOT NULL, -- 年龄组：0-14, 15-64, 65+, etc.
                               population BIGINT NOT NULL,
                               percentage DECIMAL(5,2), -- 占总人口比例（%）
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE(year, age_group)
);

-- 创建城乡分布统计表
CREATE TABLE urban_rural_distribution (
                                          id SERIAL PRIMARY KEY,
                                          year INTEGER NOT NULL,
                                          urban_population BIGINT NOT NULL,
                                          rural_population BIGINT NOT NULL,
                                          urban_percentage DECIMAL(5,2), -- 城镇化率（%）
                                          rural_percentage DECIMAL(5,2),
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          UNIQUE(year)
);

-- 创建省份人口统计表
CREATE TABLE province_population (
                                     id SERIAL PRIMARY KEY,
                                     year INTEGER NOT NULL,
                                     province_name VARCHAR(50) NOT NULL,
                                     province_code VARCHAR(10), -- 省份代码
                                     total_population BIGINT NOT NULL,
                                     urban_population BIGINT,
                                     rural_population BIGINT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     UNIQUE(year, province_code)
);

-- 创建索引以提高查询性能
CREATE INDEX idx_population_total_year ON population_total(year);
CREATE INDEX idx_gender_ratio_year ON gender_ratio(year);
CREATE INDEX idx_age_structure_year ON age_structure(year);
CREATE INDEX idx_age_structure_group ON age_structure(age_group);
CREATE INDEX idx_urban_rural_year ON urban_rural_distribution(year);
CREATE INDEX idx_province_year ON province_population(year);
CREATE INDEX idx_province_code ON province_population(province_code);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- 为所有表创建触发器
CREATE TRIGGER update_population_total_updated_at
    BEFORE UPDATE ON population_total
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_gender_ratio_updated_at
    BEFORE UPDATE ON gender_ratio
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_age_structure_updated_at
    BEFORE UPDATE ON age_structure
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_urban_rural_distribution_updated_at
    BEFORE UPDATE ON urban_rural_distribution
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_province_population_updated_at
    BEFORE UPDATE ON province_population
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入初始数据 - 人口总数（2010-2022年真实数据）
INSERT INTO population_total (year, total_population, natural_growth_rate, birth_rate, death_rate) VALUES
                                                                                                       (2010, 1340910000, 4.79, 11.90, 7.11),
                                                                                                       (2011, 1347350000, 4.79, 11.93, 7.14),
                                                                                                       (2012, 1354040000, 4.95, 12.10, 7.15),
                                                                                                       (2013, 1360720000, 4.92, 12.08, 7.16),
                                                                                                       (2014, 1367820000, 5.21, 12.37, 7.16),
                                                                                                       (2015, 1374620000, 5.00, 12.07, 7.11),
                                                                                                       (2016, 1382710000, 5.86, 12.95, 7.09),
                                                                                                       (2017, 1390080000, 5.32, 12.43, 7.11),
                                                                                                       (2018, 1395380000, 3.81, 10.94, 7.13),
                                                                                                       (2019, 1400050000, 3.34, 10.48, 7.14),
                                                                                                       (2020, 1412120000, 1.45, 8.52, 7.07),
                                                                                                       (2021, 1412600000, 0.34, 7.52, 7.18),
                                                                                                       (2022, 1411750000, -0.60, 6.77, 7.37);

-- 插入初始数据 - 性别比例（部分年份真实数据）
INSERT INTO gender_ratio (year, male_population, female_population, sex_ratio, male_percentage, female_percentage) VALUES
                                                                                                                       (2010, 686850000, 654060000, 105.02, 51.24, 48.76),
                                                                                                                       (2015, 705410000, 669210000, 105.41, 51.27, 48.73),
                                                                                                                       (2020, 723340000, 688780000, 105.02, 51.22, 48.78),
                                                                                                                       (2022, 723520000, 688230000, 105.13, 51.25, 48.75);

-- 插入初始数据 - 年龄结构（2020年第七次人口普查数据）
INSERT INTO age_structure (year, age_group, population, percentage) VALUES
                                                                        (2020, '0-14', 253380000, 17.95),
                                                                        (2020, '15-64', 968710000, 68.58),
                                                                        (2020, '65+', 189510000, 13.47),
                                                                        (2020, '0-17', 298300000, 21.12),
                                                                        (2020, '60+', 264020000, 18.70);

-- 插入初始数据 - 城乡分布
INSERT INTO urban_rural_distribution (year, urban_population, rural_population, urban_percentage, rural_percentage) VALUES
                                                                                                                        (2010, 665570000, 675340000, 49.65, 50.35),
                                                                                                                        (2015, 771160000, 603460000, 56.05, 43.95),
                                                                                                                        (2020, 914250000, 508490000, 64.71, 35.29),
                                                                                                                        (2022, 920710000, 491040000, 65.22, 34.78);

-- 插入初始数据 - 省份人口（2020年数据）
INSERT INTO province_population (year, province_name, province_code, total_population) VALUES
                                                                                           (2020, '广东省', 'GD', 126010000),
                                                                                           (2020, '山东省', 'SD', 101530000),
                                                                                           (2020, '河南省', 'HA', 99370000),
                                                                                           (2020, '江苏省', 'JS', 84750000),
                                                                                           (2020, '四川省', 'SC', 83670000),
                                                                                           (2020, '河北省', 'HE', 74610000),
                                                                                           (2020, '湖南省', 'HN', 66440000),
                                                                                           (2020, '浙江省', 'ZJ', 64570000),
                                                                                           (2020, '安徽省', 'AH', 61030000),
                                                                                           (2020, '湖北省', 'HB', 57750000),
                                                                                           (2020, '上海市', 'SH', 24870000),
                                                                                           (2020, '北京市', 'BJ', 21890000),
                                                                                           (2020, '重庆市', 'CQ', 32050000);

-- 创建视图：综合人口统计视图
CREATE VIEW population_overview AS
SELECT
    pt.year,
    pt.total_population,
    pt.natural_growth_rate,
    gr.sex_ratio,
    ur.urban_percentage,
    pt.created_at
FROM population_total pt
         LEFT JOIN gender_ratio gr ON pt.year = gr.year
         LEFT JOIN urban_rural_distribution ur ON pt.year = ur.year
ORDER BY pt.year;

-- 创建视图：人口结构分析视图
CREATE VIEW population_structure_analysis AS
SELECT
        year,
        SUM(CASE WHEN age_group = '0-14' THEN percentage ELSE 0 END) as child_ratio,
        SUM(CASE WHEN age_group = '15-64' THEN percentage ELSE 0 END) as working_age_ratio,
        SUM(CASE WHEN age_group = '65+' THEN percentage ELSE 0 END) as elderly_ratio
        FROM age_structure
        WHERE age_group IN ('0-14', '15-64', '65+')
        GROUP BY year
        ORDER BY year;

-- 创建常用查询函数：获取某年份详细人口统计
CREATE OR REPLACE FUNCTION get_year_population_stats(target_year INTEGER)
RETURNS TABLE(
    year INTEGER,
    total_population BIGINT,
    male_population BIGINT,
    female_population BIGINT,
    urban_population BIGINT,
    rural_population BIGINT,
    child_population BIGINT,
    working_age_population BIGINT,
    elderly_population BIGINT
) AS $$
BEGIN
RETURN QUERY
SELECT
    pt.year,
    pt.total_population,
    gr.male_population,
    gr.female_population,
    ur.urban_population,
    ur.rural_population,
    (SELECT (population * as_child.percentage / 100)::BIGINT
     FROM age_structure as_child
     WHERE as_child.year = target_year AND as_child.age_group = '0-14') as child_population,
    (SELECT (population * as_work.percentage / 100)::BIGINT
     FROM age_structure as_work
     WHERE as_work.year = target_year AND as_work.age_group = '15-64') as working_age_population,
    (SELECT (population * as_elder.percentage / 100)::BIGINT
     FROM age_structure as_elder
     WHERE as_elder.year = target_year AND as_elder.age_group = '65+') as elderly_population
FROM population_total pt
         LEFT JOIN gender_ratio gr ON pt.year = gr.year
         LEFT JOIN urban_rural_distribution ur ON pt.year = ur.year
WHERE pt.year = target_year;
END;
$$ LANGUAGE plpgsql;

-- 创建存储过程：批量插入人口数据
CREATE OR REPLACE PROCEDURE batch_insert_population_data(
    data_year INTEGER,
    total_pop BIGINT,
    male_pop BIGINT,
    female_pop BIGINT,
    urban_pop BIGINT,
    rural_pop BIGINT
)
AS $$
BEGIN
    -- 插入总人口数据
INSERT INTO population_total (year, total_population)
VALUES (data_year, total_pop)
    ON CONFLICT (year) DO UPDATE SET total_population = EXCLUDED.total_population;

-- 插入性别数据
INSERT INTO gender_ratio (year, male_population, female_population, sex_ratio, male_percentage, female_percentage)
VALUES (
           data_year,
           male_pop,
           female_pop,
           ROUND((male_pop::DECIMAL / female_pop::DECIMAL * 100), 2),
           ROUND((male_pop::DECIMAL / (male_pop + female_pop) * 100), 2),
           ROUND((female_pop::DECIMAL / (male_pop + female_pop) * 100), 2)
       )
    ON CONFLICT (year) DO UPDATE SET
    male_population = EXCLUDED.male_population,
                              female_population = EXCLUDED.female_population,
                              sex_ratio = EXCLUDED.sex_ratio,
                              male_percentage = EXCLUDED.male_percentage,
                              female_percentage = EXCLUDED.female_percentage;

-- 插入城乡分布数据
INSERT INTO urban_rural_distribution (year, urban_population, rural_population, urban_percentage, rural_percentage)
VALUES (
           data_year,
           urban_pop,
           rural_pop,
           ROUND((urban_pop::DECIMAL / (urban_pop + rural_pop) * 100), 2),
           ROUND((rural_pop::DECIMAL / (urban_pop + rural_pop) * 100), 2)
       )
    ON CONFLICT (year) DO UPDATE SET
    urban_population = EXCLUDED.urban_population,
                              rural_population = EXCLUDED.rural_population,
                              urban_percentage = EXCLUDED.urban_percentage,
                              rural_percentage = EXCLUDED.rural_percentage;

COMMIT;
END;
$$ LANGUAGE plpgsql;
