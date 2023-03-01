FROM centos:centos7

RUN yum update -y && \
    yum install -y ld-linux.so.2 wget gcc make postgresql-server perl perl-CPAN perl-Test-Simple perl-Digest-MD5 m4

ENV PGDATA /var/lib/pgsql/data
USER postgres
COPY --chown=postgres:postgres entrypoint.sh setup.sql /init/
RUN chmod 755 /init/entrypoint.sh
EXPOSE 5432
#RUN initdb --encoding=UNICODE
USER root
RUN echo "listen_addresses='*'" >> /var/lib/pgsql/data/postgresql.conf
RUN echo "host all all 0.0.0.0/0 md5" >> /var/lib/pgsql/data/pg_hba.conf
#USER postgres
#RUN pg_ctl start && \
#    psql -c "alter user postgres with encrypted password 'postgres';" && \
#    psql -c "create user root with password 'root' superuser;" && \
#    psql -c "create database lrb owner root;" && \
#    psql -d lrb -c "create schema authorization root;" && \
#    psql -c "create role readonly with login password 'readonly';" && \

#USER root
#RUN echo yes | perl -MCPAN -e upgrade && \
#    perl -MCPAN -e 'install DBI' && \
#    perl -MCPAN -e 'install DBD::PgPP' && \
#    perl -MCPAN -e 'install Math::Random'

WORKDIR /usr/local/MITSIMLab/data
RUN cd ../ && chmod 777 data && \
    wget http://www.cs.brandeis.edu/~linearroad/files/mitsim.tar.gz && \
    tar -zxvf ./mitsim.tar.gz
RUN echo "directoryforoutput=/usr/local/MITSIMLab/data" > mitsim.config && \
    echo "databasename=lrb" >> mitsim.config && \
    echo "databaseusername=root" >> mitsim.config && \
    echo "databasepassword=root" >> mitsim.config && \
    echo "numberofexpressways=2" >> mitsim.config

WORKDIR /usr/local/pvm3
RUN wget https://www.netlib.org/pvm3/pvm3.4.6.tgz && \
    tar -xzvf pvm3.4.6.tgz
ENV PVM_ROOT /usr/local/pvm3/pvm3
ENV PVM_ALLOW_ROOT yes
#RUN echo "export PVM_ROOT=/usr/local/pvm3/pvm3" >> ~/.profile && \
#    echo "export PVM_ALLOW_ROOT=yes" >> ~/.profile && \
#    source ~/.profile

WORKDIR /usr/local/pvm3/pvm3
RUN make

WORKDIR /usr/local/libc6.2.2.so.3
RUN wget https://archives.fedoraproject.org/pub/archive/fedora/linux/releases/16/Everything/x86_64/os/Packages/compat-libstdc++-296-2.96-143.1.i686.rpm && \
    yum localinstall -y compat-libstdc++-296-2.96-143.1.i686.rpm

WORKDIR /usr/local/MITSIMLab
ENTRYPOINT ["/init/entrypoint.sh"]
CMD ["/bin/bash"]
