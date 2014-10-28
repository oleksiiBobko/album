--create database album before run this script
USE album;
create table USERS (
ID INT NOT NULL AUTO_INCREMENT,
LOGIN VARCHAR(100) NOT NULL,
PASS VARCHAR(100) NOT NULL,
ROLE VARCHAR(100) NOT NULL,
ACTIVE BOOLEAN NOT NULL DEFAULT 1,
PRIMARY KEY (ID)
) ENGINE=InnoDB;

create table PICTURES (
ID INT AUTO_INCREMENT,
PIC_OWNER VARCHAR(100) NOT NULL,
FILENAME VARCHAR(100) NOT NULL,
DESCRIPTION VARCHAR(100),
PATH VARCHAR(100),
THUMBNAIL VARCHAR(100) NOT NULL,
CREATED TIMESTAMP NOT NULL,
USER_ID INT NOT NULL,
PRIMARY KEY (ID),
FOREIGN KEY (USER_ID)
REFERENCES USERS(ID)
ON DELETE CASCADE 
) ENGINE=InnoDB;
