import argparse
import os
import time
from subprocess import run, Popen, PIPE

# known devices: serial, name, relative speed
KNOWN_DEVICES = {
    "29131FDH3008JD":   ["Pixel_7_Pro", 1000],
    "712KPKN1048861":   ["Pixel_2XL",    900],
    "41020DLJH003HH":   ["Pixel_8",      850],
    "14151JEC204776":   ["Pixel_4a",     830],
    "2B141JEGR04407":   ["Pixel_6a",     800],
    "RFCTA0AGBNF":      ["Galaxy_S20",   750],
    "445356394a353498": ["Galaxy_S9",    700],
    "R58T215CEEP":      ["Galaxy_A12",   500],
    "LFKVVG6XW8XSKZRC": ["Redmi_9A",     650],
    "ZX1G324JBJ":       ["Nexus_6",      600],
    "0344242513ad68ab": ["Nexus_5",      500],
    "004c03eb5615429f": ["Nexus_4",      500],
}


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


def get_android_devices():
    adb = run(['adb', 'devices'], stdout=PIPE)
    devs = list(filter(None, adb.stdout.decode("utf-8").split("\n")[1:]))
    devs = list(map(lambda x: x.split("\t")[0].strip(), devs))
    devs = list(map(lambda x: [x] + KNOWN_DEVICES[x], devs))
    devs.sort(reverse=True, key=lambda x: x[2])
    return devs


def test_android(project_root, out_dir, devices):
    os.chdir(project_root)

    for dev in devices:
        dev_serial = dev[0]
        dev_name = dev[1]
        print(f"====== R U N N I N G   A N D R O I D   T E S T S:   {dev_name}({dev_serial})")

        # get rid of old test apps
        run(f"""
            adb -s {dev_serial} shell pm uninstall -k --user 0 com.couchbase.lite.kotlin.test.test
            adb -s {dev_serial} shell pm uninstall -k --user 0 com.couchbase.lite.kotlin.test
            adb -s {dev_serial} shell pm uninstall -k --user 0 com.couchbase.lite.test
            adb -s {dev_serial} logcat -G 1024K
        """, shell=True, check=False)

        # start logcat
        logger = Popen(f"adb -s {dev_serial} logcat > test.log", shell=True)

        # run the tests
        run(f"""
            export ANDROID_SERIAL={dev_serial}
            ./gradlew :ee:android-ktx:ee_android-ktx:devTest
        """, shell=True, check=False)

        time.sleep(10)

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
    parser.add_argument('-v', '--version', choices=['3_1', '3_2', '3_3', '4_0'], help='Version code name')
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
        devs = get_android_devices()
        test_android(
            f"/Users/blakemeike/Working/jak/{args.version}",
            args.output,
            devs)
