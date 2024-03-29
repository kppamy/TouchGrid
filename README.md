TouchGrid
=========

A test app to check the touch screen sensor. 
The main reason for making this app is to verify the deadzone problem with my asus transformer prime, where the left portion of the screen does not capture any touch event at all. Not noticible when use in landscape mode, but when put in potrait mode, the menu/action bar can not be 'click' at all.

Basic idea:
- Draw a mxn grid on screen
- Capture the touch event
- Support multitouch
- Mark a grid block when touched
- Draw the rect to represent the touch box

Additional:
- Show the press size
- Show pressure
- Make the grid overlay?
- Make block size configurable?

Update 2012/11/16:
Pending issue:
- State is not saved when coming back from pause 

Resolved:
- Crash on exit
  * canvas is null when thread still calling ondraw.
  
- Crash on orientation change. Need to add matrix transformation.
  * Avoided by supporting only portrait screen orientation

- drawing is flickering while touch
  * Commonly known issue with SurfaceView. Solved it based on ref [3]
  
Reference:
- [1] http://www.rbgrn.net/content/367-source-code-to-multitouch-visible-test
- [2] http://stackoverflow.com/questions/10956583/android-draw-using-surfaceview-and-thread
- [3] <Workaround for screen flicker>
http://android-coding.blogspot.sg/2012/01/flickering-problems-due-to-double.html

