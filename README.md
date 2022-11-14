# Sony Braiva TV Binding

This binding allows you to send commands to and receive status from Sony Bravia TVs. You can trigger scenes off status changes, or have scenes change the status of the TV. For example, you can trigger a TV light scene when the TV is turned on, or you can turn on and off a subwoofer (mine is a huge power vampire).  This binding also allows you to control the TV power via alexa, if you have alexa configured for your openhab installation.

See below for a list of supported channels. 

## Supported Things

Each TV that you want to control is represented as an individual thing. 

Tested on a XBR-65X930S, but should work on all modern Sony Bravia TVs. 


## Discovery

No auto discovery is currently supported. You have to know and enter the IP address of your TV.


## Thing Configuration

The following configuration is required to set up a TV to be controlled by this binding:

**IP Address**: This is the IP address of the TV. You can find this under the "Network setting" section of you TV.

**Pre Shared Key**: This is the pre shared key (PSK) for your TV. The PSK is like a password for your TV. To set up PSK on your tv, go to *Setting* -> *Network* -> *Home network* -> *IP control* -> *Pre-Shared key*.  Enter a random string or character on the TV, and enter the same string in the setting for this thing. Don't use "your favorite password", as the PSK is not encrypted and can be seen in clear by going back to this section on in your TV settings.

**Pull interval**: This is the number of milliseconds between each time the binding will pull for status updates from the TV. If you are gonig to trigger scenes from the status of the thing, you should have this relatively low. The default value is 8000[ms], and works well for triggering scenes. 

## Channels

Currently this binding on supports a single channel: **power**, and it's a switch that represents whether the TV is turned on or off.

## Example

*Example item for the power channel:*

```
Switch FR_TV "Family room TV" { channel = "sonybravia:tv:d5ed6ce2:power" }
```

