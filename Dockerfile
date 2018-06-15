FROM treppo/alpine-jdk9-leiningen AS build

COPY . /opt/application
WORKDIR /opt/application
RUN lein do midje, jlink assemble


FROM frolvlad/alpine-glibc

RUN apk add --no-cache libstdc++
COPY --from=build /opt/application/target/default/jlink /opt/yorck-ratings
WORKDIR /opt/yorck-ratings
ENTRYPOINT ["/bin/sh", "-c"]
CMD bin/yorck-ratings
