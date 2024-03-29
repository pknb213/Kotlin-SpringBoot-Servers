-- DROP TABLE IF EXISTS travel;
-- DROP TABLE IF EXISTS city;
-- DROP TABLE IF EXISTS statistic;
CREATE TABLE IF NOT EXISTS `user` -- user라는 keyword가 h2에서 reserved 되어 있음. ( `, " 추가 )
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    email varchar(50) NOT NULL UNIQUE,
    name varchar(50) NOT NULL,
    role varchar(20) DEFAULT 'ROLE_USER',
    verified BOOLEAN DEFAULT 0,
    password varchar(50) NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- INSERT INTO `user`(email, name, password) VALUES ('test', 'test', '1234');
CREATE TABLE IF NOT EXISTS city
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS travel
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name varchar(50) NOT NULL,
    city_id BIGINT NOT NULL,
    FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE RESTRICT,
    start_date timestamp,
    end_date timestamp,
--     INDEX IDX_CITY_ID(cit y_id, start_date, end_date),
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- CREATE INDEX IF NOT EXISTS `IDX_CITY_ID` ON `travel`(city_id, start_date, end_date);
CREATE TABLE IF NOT EXISTS statistic
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_id BIGINT NOT NULL,
    FOREIGN KEY (city_id) REFERENCES city(id),
    accessed_date timestamp DEFAULT CURRENT_TIMESTAMP
);
-- INSERT INTO city(name) VALUES ('Auto City');
-- INSERT INTO travel(name, city_id, start_date, end_date) VALUES ('Auto Travel', 1, '2022-11-09 09:00:00', '2022-11-12 18:00:00');
