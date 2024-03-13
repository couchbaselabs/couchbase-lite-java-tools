"""
Paw through test results to see what happened
"""
import argparse
import os
import shutil
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

WORKING_DIR = ".build_analysis"
LATESTBUILDS = "https://latestbuilds.service.couchbase.com/builds/latestbuilds/"
JAVA_VARIANTS = ["linux", "macos", "windows"]


def delete_quietly(path):
    try:
        shutil.rmtree(path)
    except OSError:
        pass

def check_tests(root, file):
    for failure in ET.parse(os.path.join(root, file)).getroot().findall("./testcase[failure]"):
        failed_test = failure.attrib
        print("  FAILED TEST: {klass}.{test}".format(klass=failed_test["classname"], test=failed_test["name"]))

def fetch_reports(path):
    url = LATESTBUILDS + path
    try:
        urllib.request.urlretrieve(url, "report.zip")
        if not os.path.isfile("report.zip"):
            raise Exception("No reports at URL: " + url)
        with zipfile.ZipFile("report.zip", 'r') as zip:
            zip.extractall(".")
    finally:
        delete_quietly("report.zip")


def analyze_java_tests(platform, root):
    print("Checking platform: {platform}".format(platform=platform))
    reports = [name for name in os.listdir(root) if
               name.endswith(".xml") and os.path.isfile(os.path.join(root, name))]
    if len(reports) < 1:
        raise Exception("No test reports for " + platform)
    for file in reports:
        check_tests(root, file)
    print()


def analyze_android_tests(root):
    reports = [name for name in os.listdir(root) if
               name.endswith("test-.xml") and os.path.isfile(os.path.join(root, name))]
    if len(reports) < 1:
        raise Exception("No test reports for android")
    for file in reports:
        print("Checking android device: {device}".format(device=file[5:-10].replace(" ", "")))
        check_tests(root, file)
        print()


def check_java(version, build_num):
    for platform in JAVA_VARIANTS:
        try:
            fetch_reports(
                "couchbase-lite-java/{v}/{n}/test-reports-{platform}-ee.zip".format(v=version, n=build_num, platform=platform))
            analyze_java_tests(platform, "test/raw")
        except Exception as e:
            print(str(e))
        finally:
            delete_quietly("test")


# should print the device name, somehow?
def check_android(version, build_num):
    try:
        fetch_reports("couchbase-lite-android/{v}/{n}/test-reports-android-ee.zip".format(v=version, n=build_num))
        analyze_android_tests("connected/raw")
    except Exception as e:
        print(str(e))
    finally:
        delete_quietly("connected")


def main():
    parser = argparse.ArgumentParser(description='Analyze')
    parser.add_argument('-v', '--version', help='CBL version to be analyzed')
    parser.add_argument('-a', '--android', type=int, help='Android build # to be analyzed')
    parser.add_argument('-j', '--java', type=int, help='Java build # to be analyzed')

    args = parser.parse_args()

    if not args.version or not (args.android or args.java):
        print("Must supply a version and at least one of android or java build number")
        parser.print_usage()
        exit(-1)

    delete_quietly(WORKING_DIR)
    try:
        os.mkdir(WORKING_DIR)

        try:
            os.chdir(WORKING_DIR)

            if args.android:
                check_android(args.version, args.android)

            if args.java:
                check_java(args.version, args.java)
        finally:
            os.chdir("../")

    finally:
        delete_quietly(WORKING_DIR)


if __name__ == "__main__":
    main()
