New mathpump
============

`mathpump2` is an improved `mathpump`; the main improvement is the use of
[JavaFX](https://en.wikipedia.org/wiki/JavaFX)
instead of Swing. I also tried to simplify installation/configuration.

You should use the older [mathpump](https://github.com/amkhlv/mathpump)
if you cannot install Java 8 with JavaFX.

Introduction
============

**Mathpump** is a simplistic remote collaboration tool for mathematicians and other researchers. It could be particularly useful
for those researchers who tend to think by drawing pictures (theoretical physics). 

![Mathpump](docs/images/mathpump.png?raw=true)

Bob uses [Inkscape](http://inkscape.org/) to draw a picture, which is incrementally transmitted to the Alice's computer so she can look at it.
She answers by drawing her own picture, which is transmitted to Bob. Transmission happens every time the svg file is saved. 

The transmission requires a special server. That is, both Bob and Alice can have their machines under a firewall. But the server
(run by Charlie) has to have some ports open. We recomment that Charlie [buy a cheap VPS](http://lowendbox.com/). The server is the standard
[RabbitMQ](http://www.rabbitmq.com/). This README only explains how to setup the client (Alice and Bob). If you are 
Charlie, please read [docs/setup-server.md](docs/setup-server.md).

Wacom device
============

A [Wacom device](http://www.wacom.com/) is recommended. As they change rapidly, some present difficulties with Linux. See, for example,
[my writeup on CTL-480](docs/Wacom_ctl-480.md), similar steps should theoretically work also for other models.
It is useful to [map one of Wacom buttons](docs/Wacom_buttons.md) to `Save File` in Inkscape. 

Client setup
============

Building
--------

This manual is for Linux. The installation on Windows should be completely analogous.

You should have JDK of Java 8 and [JavaFX](https://en.wikipedia.org/wiki/JavaFX) installed on your computer.
If you install from Oracle, then JavaFX is already bundled.

With `OpenJDK` they are separate. For example, on Debian:

    aptitude install openjdk-8-jdk openjfx

Then execute the following commands:

    git clone https://github.com/amkhlv/mathpump2
    cd mathpump2
    ./activator assembly

This actually takes some time, depending on your Internet connection. Maybe 20 min or so. 

This will create the file: `target/scala-2.11/mathpump2-assembly-1.0.jar`


Configuration and running
=========================

See [example/README.md]

You will need three things from Charlie:

1. `trustStore` (a file)
2. password
3. truststore passphrase


Known bugs
==========

If you try to copy-paste a large bitmap image into your Inkscape window, this may crash the program.

Also, the loss of Internet connection will lead to crash.
In this case, the restart of the client program is needed (for both Alice and Bob).
