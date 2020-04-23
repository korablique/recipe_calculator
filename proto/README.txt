Protoc of 3.11.4 is currently used.
Download Protoc binary here: https://github.com/protocolbuffers/protobuf/releases
Then install the binary accordingly to the README file provided with it.

To generate Java-classes from *.proto files:
$ cd <project root folder>
$ rm app/src/main/java/korablique/recipecalculator/model/proto/* 
$ protoc -I=. --java_out=./app/src/main/java/ ./proto/*.proto