# This page is still under development


# CouchMovies

This is a sample project shows how to build a search feature using Bleve/Couchbase FTS

# Demo
TBD

## Intall (Docker)
pull docker couchbase

./startLocal couchbase couchmovies

echo "Starting Couchbase standalone docker image '$1'  with name '$2'"
docker run -d --name $2 -p 8091-8096:8091-8096 -p 11210-11211:11210-11211 -v ~/Documents/demos/Docker/local/files:/mac $1
docker exec -it $2 bash 

cd /mac
./installTools

#!/bin/bash

apt-get update
apt-get -y  install vim jq zip unzip git

echo COLUMNS=300 >> ~/.bashrc
echo export PS1='"\[\033[36m\]\u\[\033[m\]@\[\033[32m\]container:\[\033[33;1m\]\w\[\033[m\] $ "' >> ~/.bashrc
echo export CLICOLOR=1 >> ~/.bashrc
echo export LSCOLORS=ExFxBxDxCxegedabagacad >> ~/.bashrc
echo export TERM=xterm >> ~/.bashrc

echo "set nocompatible" > ~/.vimrc
echo "set backspace=2" >> ~/.vimrc
echo "set formatoptions-=r" >> ~/.vimrc
echo "set formatoptions-=o" >> ~/.vimrc

useradd -m -p $(openssl passwd -1 demo) demo
su demo

cd
git clone https://github.com/escapedcanadian/couchmovies

cd couchmovies/build
<tweek env>
. .env
  
  

## How to run this project

1) Download and Install Couchbase https://www.couchbase.com/downloads
2) Unzip the file datasets.zip under the "data" folder
3) Go to the "bin" directory of you Couchbase installation( on mac: "/Applications/Couchbase\ Server.app/Contents/Resources/couchbase-core/bin/")
4) Load the movies dataset with the following command:
```
./cbimport json -c couchbase://127.0.0.1 -u Administrator -p password -b movies -d file:///Users/deniswsrosa/Desktop/FTS/the-movies-dataset/cb-movies-dataset2.json  -f list -g key::%id% -t 4
```

5) Load the actors dataset with the following command:
```
./cbimport json -c couchbase://127.0.0.1 -u Administrator -p password -b movies -d file:///Users/deniswsrosa/Desktop/FTS/the-movies-dataset/cb-movies-actors.json  -f list -g %id% -t 4 -v
```
6) Create index with
```
curl -XPUT -H "Content-type:application/json" http://<USER>:<PASSWORD>@<IP_ADDRESSES>:8094/api/index/movies_shingle -d @movies_shingle.json
```

7) Run the following command on the root folder of this project:
```
mvn clean install
```

8) Then run this command to start de application:
```
mvn spring-boot:run
```

9) Copy the content of the "front" folder in a web server (Ex: NGINX) and access the couchflix.html

10) OPTIONAL: If you want to enable the image cover (the image that appears when you click over a movie) you will need to install a chrome driver:
```
brew cask install chromedriver //on mac
```

And then update the path to your chrome driver in the class "ImageService.java"
