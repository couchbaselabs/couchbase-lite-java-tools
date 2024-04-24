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
    dest_path = f"{out_dir}/test-report-Java"
    run(f"""
        cp -a ee/java/lib/build/reports/tests/test {dest_path}
        mv test.log {dest_path}/log.txt
    """, shell=True, check=False)


def test_android(project_root, out_dir, devices):
    os.chdir(project_root)

    for dev, dev_name in devices.items():
        print(f"====== R U N N I N G   A N D R O I D   T E S T S:   {dev_name}")

        # get rid of old processes
        run(f"""
            adb -s {dev} shell pm uninstall -k --user 0 com.couchbase.lite.kotlin.test.test
            adb -s {dev} shell pm uninstall -k --user 0 com.couchbase.lite.kotlin.test
            adb -s {dev} shell pm uninstall -k --user 0 com.couchbase.lite.test
            adb -s {dev} logcat -G 512K
        """, shell=True, check=False)

        # start logcat
        logger = Popen(f"adb -s {dev} logcat > test.log", shell=True)

        # run the tests
        run(f"""
            export ANDROID_SERIAL={dev}
            ./gradlew :ee:android-ktx:ee_android-ktx:devTest
        """, shell=True, check=False)

        time.sleep(30)

        # kill the logger
        logger.terminate()
        logger.wait()

        # copy everything to a safe place
        dest_path = f"{out_dir}/test-report-{dev_name}"
        run(f"""
            cp -a ee/android-ktx/lib/build/reports/androidTests/connected {dest_path}
            mv test.log {dest_path}/log.txt
        """, shell=True, check=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Analyze')
    parser.add_argument('-v', '--version', choices=['helium', 'beryllium', 'boron'], help='Version code name')
    parser.add_argument('-o', '--output', default="/Users/blakemeike/Desktop")
    parser.add_argument('-a', '--android', action='store_true', help='Run android tests')
    parser.add_argument('-j', '--java', action='store_true', help='Run java tests')

    args = parser.parse_args()

    if not args.version or not (args.android or args.java):
        print("Must supply a version and at least one of android or java")
        exit(-1)

    print(f"====== T E S T I N G: {args.version} to {args.output}")

    if args.java:
        test_java(
            f"/Users/blakemeike/Working/jak/{args.version}",
            args.output
        )

    if args.android:
        test_android(
            f"/Users/blakemeike/Working/jak/{args.version}",
            args.output,
            {
                "14151JEC204776": "Pixel-4a",
                "712KPKN1048861": "Pixel-2XL",
                "ZX1G324JBJ": "Nexus-6",
                # "0344242513ad68ab": "Nexus-5",
                "004c03eb5615429f": "Nexus-4"
            })
