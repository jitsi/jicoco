## Configuration Options

Configuration file: sip-communicator.properties<br>
Location of file depends on your installation type, possible locations are /HOME/.sip-communicator and /etc/jitsi

Connection Options
------------------
Pings are sent to determine whether or not a valid connection still exists. There are several configuration
properties you can set to fine tune this behavior.

The name of the property which configures ping interval in ms. -1 to disable pings.<br>
property: **org.jitsi.*componentName*.PING_INTERVAL**<br>
default: 10000 ms

The name of the property used to configure ping timeout in ms.<br>
property: **org.jitsi.*componentName*.PING_TIMEOUT**<br>
default: 5000 ms

The name of the property which configures {@link #pingThreshold}.<br>
property: **org.jitsi.*componentName*.PING_THRESHOLD**<br>
default: 3

The name of the property used to determine if we should reconnect on ping failures. Setting this value to true will
force the connection to be reset when the ping threshold is reached<br>
property: **org.jitsi.*componentName*.RECONNECT_ON_PING_FAILURES**<br>
default: false

