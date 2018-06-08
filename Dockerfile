FROM anapsix/alpine-java:9_jdk AS base

RUN apk add --update --no-cache wget ca-certificates bash && \
    wget -q "https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein" \
         -O /usr/local/bin/lein && \
    chmod 0755 /usr/local/bin/lein && \
    apk del wget ca-certificates && \
    rm -rf /tmp/* /var/cache/apk/*

COPY . /opt/application
WORKDIR /opt/application
RUN lein do midje, jlink assemble


FROM frolvlad/alpine-glibc

RUN apk add --no-cache libstdc++
COPY --from=base /opt/application/target/default/jlink /opt/yorck-ratings
CMD /opt/yorck-ratings/bin/yorck-ratings

