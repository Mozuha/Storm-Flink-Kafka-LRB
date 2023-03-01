#!/usr/bin/env bash

if [ -z "$(ls -A $PGDATA)" ]; then
	initdb -E UTF8 --locale=C --encoding=UNICODE
	pg_ctl -w start
	psql -f /init/setup.sql
fi

exec "$@"
