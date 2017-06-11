#!/bin/sh

echo "Compile..."
javac -cp gson-2.6.2.jar:. Parser.java 

echo "Run..."
java -cp gson-2.6.2.jar:. Parser 

echo "Clean..."
rm -Rf *.class
