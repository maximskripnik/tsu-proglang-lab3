FROM openjdk:8

ARG SBT_VERSION=1.3.7

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  sbt sbtVersion

WORKDIR /opt/app

COPY project /opt/app/project
COPY build.sbt /opt/app/build.sbt
COPY src /opt/app/src

RUN sbt assembly

ENTRYPOINT [ "java", "-jar", "target/scala-2.13/lab3.jar" ]