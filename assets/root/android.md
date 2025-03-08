# Android API

The Android API is a Package of OpenTTY that improve experience of users in emulators at Android Phones.   


## Installing Android API

You can install **Android API** from **Yang** repository, or if you dont have a host of **OpenTTY Server** yet, you can run the command:  

```
execute install nano; tick Downloading...; wget raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/lib/android; install android; tick; get nano; echo [ OK ] Android API downloaded;
```

## How to host OpenTTY Server from your Phone

The **Android API** dont requires to root device, but you will need install the app `Termux` that can be installed from **Google Play Store** or from [**F-Droid**](https://f-droid.org/pt_BR/packages/com.termux/). It isnt required but some of features wont work as waited.  

Inside `Termux` you will need run this commands:  

```
pkg install python3 python3-pip git -y  
git clone https://github.com/mrlima4095/OpenTTY-J2ME.git  
echo -e '#!/bin/sh\ncd ~/OpenTTY-J2ME/assets\npython3 server.py' > ~/start.sh  
chmod +x ~/start.sh  
sh ~/start.sh   
```

It you start a host of **OpenTTY Server** directly from you Phone, if you already have this, skip this step!  
 
## Using the API