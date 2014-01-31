Flume Kestrel Plugin
====================

## Overview
This plugins provides a Sink to be able to send events from Cloudera's Flume v0.9.4 to a Kestrel server.

## How to build
Download and extract the latest version of [apache flume](http://www.apache.org/dyn/closer.cgi/flume/1.4.0/apache-flume-1.4.0-bin.tar.gz)

Add a link to the root of the flume binary distribution
>   ln -s /localdisk/apache-flume-1.4.0-bin

Build the sources using ant
>   ant clean && ant

