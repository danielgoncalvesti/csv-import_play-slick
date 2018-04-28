# --- !Ups

create table PEDIDO(ID BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY, RA varchar(255) NOT NULL,NOME varchar(255) NOT NULL, PEDIDO varchar(255) NOT NULL, STATUS varchar(255) NOT NULL);

# --- !Downs

drop table PEDIDO;

