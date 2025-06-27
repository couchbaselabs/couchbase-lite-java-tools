# Parthian Shots.

## I have noticed that Java / Android doesn’t have PR validation. What is the development process from changing the code to PR to after PR?

There is no check on either the common or the ce repos.  There *is*, however, a check
on the root.  Jenkins runs the gradle `smokeTest` target on a root PR
That must pass with no errors before the the PR can be merged.

My practice is to make sure that both smokeTest target and the dev tests

    caffeinate -i -s py3 etc/run_tests.py -v 4_0 -j -a)
 ... run clean, before I push a PR.


## What are the kotlin extension development and build process?

There are separate repositories, ce/android-kotlin and ee/android-kotllin for the kotlin extensions.
The depend on finding the relevant android platform.

- on local build they will try to build android first, if they can
- there is code in `repositories` section of the kotlin build.gradle that looks around
  for an appropriate android.  For a local build it uses maven local: you should have
  built the android platform with the devPublish target.
  For normal jenkins builds it looks in our internal maven server.
  For release validation (the Jenkins jobs for that have not yet been created) it looks in the public maven

Also note the `test` subdirectories.  The gradle property `automatedTests` causes
the test subdirectory, instead of the lib subdirectory to be built.  It references sources
*ONLY* for the tests: the platform gets pulled from the maven server.  You will not use that
build process very often during development development.  It is what Jenkins does.

You can define some variables in ~/.gradle/gradle.properties, to make local development
work:

    ## ~/.gradle/gradle.properties
    # Get plenty of space
    org.gradle.jvmargs=-Xmx4096m -Xms2048m -XX:ThreadStackSize=4096 -XX:CompilerThreadStackSize=4096
    
    # Tests that should not be run
    #testFilter=com.couchbase.lite.utils.ReplicatorIntegrationTest
    
    # Local extlib to use in JAK VS tests
    #javaExtLib=2.0.0-2
    #androidExtLib=2.0.0-2
    
    # Version of CBL under test by the E2E test system
    cblVersion=3.3.0-30
    datasetVersion=4.0
    
## What is your thought process whether the API should also be implemented in Kotlin language along side with the Java language?

I don't really think that would make any sense.  Java is completely interoperable with Kotlin
and vice versa.  Wrapping things in Kotlin idioms (Builders => constructors with named args,
Callabacks -> Flows, etc), in the extension package, when you can improve the API make it more
idiomatic seems entirely sufficient to me.

## Any legacy code that should be upgraded when we can bump Java version or Android version?
Yes.  My Cleaner should go away ASAP.  It has an API that is very similar to the Java Cleaner,
so jacking up C4Peer and putting the real Cleaner under it should be straightforward.

There is also code in the ConnectivityManager and in the KeyStoreManagerDelegate that handles
ancient versions of Android.  It would be great to remove it.

If you use Android Studio, you will get warnings about replacing existing code with more modern
idioms.  It would be good to comply with them when the min-version permits.

## Anything you would like to do but you didn’t have a chance to do?
Yes.  There are a few things that I really really looked forward to doing, that I just will not
get time to do:
* Replace all of the uses of C4NativePeer with C4Peer.  This will convert everything to using 
the Cleaner and remove and need for any finalizers.  This is normally pretty straightforward: move the
content of the finalizer into a lambda passed as the Cleanable.  BE CAREFUL THAT THE LAMBDA DOES NOT CONTAIN ANY REFERENCE TO THE CLEANABLE OBJECT.  There may be some complications:
  - removing any call to getPeer.  All uses of the peer should be protected using one of the
   `withPeer...` methods.  Sometimes this means that one withPeer... must wrap another.
  - The essential requirement that the lambda passed to the cleaner not refer to the cleanable object
    in any way, (an Java's constraint that the call to the super constructor be the first statement
    in a constructor, and that you cannot refer, in the call, to any object member) often leads to
    the necessity for a "static constructor": a static factory method that creates things necessary
    in the constructor and then passes them into it.  C4Socket is a particularly good example of this.
* CBL-6753: NativeFLSlice.getBuf() should go away.  It copies a FLSlice into a Java byte array.
  The only reason it does that is to hold a copy for a while so that it can be copies back into
  a FLSlice.  Copying the data into Java space is completely unnecessary and, actually, accident prone.
  Instead the data should just be managed as an FLSliceResult.
* CBL-6752: When the above gets fixed, NativeFLValue.fromTrustedData can go away.
* The way that `ResultSet`s and their ilk are managed is absolutely a user trap.  When you get the
  a value from a ResultSet, you are actualy referring to a segment of the Fleece that represents
  the entire result.  If you ever release the ResultSet, the backing Fleece goes away and the attempt
  to reference, e.g. the member of an array that you previously got from the ResultSet, will explode.
  This is made even more confusing because there is a caching mechanism that makes it so that if you
  reference the value *before* you release the ResultSet, and then release it, referencing the value
  again will succeed.  FLValues, MValues and the wrappers around them are a confusing mess.  As I say
  in the comments, though, with the exception of the above "gottcha" I have not seen bugs there, so
  cleaning it up has been a low priority.

## Anything else?
* Do remember that even `final` members of a class with a `finalizer` may be null, when the
  `finalizer` runs, on Android: You may not even be able to use them for synchronization.
  That will never happen on the JVM.
* There are three markers in code comments that are worth knowing about:
	- !!!: This is broken.  It needs to be fixed.
	- ???: I'm not sure what to do here.  Warning.
	-  "used by reflection": this class/method/field is named, explicitly, in the JNI.
	   Don't change it without making related changes there.
* There are separate release branches for Java and Android.  This was necessary, in the past,
  because we released the two platforms separately.  Our branch protection rules would require
  separate PRs for each branch.  My routine is to submit a PR forthe Android release branch and,
  after it is approved and committed, to relax to commit constraint and to push exactly the same
  commit to the Java branch.
* Godspeed, you guys.