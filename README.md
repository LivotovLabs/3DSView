3DSView (D3SView :) , aka 3D Secure WebView
===================

Self-contained UI component to process banking 3D Secure (MasterCard SecureCode / Verified By Visa) payment 
authorizations in Android apps.

Why exactly "D3S" ? Simply because Java does not allow to have number as a first character in a package and class names :)

Component have to be used instead of a WebView and handles the complete payment authorization process from redirecting user to an ACS banking server web UI and to grabbing authorization results and parameters, intercepting post events and parsing the code. 

Simply add it to your layout just instead of a WebView, invoke only two methods and then you have 3DS auth implemented.

Component can be used in activity, fragment or in any other part of your layout, both declaratively (in xml files) or programmatically by creating an instance in the source code.  Only make sure to give it sufficient space on the screen to display the banking ACS web page. 


Status
======

- Current stable version: 1.0.0
- Current development version: 1.1.0-SNAPSHOT


What's new (1.1.0)
==========
- Added D3SDialog as DialogFragment, so you can perform 3DS authorization using a nice dialog above your business activity



Installation
============
Current compiled snapshots and releases could be automatically added to your project using gradle from our maven repository http://maven.livotovlabs.pro

Add link to our repository to your gradle build file (repositories section):

``
    maven { url 'http://maven.livotovlabs.pro/content/groups/public' }
``

Then append appropriate dependency to your "compile" statement:

- For release version:
``
dependencies {
    ...
    compile 'labs.livotov.eu:3dsview:1.0.0'
    ...
}
``

- For snapshots:
``
    configurations.all {
        // check for updates every build, so you will pick up latest snapshot, even if it was refreshed a minute ago.
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }

    dependencies {
        ...
        compile group: "eu.livotov.labs", name: "3dsview", version: "1.1.0-SNAPSHOT", ext: "aar", changing: true
        ...
    }
``
Alternatively, you may download the source code and build it on your own.


Quick Usage
===========

- Build your own or download precompiled 3dsview.jar from releases section and put it to the libs folder of your app project.
- Add eu.livotov.labs.android.d3s.D3SView to your layout file (or create and add it programmatically)
- In corresponding activity or fragment, configure the instance of D3SView by calling setXXX methods (see configuration options below)
- Invoke the authorize(...) method, providing "dummy" postback url (optionally), callback listener and 3DS initiation parameters.

Once user completes the authorization at the ACS server, your callback method will be automatically called with the 3DS response data, which you may then pass to your processing backend server for payment finalization.


Configuration
=============

- d3sview.setDebug(true|false) - enables or disables debug mode. In debug mode, self-signed or broken certificates/ssl
 errors will be ignored. It is important not to enable debug mode for release apps.
- d3sview.setAuthorizationListener(D3SViewAuthorizationListener) - adds listener to receive authorization results and
progress messages. You will receive authorization MD and PaRes values there as well, when 3DSecure completes.


Start
=====

Simply call d3sview.authorize(...) method and pass MD, PaReq and ACS url values, you receive from your card payment gateway and listen for authorization completion event in your callback. 

P.S. Specifying postback url is optional, the library will use the default value then.


Roadmap
=======

- Add gradle and maven repo support, so library can be automatically downloaded and used in the projects.


Bugs, Suggestions, Ideas
========================
Any ideas/bugs/etc as well as pull requests are welcome into the issues section.

