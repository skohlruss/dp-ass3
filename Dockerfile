FROM ubuntu:16.10

# add resources
ADD . /usr/local/dp-ass3

# initialize
WORKDIR /usr/local/dp-ass3
RUN apt-get update && apt-get install -y openjdk-8-jdk mariadb-server

# start work
CMD ./run.sh

