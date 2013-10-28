3DSView (D3SView :) , aka 3D Secure WebView
===================

Self-contained UI component to process banking 3D Secure (MasterCard SecureCode / Verified By Visa) payment 
authorizations in Android apps.

Why D3S ? Simply because Java does not allow to have package and class names beginning with the digit, so we simply swapped
first two letters in source code :)

It handles the complete process from redirecting user to an ACS banking server, displaying web interface for payment 
authorization and "catching" authorization results and parameters, so you don't need to dig into WebView internals,
intercept post events on your own and parse html code. Simply add it to your layout, invoke only two methods and then
you have 3DS implemented in your app.

Component can be used in activity, fragment or in any other part of your layout. Only ensure you gave it sufficient
space on the screen to display the banking ACS page to the user. Just add it to your layout, set parameters and
authorization callbacks and invoke the performAuthorization(...) method.



Quick Usage
===========

- Build your own or download precompiled 3dsview.jar from releases section and put it to the libs folder of your app project.
- Add eu.livotov.labs.android.d3s.D3SView to your layout
- In corresponding activity or fragment, configure the instance of D3SView by calling setXXX methods (see configuration options below)
- Invoke the authorize(...) method, providing "dummy" postback url (optionally), callback listener and 3DS initiation parameters.

Once user completes the authorization, your callback method will be called with the 3DS response data, which you may
pass to your processing backend server for payment finalization.


Configuration
=============

- d3sview.setDebug(true|false) - enables or disables debug mode. In debug mode, self-signed or broken certificates/ssl
 errors will be ignored. It is important not to enable debug mode for release apps.
- d3sview.setAuthorizationListener(D3SViewAuthorizationListener) - adds listener to receive authorization results and
progress messages. You will receive authorization MD and PaRes values there as well, when 3DSecure completes.


Start
=====

Simply call d3sview.authorize(...) method and pass MD, PaReq and ACS url values, you receive from your card payment gateway.
And then listen for authorization completion events.


Bugs, Suggestions, Ideas
========================
Any ideas/bugs/etc as well as pull requests are welcome into the issues section.

