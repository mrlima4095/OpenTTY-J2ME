OpenTTY Support scripts

The support scripts are tools to you run in 
another device, it is utilies that add things that cant be
implemented directly on J2ME 'cause it is a restrict envirroment


Download Script

    python download.py <ip> <port> [filename]
 
    This script connect with the OpenTTY server at
J2ME and download the response of server, its used for
transfer nano text or any texts saved in MIDlet storage


Send file Script

    python transfer.py <port> [filename] 

    It starts a localhost HTTP server to you send a file
to OpenTTY at you J2ME phone. You can download using wget 
tool, try `wget <ip>:<port>` to save into nano.


Localhost Script

    python localhost.py [port]

    It start a localhost server, it stream index.html
if file doesnt exists it show directory listing, as root
in script execiting directory.

Remote acess Script

    python remote.py 
 
    Start a remote shell in localhost at port 4095
it execute recived commands in System command processor
and stream stdout output.




