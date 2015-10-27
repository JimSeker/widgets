Andriod home screen widget Examples
===========

eclipse/ has the examples in eclipse project format, no longer updated.  Otherwise the examples are for android studio.

<b>widgetDemo</b>  provides a "simple" demo of a homescreen widget that displays a random number.  The number is configured
in the configActivity.  It uses a sharedpreference so each instance of the widget can use a different number.  The preferences are
managed in the static methods in the ConfigActivity.  There is a launcher activity, but does nothing, it just so you know the app has
been installed on the phone.  In a real app, the launcher app would not exist.

<b>widgetDemo2</b> is the same as widgetDemo, except there are no static methods calls between the configActivity and the widget.
It maybe easier to follow what is going on.

<b>widgetDemo3</b> an extension of widgetDemo, with an extra "button" that calls the confActivity, so the user can change the max
number for the random number.

<b>widgetDemoButtons</b> shows how the homescreen widget can have different buttons via a receiver, so it can do more interesting/complex things.

<b>TapWidget</b> is a demo of wideget, service, networking and pull it all together to basically connect two devices via wifi and send messages
that vibrate the other phone.   It is a work in progress and still has some issues.

These are example code for University of Wyoming, Cosc 4730 Mobile Programming course.
All examples are for Android.
