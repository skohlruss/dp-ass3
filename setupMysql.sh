#!/bin/bash

mysql -e "CREATE DATABASE IF NOT EXISTS experiment;"
mysql -e "CREATE TABLE IF NOT EXISTS raw (\`year\` VARCHAR(4), \`country\` VARCHAR(255), \`articles\` DOUBLE DEFAULT NULL, \`population\` DOUBLE DEFAULT NULL);" experiment
mysql -e "CREATE TABLE IF NOT EXISTS result(\`year\` VARCHAR(4), \`articles_per_capita\` DOUBLE);" experiment
