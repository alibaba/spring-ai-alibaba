create table TBL_USER (
                          id int not null auto_increment,
                          username varchar(255) not null,
                          email varchar(255) not null,
                          password varchar(255) not null,
                          primary key (id)
);

create table TBL_ACCOUNT (
                             id int not null auto_increment,
                             accountNumber varchar(255) not null,
                             user_id int not null,
                             balance decimal(10, 2) not null,
                             openDate date not null,
                             primary key (id),
                             foreign key (user_id) references TBL_USER(id)
);
