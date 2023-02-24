alter user postgres with encrypted password 'postgres';

create user root with password 'root' superuser;

create database lrb owner root;
\c lrb

create schema authorization root;
\c postgres

create role readonly with login password 'readonly';
