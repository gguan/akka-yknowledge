#Akka Importer - Knowledge Graph

Neo4J is wired(no drivers, using rest api as library). Mule is too heavy and not very flexible. Java is tedious. So I start this project.

- The overall system is based on [Akka(Scala)](http://akka.io/).
- It use Akka-Camel module as endpoint component to monitor input file source and connect to [ActiveMQ](activemq.apache.org) to fetch queued messages.
- Database can be either [Titan-Cassandra](http://thinkaurelius.github.io/titan/) or [Neo4J](http://neo4j.org).

## Integrate Typesafe Console 

[link: http://localhost:9900](http://localhost:9900)

![alt text](https://git.corp.yahoo.com/github-enterprise-assets/0000/1605/0000/5921/0913cbc2-2636-11e3-9c77-58e191db5048.png "Console")


## Spray as Web Service

http://localhost:8080/data/{id}

http://localhost:8080/search/{query} (ex. Entity_alternateKey:Wikipedia_EN::Cham-e_Hashem)


## Prerequsite 

1. Install and run ActiveMQ 
2. Setup a Cassandra server
3. Install SBT 
4. run / atmos:run (with Typesafe Console)

