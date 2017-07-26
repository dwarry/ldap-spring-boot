# ldap-spring-boot

This application can run standalone or can be deployed to various PAAS solutions (e.g. to PCF) and can be connected to LDAP in order to enable users to change their password

In order to connect it against LDAP you need to set some environment variables:

* url is the url to the LDAP istallation including the port, default is "localhost:3890" 
* cnsuffix will be added to the username, add the parameters there you need to find the entry in your directory default is ", ou=users,dc=example,dc=com" which leads to "cn=test, ou=users,dc=example,dc=com" for a user test 
* ssl use ssl or not, default is "false"
* basename default is dc=example,dc=com
    
some words about LDAP:

The Lightweight Directory Access Protocol (LDAP) is a directory service protocol that runs on a layer above the TCP/IP stack. It provides a mechanism used to connect to, search, and modify Internet directories. The LDAP directory service is based on a client-server model.

some terms you should know:

* Domain Component (DC)
* Organisational Unit (OU) 
* Distinguished Name (DN) - full identification path including relative path/domain, e.g. "dn: cn=John Doe,dc=example,dc=com" means id John which is under com.example

## Test app locally

It is up to you how to test it, I use https://github.com/gschueler/vagrant-rundeck-ldap for a local LDAP installation (it uses OpenLDAP), apache director studio (http://directory.apache.org/studio/) as LDAP editor and run the app locally (gradlew bootRun) or I use PCF Dev (https://pivotal.io/platform/pcf-tutorials/getting-started-with-pivotal-cloud-foundry-dev/introduction). This hints are referring to this.

prepare:

* in https://github.com/gschueler/vagrant-rundeck-ldap execute vagrant up
* install apache director editor
* connect to ldap in vagrant box via editor (localhost:3890; bind:cn=deploy, ou=users,dc=example,dc=com
* test the box and the users, you can play with this users e.g. 'cn=deploy,ou=users,dc=example,dc=com with password deploy' or https://github.com/gschueler/vagrant-rundeck-ldap/blob/master/example-jaas-ldap.conf or https://github.com/gschueler/vagrant-rundeck-ldap/blob/master/default.ldif

optional - prepare:

* optional if you want to use PCF - download PCF DEV https://github.com/gschueler/vagrant-rundeck-ldap execute vagrant up
* optional if you want to use PCF - install PCF cli (https://pivotal.io/platform/pcf-tutorials/getting-started-with-pivotal-cloud-foundry-dev/install-the-cf-cli)

1.build the app:

    gradlew clean build

2.a test locally: 

    gradlew bootRun
    # open localhost:8080
    # chanmge password for user deploy (password is deploy)

2.b optional - test with local PCF:  

    gradlew build
    cf login -a https://api.local.pcfdev.io --skip-ssl-validation
    # Email: user
    # Password: pass
    cf push -p .\build\libs\ldap-spring-boot-0.0.1.jar ldap-password
    # set taget to host (check ifconfig)
    # e.g. 
    cf set-env ldap-password URL 192.168.11.1:3890
    cf restart ldap-password 
    # open https://ldap-password.local.pcfdev.io/
    # 
    # Apps Manager URL: https://local.pcfdev.io
    # Admin user => Email: admin / Password: admin
    # Regular user => Email: user / Password: pass



