# ChiralDNS
Pure Java DNS server
# About
This is the initial upload of a pure Java DNS server.

This server implements the message and compression formats. However, it is old code, and doesn't even build in its current state. It does need to be modernized. The eventual goal is to turn this into a DNS proxy which can be used to filter DNS for ad removal, etc.

# To do
Many things to do, including:
* Make it asynchronous
* Modernized database
* Do proxying
* Run as a jsvc 
* Admin interface
