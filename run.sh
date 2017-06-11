#!/bin/sh

echo "Starting Mysql..."
service mysql start
echo "Setup DB"
./setupMysql.sh

echo "Compile..."
javac -cp lib/*:. Parser.java 
echo "Run..."
java -cp lib/*:. Parser 

echo "Clean..."
rm -Rf *.class
