# Lab #3 - Simple TCP server/client Scala
----

## Usage

```bash
$ sbt compile
$ sbt "runMain -m server -p 9000 -n 50" # start server
$ sbt "runMain -m client --host localhost -p 9000" # start client
$ sbt "runMain" # see help
```

## In docker

```bash
$ docker build -t maximskripnik/lab3 .
$ docker run --rm --net host maximskripnik/lab3 -m server -p 9000 # start server
$ docker run -ti --rm --net host maximskripnik/lab3 -m client --host localhost -p 9000 # start client
```