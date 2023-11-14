import os
import time
import argparse
from subprocess import run, Popen


def test_java(project_root, out_dir):
    os.chdir(project_root)

    # run the tests
    print(f"====== R U N N I N G   J A V A   T E S T S")
    run(f"./gradlew ee:java:ee_java:devTest -Pverbose=true > test.log 2>&1", shell=True, check=False)

    # copy everything to a safe place
    dest_path = f"{out_dir}/java"
    run(f"""
        cp -a ee/java/lib/build/reports/tests/test {dest_path}
        mv test.log {dest_path}
    """, shell=True, check=False)


def test_android(project_root, out_dir, devices):
    os.chdir(project_root)

    for dev, dev_name in devices.items():
        # get rid of old processes
        run(f"""
            adb -s {dev} shell pm uninstall -k --user 0 com.couchbase.lite.kotlin.test.test
            adb -s {dev} shell pm uninstall -k --user 0 com.couchbase.lite.kotlin.test
            adb -s {dev} shell pm uninstall -k --user 0 com.couchbase.lite.test
        """, shell=True, check=False)

        # start logcat
        logger = Popen(f"adb -s {dev} logcat > {dev}.log", shell=True)

        # run the tests
        print(f"====== R U N N I N G   A N D R O I D   T E S T S:   {dev_name}")
        run(f"""
            export ANDROID_SERIAL={dev}
            ./gradlew :ee:android-ktx:ee_android-ktx:devTest
        """, shell=True, check=False)

        time.sleep(30)

        # kill the logger
        logger.terminate()
        logger.wait()

        # copy everything to a safe place
        dest_path = f"{out_dir}/{dev_name}"
        run(f"""
            cp -a ee/android-ktx/lib/build/reports/androidTests/connected {dest_path}
            mv {dev}.log {dest_path}/test.log
        """, shell=True, check=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Analyze')
    parser.add_argument('-a', '--android', action='store_true', help='Run android tests')
    parser.add_argument('-j', '--java', action='store_true', help='Run java tests')

    args = parser.parse_args()

    if args.java:
        test_java(
            "/Users/blakemeike/Working/jak/beryllium",
            "/Users/blakemeike/Desktop"
        )

    if args.android:
        test_android(
            "/Users/blakemeike/Working/jak/beryllium",
            "/Users/blakemeike/Desktop",
            {
                "14151JEC204776": "Pixel-4a",
                "712KPKN1048861": "Pixel-2XL",
                "ZX1G324JBJ": "Nexus-6",
                "0344242513ad68ab": "Nexus-5",
                "004c03eb5615429f": "Nexus-4"
            })
