3DSView (D3SView :) , aka 3D Secure WebView
===========================================

Self-contained UI component to process banking 3D Secure (MasterCard SecureCode / Verified By Visa) payment 
authorizations in Android apps.

Why exactly "D3S" ? Simply because Java does not allow to have number as a first character in a package and class names :)

Component have to be used instead of a WebView and handles the complete payment authorization process from redirecting user to an ACS banking server web UI and to grabbing authorization results and parameters, intercepting post events and parsing the code. 

Simply add it to your layout just instead of a WebView, invoke only two methods and then you have 3DS auth implemented.

Component can be used in activity, fragment or in any other part of your layout, both declaratively (in xml files) or programmatically by creating an instance in the source code.  Only make sure to give it sufficient space on the screen to display the banking ACS web page. 

Status
======

- Current stable version: [ ![Download](https://api.bintray.com/packages/livotovlabs/maven/3DSView/images/download.svg) ](https://bintray.com/livotovlabs/maven/3DSView/_latestVersion)
- Current development version: n/a

Get It
===

- Maven repository: jCenter
- Group: eu.livotov.labs.android
- Artifact ID: 3DSView

```groovy
implementation 'eu.livotov.labs.android:3DSView:x.y.z@aar'

```

What's new (1.1.2.9)
==========
- Latest pull requests merged
- Code reading

Installation
============

Release versions are available from jCenter repository, so just add the "implementation" statement to your project. For snapshots, please
add our bintray snapshots repository url first: https://dl.bintray.com/livotovlabs/maven

```groovy
dependencies {
    implementation 'eu.livotov.labs.android:3DSView:x.y.z@aar'
}
```

Alternatively you may download the source code and build it on your own.


Quick Usage
===========

1. Build your own or download precompiled 3dsview.jar from releases section and put it to the libs folder of your app project.
2. Add `eu.livotov.labs.android.d3s.D3SView` to your layout file (or create and add it programmatically)
3. In corresponding `Activity` or `Fragment`, configure the instance of `D3SView` by calling `DS3View#setAuthorizationListener(D3SViewAuthorizationListener)`. 
   - This adds a listener to receive authorization results and
progress messages. 
   - You will receive authorization MD and PaRes values there as well, when 3DSecure completes.
4. Invoke the `D3SView#authorize(String, String, String)` method by passing MD, PaReq and ACS url values, you receive from your card payment gateway and listen for authorization completion event in your callback. 
   - Specifying postback url is optional but recommended, the library will use a sensible default value if not set.
5. Once user completes the authorization at the ACS server, your callback method will be automatically called with the 3DS response data, which you may then pass to your processing backend server for payment finalization.

Bugs, Suggestions, Ideas
========================
Any ideas/bugs/etc, as well as pull requests, are welcome in the [issues section](https://github.com/LivotovLabs/3DSView/issues).

Credits
=======
Alex Askerov (@askerov), Mia Alexiou (@subsymbolic), Luke Korth (@lkorth), Christophe Beyls (@cbeyls), Owen O Byrne (@owenobyrne)
