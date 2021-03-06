
Completing the configuration
============================

You are now in the `example/` folder

Missing files
-------------

    alice2bob/trustStore
    bob2alice/trustStore

you should obtain them from Charlie


Missing settings
----------------

In `alice2bob/settings.conf` and `bob2alice/settings.conf`, the actual values of `password` and `trustStorePassphrase` should be the ones provided by Charlie


Running
=======

Obviously the profiles `alice2bob` and `bob2alice` are meant to be run on two different computers. However, they may be run on the same
computer for testing.

On Linux, go to `alice2bob` and say:

    ./start.sh  /path/to/your/mathpump2/target/scala-2.11/mathpump2-assembly-1.0.jar

(this script assumes that `inkscape` is installed on your system)

After initialization, a small "control" window with a `start` button should appear. Once you press the `start` button,
you get a big window for showing bob's formulas. This big window will probably cover the previous small window,
which now contains a `stop` button. When you want to stop `mathpump`, just dig up that "control" window and press `stop`. 

Whenever you draw something in the Inkscape and save, it will be transmitted and the other party will see it.
The transmission is *incremental* in the following sense: every time you hit the Inkscape's `save` button, what is actually
transmitted is the *difference* (the diff patch) between the saved `svg` file and what you have previously saved. The patch
is then applied on the other other end. This speeds up the transmission. 
