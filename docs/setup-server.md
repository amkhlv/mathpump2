
Installing RabbitMQ
===================

On Debian:

    aptitude install rabbitmq-server

It will install and start immediately. To stop it, type:

    /etc/init.d/rabbitmq-server stop

First of all, let us change the default user's password. The default user is called `guest`.
To change the password, first have to start the server:

    /etc/init.d/rabbitmq-server start

and then say:

    rabbitmqctl change_password guest VGIWB0CEWwDW

where `VGIWB0CEWwDW` is the new password. 

Configuring RabbitMQ
====================

Keys and Certificates
---------------------

There is a very good guide on the [RabbitMQ website](http://www.rabbitmq.com/ssl.html), just follow the 
instructions in the section called "Keys, Certificates and CA Certificates". You do not need the
client part, **only the server part**. 

The only thing I have to add to those instructions, is the following.  The RabbitMQ server runs under the user `rabbitmq`
(which was  automatically created by the ``aptitude`` when you installed RabbitMQ).
All these files which you created
in ``testca/`` and ``server/`` should be **readable** by the ``rabbitmq`` user:

    chown -R root:rabbitmq testca server

and set the 750 permission:

    drwxr-x--- 2 root rabbitmq 4.0K Aug 20 15:11 client
    drwxr-x--- 2 root rabbitmq 4.0K Aug 20 15:02 server
    drwxr-x--- 4 root rabbitmq 4.0K Aug 20 15:11 testca

On my server, I created ``/var/rabbitmq`` and put both ``testca/`` and ``server/`` as its subdirectories, so I have:

    /var/rabbitmq/testca/
    /var/rabbitmq/server/

Email the server certificate to Alice and Bob
--------------------------------------------

The folder ``server/`` contains a file `cert.pem`. Execute the following command:

    keytool -import -alias server1 -file cert.pem -keystore trustStore

You will be asked for a passphrase. Choose some passphrase and give it to Alice and Bob. You should
also send them the newly created file `trustStore`.


Enabling SSL Support in RabbitMQ
--------------------------------

This, again, is explained on the [RabbitMQ website](http://www.rabbitmq.com/ssl.html), in the section
called "Enabling SSL Support in RabbitMQ". Basically, all you have to do is (as root):

```
echo '[
  {rabbit, [
     {ssl_listeners, [5671]},
     {ssl_options, [{cacertfile,"/path/to/testca/cacert.pem"},
                    {certfile,"/path/to/server/cert.pem"},
                    {keyfile,"/path/to/server/key.pem"},
                    {verify,verify_peer},
                    {fail_if_no_peer_cert,false}]}
   ]}
].' >>    /etc/rabbitmq/rabbitmq.config

```

**and then replace** ``/path/to/testca`` and ``/path/to/server`` with the path to those two directories which you
created in the previous subsection, when setting up keys.

Opening the port
----------------

As you have noticed, we have configured the SSL-enabled RabbitMQ to listen on Port 5671.
Obviously, we have to open that port:

    iptables -A INPUT -p tcp --dport 5671 -j ACCEPT
    /etc/init.d/iptables-persistent save

Registering users
=================

To add users `alice` and `bob` and allow them to talk over the vhost ("Virtual Host") `ourtheorem` :

    rabbitmqctl add_vhost ourtheorem
    rabbitmqctl add_user alice sarrpuOSX1Nm 
    rabbitmqctl set_permissions -p ourtheorem alice '^(alice|bob)$' '^amq\.default$' '^(alice|bob)$'
    rabbitmqctl add_user bob eij3yataeJoh
    rabbitmqctl set_permissions -p ourtheorem bob '^(alice|bob)$' '^amq\.default$' '^(alice|bob)$'

where `sarrpuOSX1Nm` and `eij3yataeJoh` are their passwords. Notice that `ourtheorem` is the name of the Virtual Host,
which should match the line `<vhost>ourtheorem</vhost>` in the client configuration files `conf.xml` for both Alice
and Bob. 

Also notice that `amq.default` is the name reserved in RabbitMQ for the default exchange. 
See [the manual](https://www.rabbitmq.com/access-control.html) for details about the access control structure.

See also [the manual for rabbitmqctl](https://www.rabbitmq.com/man/rabbitmqctl.1.man.html).

Web console
===========

I want one of the users to be the administrator for the purpose of Web Console
management. Let it be Alice:

    rabbitmqctl set_user_tags alice administrator

Then I want to enable the Web Console plugin:

    rabbitmq-plugins enable rabbitmq_management

Moreover, I want to run the Web Console under SSL. For that, I have to edit the file ``/etc/rabbitmq/rabbitmq.conf``
to contain the following lines:

    [
      {rabbit, [
         {ssl_listeners, [5671]},
         {ssl_options, [{cacertfile,"/var/rabbitmq/testca/cacert.pem"},
                        {certfile,"/var/rabbitmq/server/cert.pem"},
                        {keyfile,"/var/rabbitmq/server/key.pem"},
                        {verify,verify_peer},
                        {fail_if_no_peer_cert,false}]}
       ]},
      {rabbitmq_management, 
            [ 
                    {http_log_dir, "/tmp/rabbit-mgmt"},
                    {listener, [
                            {port, 15672}, 
                            {ssl, true},
                            {ssl_opts, [{cacertfile,"/var/rabbitmq/testca/cacert.pem"},
                                        {certfile,"/var/rabbitmq/server/cert.pem"},
                                        {keyfile,"/var/rabbitmq/server/key.pem"}
                                       ]}
                               ]}
            ]}
    ].

Troubleshooting
===============

Log files are in ``/var/log/rabbitmq``


