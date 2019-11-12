FROM treppo/alpine-leiningen:13 AS build

COPY . /opt/application
WORKDIR /opt/application
RUN lein do test, jlink assemble


FROM frolvlad/alpine-glibc:alpine-3.10

RUN apk add --update --no-cache libstdc++
COPY --from=build /opt/application/target/default/jlink /opt/cinema-ratings
WORKDIR /opt/cinema-ratings
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["bin/cinema-ratings"]