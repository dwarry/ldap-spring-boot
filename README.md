# ldap-spring-boot

This application can be deployed (e.g. to PCF) and can be connected to LDAP in order to enable useres to change their password

In order to connect it against LDAP you need to set some environment variables:

* providerUrl is the url to the LDAP istallation including the port default is "localhost:3890" 
* cnsuffix will be added to the username, add the parameters there you need to find the entry in your directory default is ", ou=users,dc=example,dc=com" which leads to "cn=test, ou=users,dc=example,dc=com" for a user test 
* ssl use ssl or not, default is "false"
    
some words about LDAP:

The Lightweight Directory Access Protocol (LDAP) is a directory service protocol that runs on a layer above the TCP/IP stack. It provides a mechanism used to connect to, search, and modify Internet directories. The LDAP directory service is based on a client-server model.

some terms you should know:

* Domain Component (DC)
* Organisational Unit (OU) 
* Distinguished Name (DN) - full identification path including relative path/domain, e.g. "dn: cn=John Doe,dc=example,dc=com" means id John which is under com.example


    